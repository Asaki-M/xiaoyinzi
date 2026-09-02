package com.xiaoyinzi.player.casting

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Inet4Address
import java.util.ArrayDeque

class MacServiceDiscovery(context: Context) : AutoCloseable {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val multicastLock =
        (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
            .createMulticastLock("xiaoyinzi-lyrics-discovery")
            .apply { setReferenceCounted(false) }
    private val _devices = MutableStateFlow<List<CastDevice>>(emptyList())
    val devices: StateFlow<List<CastDevice>> = _devices.asStateFlow()
    private val _discovering = MutableStateFlow(false)
    val discovering: StateFlow<Boolean> = _discovering.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val resolvedByService = linkedMapOf<String, CastDevice>()
    private val pendingResolution = ArrayDeque<NsdServiceInfo>()
    private var resolving = false
    private val discoveryListeners = linkedMapOf<String, NsdManager.DiscoveryListener>()

    val isDiscovering: Boolean
        get() = _discovering.value

    @Synchronized
    fun start() {
        if (discoveryListeners.isNotEmpty()) return
        resolvedByService.clear()
        publishDevices()
        _error.value = null
        runCatching {
            if (!multicastLock.isHeld) multicastLock.acquire()
        }.onFailure { error ->
            _error.value = "无法接收局域网设备广播：${error.localizedMessage.orEmpty()}"
        }
        CAST_SERVICE_TYPES.forEach(::startDiscoveryForType)
        _discovering.value = discoveryListeners.isNotEmpty()
        if (discoveryListeners.isEmpty() && _error.value == null) {
            _error.value = "无法启动局域网设备搜索"
            releaseMulticastLock()
        }
    }

    private fun startDiscoveryForType(requestedType: String) {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                _discovering.value = true
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                synchronized(this@MacServiceDiscovery) {
                    if (pendingResolution.none { serviceKey(it) == serviceKey(serviceInfo) }) {
                        pendingResolution.addLast(serviceInfo)
                    }
                    resolveNext()
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                synchronized(this@MacServiceDiscovery) {
                    resolvedByService.remove(serviceKey(serviceInfo))
                    publishDevices()
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                synchronized(this@MacServiceDiscovery) {
                    discoveryListeners.remove(requestedType)
                    updateDiscoveryState()
                }
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                synchronized(this@MacServiceDiscovery) {
                    discoveryListeners.remove(requestedType)
                    if (discoveryListeners.isEmpty()) {
                        _error.value = "局域网设备搜索启动失败（错误码 $errorCode）"
                    }
                    updateDiscoveryState()
                }
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                synchronized(this@MacServiceDiscovery) {
                    discoveryListeners.remove(requestedType)
                    updateDiscoveryState()
                }
            }
        }
        discoveryListeners[requestedType] = listener
        runCatching {
            nsdManager.discoverServices(requestedType, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure { error ->
            discoveryListeners.remove(requestedType)
            if (discoveryListeners.isEmpty()) {
                _error.value = "局域网设备搜索启动失败：${error.localizedMessage.orEmpty()}"
            }
        }
    }

    @Synchronized
    fun stop() {
        val listeners = discoveryListeners.values.toList()
        discoveryListeners.clear()
        _discovering.value = false
        listeners.forEach { listener ->
            runCatching { nsdManager.stopServiceDiscovery(listener) }
        }
        pendingResolution.clear()
        resolving = false
        resolvedByService.clear()
        publishDevices()
        releaseMulticastLock()
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
                    if (resolvedByService.isEmpty()) {
                        _error.value = "已发现 ${serviceInfo.serviceName}，但无法获取地址（错误码 $errorCode）"
                    }
                    resolving = false
                    resolveNext()
                }
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    serviceInfo.hostAddresses
                } else {
                    listOfNotNull(serviceInfo.host)
                }
                val address = addresses.firstOrNull { it is Inet4Address }
                    ?: addresses.firstOrNull { !it.isLinkLocalAddress }
                    ?: addresses.firstOrNull()
                synchronized(this@MacServiceDiscovery) {
                    if (discoveryListeners.isNotEmpty()) address?.hostAddress?.let { host ->
                        resolvedByService[serviceKey(serviceInfo)] = CastDevice(
                            id = "${serviceInfo.serviceName}@$host:${serviceInfo.port}",
                            name = serviceInfo.serviceName,
                            host = host,
                            port = serviceInfo.port,
                        )
                        _error.value = null
                        publishDevices()
                    }
                    resolving = false
                    resolveNext()
                }
            }
        })
    }

    private fun publishDevices() {
        _devices.value = resolvedByService.values.distinctBy(CastDevice::id).sortedBy(CastDevice::name)
    }

    private fun serviceKey(service: NsdServiceInfo): String =
        "${service.serviceType.trimEnd('.')}|${service.serviceName}"

    private fun updateDiscoveryState() {
        _discovering.value = discoveryListeners.isNotEmpty()
        if (discoveryListeners.isEmpty()) releaseMulticastLock()
    }

    private fun releaseMulticastLock() {
        runCatching {
            if (multicastLock.isHeld) multicastLock.release()
        }
    }

    override fun close() = stop()
}
