# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Android application. Production Kotlin lives under `app/src/main/java/com/xiaoyinzi/player/`, organized by responsibility: `casting/` handles Mac discovery and the lyrics protocol, `data/` owns Room entities and DAOs, `library/` scans and manages local media, `lyrics/` parses `.lrcx`, `playback/` wraps Media3, and `ui/` contains Compose screens and theming. Android resources are in `app/src/main/res/`; keep raster artwork in `drawable-nodpi/`. Local JVM tests mirror production packages in `app/src/test/`. Protocol details belong in `mac-lyrics-protocol.md`; general setup belongs in `README.md`.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper with JDK 17 and Android SDK 35:

```bash
./gradlew assembleDebug       # Build the debug APK
./gradlew testDebugUnitTest   # Run local JVM unit tests
./gradlew lintDebug           # Run Android lint on the debug variant
./gradlew installDebug        # Install on a connected device/emulator
```

Android Studio can run the `app` configuration for interactive development. Before submitting changes, run at least `./gradlew testDebugUnitTest assembleDebug`.

## Coding Style & Naming Conventions

Follow the official Kotlin style configured in `gradle.properties`: four-space indentation, trailing commas in multiline declarations, and concise expression bodies where readable. Use `PascalCase` for classes, composables, and test classes; `camelCase` for functions and properties; and `UPPER_SNAKE_CASE` for constants. Keep packages lowercase and place feature-specific code beside its owning module. Compose functions should describe UI nouns (for example, `PlayerScreen`), while state and service types should state their role (`LibraryUiState`, `PlaybackService`).

## Testing Guidelines

Tests use JUnit 4 (`org.junit.Test`). Name files `<Subject>Test.kt` and test methods after observable behavior; backtick names are acceptable for protocol-style specifications. Add focused tests for parser edge cases, serialization compatibility, and business logic. Instrumented or Compose UI tests, when needed, belong in `app/src/androidTest/`. No coverage threshold is currently enforced; prioritize regression tests for changed behavior.

## Commit & Pull Request Guidelines

Recent history uses Conventional Commit prefixes such as `feat:` followed by a short, imperative summary. Continue with scoped prefixes like `fix:`, `test:`, `docs:`, or `refactor:` and keep each commit focused. Pull requests should explain user-visible behavior, list verification commands, link relevant issues, and include screenshots or recordings for UI changes. Call out protocol or Room schema changes explicitly, including compatibility or migration considerations.

## Security & Assets

Do not commit signing keys, secrets, local SDK paths, or user media. The bundled artist images are for personal fan use; replace them with properly licensed assets before public or commercial distribution.
