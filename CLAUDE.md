# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

GuardianApp is being built in phases (see the user's phase instructions in chat/PR
history — there is no in-repo phase tracker).

- **Phase 1** (done): single-module Android project using Kotlin + Jetpack Compose,
  with a static "app is working" screen.
- **Phase 2** (done): local role selection — `RoleSelectionScreen` lets the user pick
  `Role.USUARIO` or `Role.APODERADO` (`Role.kt`); `GuardianAppRoot` in
  `MainActivity.kt` holds the selected role as plain Compose state
  (`remember { mutableStateOf<Role?>(null) }`) and switches between
  `RoleSelectionScreen` and `RoleHomeScreen` — no navigation library was added, since
  it's a single back-and-forth toggle with no back-stack/args/deep-link needs.
  "Cerrar sesión" just resets the state to `null`. The role is **not** persisted
  (by design for this phase) and there is no real auth/accounts yet.

- **Phase 3** (done): BLE connection from the Usuario role to an ESP32 test
  peripheral. `UsuarioBleScreen.kt` replaces `RoleHomeScreen` for
  `Role.USUARIO` only (Apoderado still uses the generic `RoleHomeScreen`
  unchanged). All BLE logic lives in `app/.../ble/` (`BleManager`,
  `BlePermissions`, `BleConstants`, `BleModels`) — the UI only reads
  `BleManager`'s `mutableStateOf` properties and calls `startScan()` /
  `connect()` / `disconnect()`. No coroutines/Flow: BLE callbacks (which run
  on Binder threads) write directly to Compose state, which is safe from any
  thread. See `app/.../ble/BleConstants.kt` for the Service/Characteristic
  UUIDs (must match `esp32/GuardianAppBleTest/GuardianAppBleTest.ino`) and
  `esp32/README.md` for the ESP32 test firmware (Arduino IDE, board "ESP32
  Dev Module", no external libraries needed).
  - Permission model (dual, because `minSdk 26` spans the Android 12/API 31
    permission split): `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT` on API 31+,
    `ACCESS_FINE_LOCATION` (runtime) + `BLUETOOTH`/`BLUETOOTH_ADMIN`
    (install-time) below that. See `BlePermissions.kt` / `AndroidManifest.xml`.
  - Deliberately uses the pre-API-33 GATT surface (`characteristic.value`,
    `descriptor.value`, single-arg `onCharacteristicChanged`) with
    `@Suppress("DEPRECATION")`/`@SuppressLint("MissingPermission")` rather
    than branching on `Build.VERSION.SDK_INT >= 33` for the newer
    `ByteArray`-based overloads — one code path across `minSdk 26..targetSdk
    37`, at the cost of deprecation warnings (suppressed, not errors).
    `./gradlew lintDebug` should stay clean of new errors when touching this
    file.

- **Phase 4** (done): the Usuario role forwards each BLE event to Cloud
  Firestore. All Firebase code lives in `app/.../firebase/`
  (`FirebaseRepository`, `EventUploadState`) — `BleManager`/`ble/` were
  **not** modified. `UsuarioBleScreen.kt` reacts to `bleManager.lastEvent`
  changing (`LaunchedEffect`) by calling
  `firebaseRepository.logEvent(event.message)`, and reads
  `firebaseRepository.uploadState` (same "class exposes `mutableStateOf`,
  UI just reads it" pattern as `BleManager`) to render
  Idle/Sending/Success/Error under the received-event text.
  - Firestore collection `events`, one document per event: `type`
    (`"ESP32_EVENT"`), `message`, `deviceId` (`"ESP32_GUARDIAN"`, a fixed
    placeholder — see `FirebaseRepository.DEFAULT_DEVICE_ID`), `source`
    (`"esp32"`), `timestamp` (`FieldValue.serverTimestamp()`, not the phone's
    local clock).
  - `FirebaseRepository.logEvent` never throws outward: it handles Firebase
    not configured (`IllegalStateException` from `FirebaseFirestore.getInstance()`),
    no internet (pre-checked via `ConnectivityManager`), empty message,
    Firestore errors (`addOnFailureListener`, mapped to friendlier text for
    `UNAVAILABLE`/`PERMISSION_DENIED`/`DEADLINE_EXCEEDED`), and a manual
    10s timeout (`Handler.postDelayed`, guarded by a request-sequence number
    so a stale timeout can't clobber a later request's real result) — all
    surfaced as `EventUploadState.Error(message)`, never a crash.
  - Requires `google-services.json` in `app/` (gitignored — see
    `firebase/README.md` for how to generate one; not needed to read/edit
    the Kotlin code, only to actually build/run) plus the `google-services`
    Gradle plugin (`gradle/libs.versions.toml`) and `firebase-bom` +
    `firebase-firestore` deps in `app/build.gradle.kts`.
  - Firestore security rules (temporary, no Auth yet — see
    `firebase/firestore.rules` and the "must revisit once Auth exists" note
    at its top) live in `firebase/firestore.rules`, published by hand in
    Firebase Console (no Firebase CLI/emulator set up in this repo).
  - Added `INTERNET` / `ACCESS_NETWORK_STATE` permissions to
    `AndroidManifest.xml`.

Still not implemented (explicitly deferred to later phases — don't add unless
asked): Firebase Cloud Messaging / push notifications, real login/accounts
(Firebase Authentication), the Apoderado phone, phone-to-phone communication,
a general-purpose backend/API. Phase 5 is "Firebase Cloud Messaging so the
Apoderado phone gets notified when an event happens."

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

This is a git repository (`git init` was run during Phase 2); the first commits are
tagged `Fase 1: ...` / `Fase 2: ...`.

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
- Application code: `app/src/main/java/com/example/guardianapp` — flat package
  for screens/composables (`MainActivity.kt`, `Role.kt`,
  `RoleSelectionScreen.kt`, `RoleHomeScreen.kt`, `UsuarioBleScreen.kt`); BLE
  logic separated into the `ble/` subpackage (`BleManager.kt`,
  `BlePermissions.kt`, `BleConstants.kt`, `BleModels.kt`); Firebase logic
  separated into the `firebase/` subpackage (`FirebaseRepository.kt`,
  `EventUploadState.kt`).
- JVM unit tests: `app/src/test/java/com/example/guardianapp`
- Instrumented (on-device) tests: `app/src/androidTest/java/com/example/guardianapp`
- `esp32/` (repo root, outside `app/`, not part of the Gradle build): Arduino
  sketch + README for the ESP32 BLE test peripheral used in Phase 3.
- `firebase/` (repo root, outside `app/`, not part of the Gradle build):
  `firestore.rules` (source of truth, published by hand in Firebase Console)
  + README with the manual Firebase Console setup steps for Phase 4.

### Running `./gradlew` from a plain terminal

Android Studio bundles its own JDK and uses it automatically; a bare terminal on
this machine has no `java` on `PATH`. Set `JAVA_HOME` to Android Studio's bundled
JBR before invoking the wrapper directly:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug
```
