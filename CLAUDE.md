# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

GuardianApp is being built in phases (see the user's phase instructions in chat/PR
history — there is no in-repo phase tracker). **Phase 1** (done): a single-module
Android project using Kotlin + Jetpack Compose, with one screen (`MainActivity` /
`GuardianAppScreen` in `app/src/main/java/com/example/guardianapp/MainActivity.kt`)
that just shows "GuardianApp" / "Aplicación funcionando correctamente" / "FASE 1".
No BLE, Firebase, roles (Usuario/Apoderado), auth, database, notifications, or
ESP32 integration yet — those are explicitly deferred to later phases. Don't add
them unless asked.

`androidx.appcompat` and `com.google.android.material` (the old View-system Material
Components library) are still declared as dependencies and are what the manifest
theme (`Theme.GuardianApp`, extending `Theme.MaterialComponents.DayNight.DarkActionBar`)
resolves against, but no code uses them — all UI is Compose (`material3`). They were
kept to avoid touching `themes.xml`/`colors.xml` unnecessarily; feel free to remove
them once/if the app fully moves to a Compose-only theme.

This project uses **AGP 9's built-in Kotlin support**: do **not** apply
`org.jetbrains.kotlin.android` in `build.gradle.kts` — AGP compiles Kotlin sources
itself (KGP is a runtime dependency of AGP). Only the Compose compiler plugin
(`org.jetbrains.kotlin.plugin.compose`, aliased as `libs.plugins.kotlin.compose`) is
applied explicitly, in `app/build.gradle.kts`. Applying `kotlin.android` manually
fails the build with an explicit error telling you to remove it.

This is not a git repository yet (no `.git`). If the user asks to commit, `git init` first.

## Build system notes

- Gradle wrapper: Gradle 9.5.0, Java toolchain 25 (see `gradle/gradle-daemon-jvm.properties`).
- AGP version 9.3.2, declared via the version catalog (`gradle/libs.versions.toml`) and
  used with its newer declarative DSL — notably:
  - `compileSdk { version = release(37) }` instead of the older `compileSdk = 37`.
  - `buildTypes { release { optimization { enable = false } } }` instead of the older
    `isMinifyEnabled`.
  - R8/ProGuard keep rules go in `app/src/main/keepRules/*.keep` (AGP combines all files
    in that directory), not a `proguard-rules.pro` referenced from `build.gradle.kts`.
  When adding build config, follow this newer declarative style rather than
  older AGP examples/tutorials, which use the legacy syntax.
- All dependency versions are centralized in `gradle/libs.versions.toml` (version
  catalog) and referenced via `libs.*` in `app/build.gradle.kts` — add new
  dependencies there rather than hardcoding coordinates in the module build file.
- `minSdk = 26`, `targetSdk = compileSdk = 37`, Java 11 source/target compatibility.

## Common commands

Run all commands from the repo root using the Gradle wrapper.

```bash
# Build
./gradlew assembleDebug          # build debug APK
./gradlew build                  # full build (compiles, tests, lints)

# Unit tests (JVM, app/src/test)
./gradlew test
./gradlew testDebugUnitTest --tests "com.example.guardianapp.ExampleUnitTest"
./gradlew testDebugUnitTest --tests "*.ExampleUnitTest.addition_isCorrect"

# Instrumented tests (require a connected device/emulator, app/src/androidTest)
./gradlew connectedAndroidTest
./gradlew connectedDebugAndroidTest --tests "com.example.guardianapp.ExampleInstrumentedTest"

# Lint
./gradlew lint
./gradlew lintDebug

# Clean
./gradlew clean
```

Source sets:
- Application code: `app/src/main/java/com/example/guardianapp`
- JVM unit tests: `app/src/test/java/com/example/guardianapp`
- Instrumented (on-device) tests: `app/src/androidTest/java/com/example/guardianapp`

### Running `./gradlew` from a plain terminal

Android Studio bundles its own JDK and uses it automatically; a bare terminal on
this machine has no `java` on `PATH`. Set `JAVA_HOME` to Android Studio's bundled
JBR before invoking the wrapper directly:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug
```
