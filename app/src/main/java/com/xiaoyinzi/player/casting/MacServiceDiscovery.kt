package com.xiaoyinzi.player.casting

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque

class MacServiceDiscovery(context: Context) : AutoCloseable {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val _devices = MutableStateFlow<List<CastDevice>>(emptyList())
    val devices: StateFlow<List<CastDevice>> = _devices.asStateFlow()
    private val _discovering = MutableStateFlow(false)
    val discovering: StateFlow<Boolean> = _discovering.asStateFlow()
    private val resolvedByName = linkedMapOf<String, CastDevice>()
    private val pendingResolution = ArrayDeque<NsdServiceInfo>()
    private var resolving = false
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    val isDiscovering: Boolean
        get() = _discovering.value

    @Synchronized
    fun start() {
        if (discoveryListener != null) return
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                _discovering.value = true
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                synchronized(this@MacServiceDiscovery) {
                    if (pendingResolution.none { it.serviceName == serviceInfo.serviceName }) {
                        pendingResolution.addLast(serviceInfo)
                    }
                    resolveNext()
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                synchronized(this@MacServiceDiscovery) {
                    resolvedByName.remove(serviceInfo.serviceName)
                    publishDevices()
                }
            }

            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                synchronized(this@MacServiceDiscovery) {
                    discoveryListener = null
                    _discovering.value = false
                }
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                synchronized(this@MacServiceDiscovery) {
                    discoveryListener = null
                    _discovering.value = false
                }
            }
        }
        discoveryListener = listener
        _discovering.value = true
        runCatching {
            nsdManager.discoverServices(CAST_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure {
            discoveryListener = null
            _discovering.value = false
        }
    }

    @Synchronized
    fun stop() {
        val listener = discoveryListener ?: return
        discoveryListener = null
        _discovering.value = false
        runCatching { nsdManager.stopServiceDiscovery(listener) }
        pendingResolution.clear()
        resolving = false
    }

    @Suppress("DEPRECATION")
    @Synchronized
    private fun resolveNext() {
        if (resolving || pendingResolution.isEmpty()) return
        resolving = true
        val service = pendingResolution.removeFirst()
        nsdManager.resolveService(service, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                synchronized(this@MacServiceDiscovery) {
                    resolving = false
                    resolveNext()
                }
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    serviceInfo.hostAddresses.firstOrNull()
                } else {
                    serviceInfo.host
                }
                synchronized(this@MacServiceDiscovery) {
                    address?.hostAddress?.let { host ->
                        resolvedByName[serviceInfo.serviceName] = CastDevice(
                            id = "${serviceInfo.serviceName}@$host:${serviceInfo.port}",
                            name = serviceInfo.serviceName,
                            host = host,
                            port = serviceInfo.port,
                        )
                        publishDevices()
                    }
                    resolving = false
                    resolveNext()
                }
            }
        })
    }

    private fun publishDevices() {
        _devices.value = resolvedByName.values.sortedBy(CastDevice::name)
    }

    override fun close() = stop()
}
