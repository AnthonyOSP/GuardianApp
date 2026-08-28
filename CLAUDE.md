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
  `Role.USUARIO` only (Apoderado still used the generic `RoleHomeScreen`
  unchanged at this point — Phase 5 later replaced it too and deleted
  `RoleHomeScreen.kt`, see below). All BLE logic lives in `app/.../ble/` (`BleManager`,
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
    placeholder — see `EventConstants.DEFAULT_DEVICE_ID`), `source`
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

- **Phase 5** (done): Apoderado gets a notification when the Usuario's BLE
  event fires. `ApoderadoScreen.kt` replaces `RoleHomeScreen` for
  `Role.APODERADO` only (`RoleHomeScreen.kt` was deleted — it had no
  remaining callers once both roles had a dedicated screen). All FCM
  *receiving* code lives in `app/.../fcm/` (`FcmTokenRepository`,
  `GuardianFirebaseMessagingService`, `GuardianNotifications`,
  `GuardianNotificationCenter`, `DeviceRegistrationState`, `NotifiedEvent`).
  - **Architecture**: Android does **not** trigger FCM by watching Firestore
    (that was the original plan and was rejected — see chat history — because
    it would've meant either Firebase Cloud Functions, which requires
    upgrading to the Blaze billing plan just to *deploy*, or a script that
    has to keep running somewhere). Instead: `Usuario` → `POST /api/events`
    (HTTPS, own backend) → backend reads `apoderadoTokens` from Firestore
    with Admin SDK → FCM. The backend lives in `backend/` (repo root,
    **separate Node.js/Express project, not part of the Gradle build**,
    deployed to Render — see `backend/README.md`) and holds the Admin SDK
    credentials that must never reach Android. Firestore's `events`
    collection (Phase 4) is unchanged and still written directly from
    Android — the HTTP call to the backend is a second, independent side
    effect of the same BLE event, not a replacement for it.
    ```
    ESP32 --BLE--> Usuario --+--> Firestore (events, unchanged from Phase 4)
                              +--> HTTPS POST /api/events --> backend (Render)
                                     --Admin SDK--> reads apoderadoTokens, sends FCM --> Apoderado
    ```
  - `EventConstants.kt` (top-level, not under `firebase/` or `backend/`)
    holds `EVENT_TYPE`/`DEFAULT_DEVICE_ID` — extracted out of
    `FirebaseRepository` because `BackendEventRepository` needed the same
    constants and they're conceptually about the test event, not about
    either integration.
  - `app/.../backend/` (Android side): `BackendEventRepository` (same
    `mutableStateOf`-exposing pattern as the other repositories),
    `BackendNotifyState`. Deliberately plain `HttpURLConnection` on a
    single-thread `Executor`, not OkHttp/Retrofit — one small JSON POST
    doesn't justify a new dependency in a project that's stayed
    dependency-light throughout. Generous timeouts (connect 20s, read 60s)
    because Render's free tier sleeps after inactivity and can take 30-50s
    to wake on the next request.
  - `BACKEND_BASE_URL` / `BACKEND_API_KEY` are `BuildConfig` fields
    (`buildFeatures.buildConfig = true`), read from `local.properties`
    (gitignored) in `app/build.gradle.kts`, defaulting to `""` so the
    project still compiles before the backend is deployed — see
    `firebase/README.md` § 7e. Never hardcoded in committed `.kt` source.
  - `FcmTokenRepository` mirrors `FirebaseRepository`'s pattern (exposes
    `mutableStateOf`, UI reads it); `isInternetAvailable`/`describeFirebaseError`
    were extracted from `FirebaseRepository` into `firebase/FirebaseUtils.kt`
    (both `internal`, module-wide visible) so the two repositories share them.
  - Temporary Apoderado↔device association (no accounts yet): Firestore
    collection `apoderadoTokens`, doc ID = the FCM token itself (proof the
    writer holds that token, without real auth) — see `FcmTokenRepository`
    doc comment and `firebase/README.md`.
  - `GuardianNotificationCenter` is a plain Kotlin `object` holding
    `mutableStateOf<NotifiedEvent?>` — the **one** deliberate exception to
    "each screen creates its own manager via `remember`", because
    `FirebaseMessagingService` and `MainActivity.onNewIntent` are instantiated
    by the OS, not by the Compose tree, so there's no per-screen instance to
    inject into. Both paths (message arrives in foreground via
    `onMessageReceived`, or the app is (re)launched by tapping a
    system-displayed notification, extras read in `MainActivity.onCreate`/
    `onNewIntent`) funnel into this same singleton so `ApoderadoScreen` only
    has to read one thing.
  - `MainActivity` is `launchMode="singleTop"` specifically so tapping a
    notification while the activity is already alive reuses it via
    `onNewIntent` instead of recreating it — a fresh instance would reset
    `selectedRole` to `null` (role still isn't persisted, by design since
    Phase 2) and bounce the Apoderado back to role selection.
  - FCM foreground/background behavior (why messages carry both
    `notification` and `data` payloads, and why `onMessageReceived` only
    fires in foreground) is documented in
    `GuardianFirebaseMessagingService`'s class doc comment and
    `firebase/README.md` § "contrato del mensaje FCM" — that section is the
    payload contract the backend must match.
  - Compatibility note: the resolved `firebase-messaging` SDK (25.1.2, from
    `firebase-bom` 34.18.0) marks `FirebaseMessaging.token` and
    `FirebaseMessagingService.onNewToken()` `@Deprecated` in favor of a newer
    `register()`/`onRegistered()`/`onUnregistered()` model (installation-ID
    based) that's opt-in and off by default. Deliberately kept the classic
    token API (still fully supported, and what current Firebase docs use)
    with `@Suppress("DEPRECATION")` — same rationale/precedent as the BLE
    pre-API-33 GATT surface above. Revisit if Firebase ever removes the old
    path.
  - `POST_NOTIFICATIONS` runtime permission requested in `ApoderadoScreen`
    only on API 33+ (`Build.VERSION.SDK_INT >= TIRAMISU`); below that it's
    granted at install time. Token registration does not wait on this
    permission (FCM delivery and the permission are independent — the
    permission only gates whether Android will *display* a notification).
  - `firebase/firestore.rules` gained an `apoderadoTokens` match block (same
    "temporary, no Auth yet" caveat as `events`). The Render backend uses
    Admin SDK credentials that bypass these rules entirely — it doesn't need
    an `allow` rule to read tokens or delete stale ones.
  - **Security, all deliberately temporary and documented as such** (same
    spirit as the Firestore rules' "no Auth yet" caveat): `backend/`
    validates requests with a shared-secret header (`X-API-Key` against
    `EVENTS_API_KEY`), not real authentication — anyone with the key
    (extractable from the APK, which isn't obfuscated:
    `optimization.enable = false`) could call the endpoint. Documented in
    `backend/src/middleware/apiKey.js` and `backend/README.md` as something
    to replace with Firebase Authentication (Android sends an ID token,
    backend calls `admin.auth().verifyIdToken(...)`) once accounts exist.
    Admin SDK credentials (the Firebase service account JSON) are **never**
    in Android, **never** committed to git (`backend/.gitignore`), and on
    Render are a **Secret File**, not a plaintext env var (avoids escaping
    the RSA private key's newlines in a single-line value).
  - **Multi-user preparation (not yet implemented)**: today there's one test
    Usuario/ESP32 and the backend broadcasts to *every* doc in
    `apoderadoTokens` — deliberate, documented temporary behavior (see the
    `TODO(Fase futura...)` comment in `backend/src/routes/events.js`, which
    is the exact spot where `deviceId → Usuario → Apoderado → token` must be
    resolved once real accounts/linking exist, instead of the broadcast).
    `deviceId` already travels on every event specifically so that payload
    contract doesn't need to change later.
  - Compatibility note (backend): the resolved `firebase-admin` npm package
    (14.3.0) marks the token-based `sendEachForMulticast(MulticastMessage)`
    overload `@deprecated` in favor of one based on Firebase Installation
    IDs ("FIDs") — the same underlying shift as the Android
    `FirebaseMessaging.token` deprecation noted above. Kept the classic
    token-based overload deliberately, for the same reason.

Still not implemented (explicitly deferred to later phases — don't add unless
asked): Firebase Authentication, the real Usuario↔Apoderado relationship
(replacing the `apoderadoTokens` broadcast — see "Multi-user preparation"
above), a general-purpose backend/API beyond this one small events endpoint,
sensors, geolocation, SMS/WhatsApp/email. Phase 6 is final testing, fixes,
and producing the release APK for the university presentation.

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
- `buildFeatures { buildConfig = true }` (Phase 5) generates `BuildConfig.BACKEND_BASE_URL`
  / `BACKEND_API_KEY` from `local.properties` (read manually in
  `app/build.gradle.kts` via `java.util.Properties`, defaulting to `""` if
  the keys are absent so the project still compiles before the backend is
  deployed) — this used the classic `defaultConfig { buildConfigField(...) }`
  API and worked fine alongside AGP 9's newer declarative blocks above; it
  wasn't necessary to find a declarative equivalent.

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
  `RoleSelectionScreen.kt`, `UsuarioBleScreen.kt`, `ApoderadoScreen.kt`;
  no `RoleHomeScreen.kt` anymore — deleted in Phase 5 once both roles had a
  dedicated screen and nothing referenced it) plus `EventConstants.kt`
  (shared `EVENT_TYPE`/`DEFAULT_DEVICE_ID`, used by both `firebase/` and
  `backend/` below); BLE logic separated into the `ble/` subpackage
  (`BleManager.kt`, `BlePermissions.kt`, `BleConstants.kt`, `BleModels.kt`);
  Firebase/Firestore logic in the `firebase/` subpackage
  (`FirebaseRepository.kt`, `EventUploadState.kt`, `FirebaseUtils.kt`); FCM
  *receiving* logic in the `fcm/` subpackage (`FcmTokenRepository.kt`,
  `GuardianFirebaseMessagingService.kt`, `GuardianNotifications.kt`,
  `GuardianNotificationCenter.kt`, `DeviceRegistrationState.kt`,
  `NotifiedEvent.kt`); the HTTP client that talks to the `backend/` project
  below is in the `backend/` subpackage (`BackendEventRepository.kt`,
  `BackendNotifyState.kt`) — same subpackage name as the top-level
  `backend/` Node project one level up, don't confuse the two.
- JVM unit tests: `app/src/test/java/com/example/guardianapp`
- Instrumented (on-device) tests: `app/src/androidTest/java/com/example/guardianapp`
- `esp32/` (repo root, outside `app/`, not part of the Gradle build): Arduino
  sketch + README for the ESP32 BLE test peripheral used in Phase 3.
- `firebase/` (repo root, outside `app/`, not part of the Gradle build):
  `firestore.rules` (source of truth, published by hand in Firebase Console)
  + README with the manual Firebase Console setup steps (Phases 4-5), the
  FCM message payload contract, and how to point Android at the deployed
  backend (`local.properties`).
- `backend/` (repo root, outside `app/`, **separate Node.js/Express
  project, not part of the Gradle build, not compiled/run by any
  `./gradlew` command**): receives `POST /api/events` from the Usuario,
  reads `apoderadoTokens` from Firestore with Firebase Admin SDK, sends the
  FCM notification, deployed to Render. See `backend/README.md` for local
  run and deployment steps. Holds Admin SDK credentials — must never be
  merged into or imported by the Android app.

### Running `./gradlew` from a plain terminal

Android Studio bundles its own JDK and uses it automatically; a bare terminal on
this machine has no `java` on `PATH`. Set `JAVA_HOME` to Android Studio's bundled
JBR before invoking the wrapper directly:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug
```
