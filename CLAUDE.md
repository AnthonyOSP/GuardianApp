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
  - Compatibility note (backend): the resolved `firebase-admin` npm package
    (14.3.0) marks the token-based `sendEachForMulticast(MulticastMessage)`
    overload `@deprecated` in favor of one based on Firebase Installation
    IDs ("FIDs") — the same underlying shift as the Android
    `FirebaseMessaging.token` deprecation noted above. Kept the classic
    token-based overload deliberately, for the same reason.

- **Phase 6** (done): real Usuario↔Apoderado linking — the backend
  broadcast from Phase 5 is gone. A BLE event now reaches only the
  Apoderado actually linked to the Usuario that generated it.
  - **Identity, still without Firebase Authentication**: `usuarioId`
    (Usuario, a 6-character human-typeable code) and `apoderadoId`
    (Apoderado, a UUID) are anonymous per-install IDs generated once and
    persisted in `SharedPreferences` — new file
    `app/.../identity/LocalIdentity.kt`. This is a deliberate, narrow
    exception to Phase 2's "don't persist anything" rule: the selected
    *role* still isn't persisted (`GuardianAppRoot`'s `selectedRole` state
    is unchanged), only this anonymous pairing identity is. Designed so
    Firebase Authentication (still not implemented) can later replace *only
    the source* of these two values with a real `uid` — the Firestore shape
    (`vinculaciones/{usuarioId}`, `apoderadoTokens.apoderadoId`) doesn't
    change when that happens.
  - **Firestore**: new collection `vinculaciones/{usuarioId}` → `{
    apoderadoId, createdAt }`; `apoderadoTokens/{token}` gained an
    `apoderadoId` field (written by `FcmTokenRepository`, which now takes
    `apoderadoId` as a parameter instead of writing just
    `fcmToken`/`updatedAt`). Old Phase-5 `apoderadoTokens` docs lack
    `apoderadoId` and are simply unreachable by the new filtered query —
    harmless but orphaned; `firebase/README.md` § 7f tells the user to
    delete them before testing this phase.
  - **Pairing UX**: the 6-char code is generated with an alphabet that
    excludes ambiguous characters (`0/O`, `1/I`) since a human types it.
    `UsuarioBleScreen` displays it unconditionally (not gated on BLE
    connection state). `ApoderadoScreen` gained a text field + "Vincular"
    button, backed by new `firebase/VinculacionRepository.kt` +
    `VinculacionState.kt` (same `mutableStateOf`-exposing pattern as every
    other repository in the project) that writes
    `vinculaciones/{code} = { apoderadoId, createdAt }`.
  - **Explicitly documented as NOT a security measure** (per the user's own
    instruction): the pairing code is a convenience mechanism only —
    anyone who learns a Usuario's code can link an Apoderado to it. Same
    caveat as `EVENTS_API_KEY`; both get replaced when Firebase
    Authentication arrives. See the comment on `vinculaciones` in
    `firebase/firestore.rules` and `backend/README.md` § "Multi-usuario".
  - **`backend/src/routes/events.js`**: the old
    `apoderadoTokens.get()` (broadcast) became `usuarioId` (now a required
    body field, validated like the others) → `vinculaciones/{usuarioId}.get()`
    → `apoderadoTokens.where('apoderadoId', '==', apoderadoId).get()`. No
    vinculación, or a vinculación with no tokens, both return `200 {
    notified: 0, warning: "..." }` (not an error — same style as Phase 5's
    "0 tokens" case). No new dependency (`.where()` was already part of the
    Firestore SDK in use). `firebaseAdmin.js`, `middleware/apiKey.js`,
    `index.js` untouched.
  - **`firebase/firestore.rules`**: this phase also fixed a pre-existing
    file corruption (two stray backtick characters after the closing brace,
    `  }`` ``, from an earlier bad edit) that would have made the file fail
    to parse in Firebase Console — unrelated to Phase 6 but discovered
    while touching this file and fixed as part of it. Rules for
    `apoderadoTokens` now require `apoderadoId`; new `vinculaciones/{usuarioId}`
    block blocks all client reads/deletes and validates create/update shape
    (`apoderadoId is string`, `createdAt == request.time`) — same "no real
    auth yet" caveat as every other rule in this file.
  - Verified with an in-memory Firestore/FCM stub injected via
    `require.cache` (temporary test script, not committed) exercising the
    real `routes/events.js` route handler end-to-end: unlinked `usuarioId`
    → `notified: 0`, zero calls to `sendEachForMulticast`; linked
    `usuarioId` with two Apoderados' tokens present → notifies only the
    linked Apoderado's token(s), never the other Apoderado's.
  - **Git note**: this phase's work was committed on a separate branch
    (`relacionUsuarios`) and `main` was later fast-forwarded onto it
    (`git merge --ff-only`, no merge commit) — if `main` and a feature
    branch ever look out of sync again, check `git log --oneline --all` /
    `git reflog` before assuming code was lost or reverted.

- **Phase 7** (done): the Usuario role can generate 4 fixed test events by
  hand (no ESP32 yet) — `EMERGENCY`, `FOOD`, `BATHROOM`, `HELP`, defined in
  new `SimulatedEvent.kt` (top-level, next to `EventConstants.kt`/`Role.kt`).
  Purely a UI feature that reuses the entire Phase 5/6 pipeline
  unchanged — `ble/` untouched, `backend/` (Node/Render) untouched.
  - `UsuarioBleScreen` gained an "Enviar alerta" section (4 large buttons,
    disabled while a send is in flight) shown unconditionally, not gated on
    BLE connection state — same reasoning as the Phase 6 `usuarioId` code
    display. Each button calls the *same*
    `BackendEventRepository.notifyEvent(type, message, deviceId, usuarioId)`
    used by the real BLE-triggered flow — same repository instance, same
    `notifyState`, no parallel/second state machine.
  - `BackendEventRepository` behavior fix (not a signature change): it used
    to treat any HTTP 2xx as `Success` without reading the response body,
    so a `{"ok":true,"notified":0}` (Usuario not yet linked to an
    Apoderado) showed the same "success" text as an actual delivery — now
    it parses `notified` and only reports `Success` when `notified > 0`;
    `notified == 0` surfaces as `Error("El Usuario todavía no está
    vinculado a un Apoderado.")`. Also stopped echoing the raw HTTP
    error/response body to the user (`401` → "API key inválida o
    ausente."; anything else → a generic "no se pudo enviar" message) —
    tightens what Phase 5/6 already documented ("don't show raw
    stack/response traces to the user").
  - Known limitation at the time (foreground-only per-event title/emoji,
    since `events.js` hardcoded generic notification text) — **resolved in
    Phase 8**, see below.

- **Phase 8** (done): the per-event title/emoji now shows correctly
  regardless of the Apoderado's app state (foreground/background/killed),
  and a real (not simulated) bug in the background/killed tap-to-open path
  got fixed along the way.
  - **`backend/src/routes/events.js`**: new `EVENT_TITLES` lookup
    (`EMERGENCY`/`FOOD`/`BATHROOM`/`HELP` → their emoji+label, must stay in
    sync with `SimulatedEvent.kt`'s `notificationTitle`). For those 4
    known types, `notification.body` is the incoming `message` field
    directly (not a duplicated copy of `SimulatedEvent.kt`'s text — avoids
    drift between the two files). Any other `type` (today, only the real
    ESP32's `ESP32_EVENT`) keeps the exact Phase 5 generic
    title/body — verified byte-for-byte unchanged via a temporary in-memory
    test (same technique as Phase 6's, not committed). `data.type`/
    `message`/`deviceId`, the response JSON shape, `vinculaciones`/
    `apoderadoTokens` lookup logic, `middleware/apiKey.js`, and
    `firebaseAdmin.js` are all untouched.
  - **Bug found and fixed, not part of what was asked but blocking the
    phase's own background/killed test goal**: `MainActivity.handleNotificationIntent()`
    only ever read the `EXTRA_EVENT_*`-namespaced intent extras — which
    only exist when *our own code* builds the notification's `PendingIntent`
    (the foreground path, `GuardianNotifications.buildEventNotification`).
    For background/killed, Android auto-displays the notification and, on
    tap, launches `MainActivity` with the FCM `data` payload's *raw* keys
    (`"type"`/`"message"`/`"deviceId"`) as extras — keys that never matched
    `EXTRA_EVENT_*`. Net effect since Phase 5: tapping a background/killed
    notification opened the app but silently failed to populate
    `GuardianNotificationCenter`, so `ApoderadoScreen` never showed "Último
    evento" for that path. Fixed with a fallback read (`intent.getStringExtra(EXTRA_EVENT_TYPE)
    ?: intent.getStringExtra("type")`, same for the other two fields) —
    localized entirely to `MainActivity.kt`, no other file touched for
    this.
  - `GuardianFirebaseMessagingService`'s foreground-only `SimulatedEvent`-based
    title/body computation (added in Phase 7) is now redundant (the server
    already sends the right text) but harmless — computes the identical
    string, so it was left in place rather than removed, per this phase's
    "minimal change" instruction. Its class doc comment was updated to
    stop describing the now-fixed limitation as current.
  - Nothing changed in `GuardianNotifications.kt` (channel id/importance/icon),
    `GuardianNotificationCenter.kt`, `NotifiedEvent.kt`, `AndroidManifest.xml`,
    or `LocalIdentity.kt` — all already correct for this phase's goals
    (channel `IMPORTANCE_HIGH`, `POST_NOTIFICATIONS` declared, single
    channel id shared between the manifest meta-data and
    `GuardianNotifications.ensureChannel`).

- **UI/UX redesign** (not a numbered "Fase", a visual-only pass done with
  the `mobile-app-ui-design` skill, iterated twice; zero logic/state/
  repository changes — same `LaunchedEffect`s, same function signatures,
  same backend/BLE/FCM wiring throughout):
  - New `app/.../ui/theme/` package (`Color.kt`, `Theme.kt`) — a real
    `GuardianAppTheme(darkTheme = isSystemInDarkTheme(), ...)` composable
    replacing the bare, unbranded `MaterialTheme { }` every screen used
    since Phase 1 (default Compose purple, and — since nothing ever passed
    an explicit `colorScheme` — no actual dark-mode support despite
    `themes.xml`'s legacy `Theme.MaterialComponents.DayNight...` implying
    there should be one). `MainActivity.setContent` and its `@Preview` now
    use `GuardianAppTheme` instead of bare `MaterialTheme`.
  - **v2 palette (current)**: monochrome black/white + one red accent,
    requested explicitly by the user ("botones en negro", referencing a
    fintech/crypto-wallet visual style). `ColorScheme.primary` is near-black
    in light mode / near-white in dark mode (`InkLight`/`InkDark` in
    `Color.kt`) — since Material3's default `Button`/`OutlinedButton` use
    `primary` for their fill/border+content color, this alone makes nearly
    every button in the app render black-on-white (or inverted in dark
    mode) *without* per-button color overrides. `error` (red) stays the
    **only** accent color, reserved for the Emergency button and error
    states (60/30/10 rule from the skill). A fixed "hero" color pair
    (`HeroContainer`/`HeroOnContainer`, always near-black+white regardless
    of light/dark theme — a deliberate constant, not theme-inverted like
    `primary`) is used for the "Tu código de Usuario" card and the role
    badges in `RoleSelectionScreen`, evoking a wallet-app "balance card".
    `ButtonShape` (a fully-rounded pill, `RoundedCornerShape(percent = 50)`)
    is applied explicitly to every `Button`/`OutlinedButton` call site.
    `successColor()` (green) is unchanged — still not a Material3
    `ColorScheme` role, resolved separately.
  - New `app/.../ui/components/Animations.kt`: `Modifier.pressScale(interactionSource)`
    (a tactile ~5% shrink while a `Button`/`Card` is pressed, via
    `collectIsPressedAsState()` + `animateFloatAsState` — remember the
    `getValue`/`setValue` operator imports for the `by` delegate here, easy
    to forget) and `AnimatedStatus(targetState, content)` (a generic
    `AnimatedContent` fade+slide wrapper reused by every Idle/Sending/
    Success/Error status block across all three screens, so state changes
    animate instead of snapping). Note: `Button`'s `content` lambda has a
    `RowScope` receiver (`@Composable RowScope.() -> Unit`) — matters if a
    content lambda is extracted to a local `val` and shared between a
    `Button` and an `OutlinedButton` call, as `AlertaButton` does.
  - `RoleSelectionScreen.kt`: two large tappable `Card`s (dark hero badge +
    title + one-line description, with `pressScale`) instead of two plain
    `Button`s.
  - `UsuarioBleScreen.kt` / `ApoderadoScreen.kt`: each functional block
    (código de vinculación, alertas, conexión BLE / estado de
    notificaciones, vinculación, último evento) is its own `Card` instead
    of a flat `Column` separated by `HorizontalDivider()`s. Status text is
    color-coded (`successColor()` for ✓, `colorScheme.error` for ✗/✕,
    `onSurfaceVariant` for in-progress/neutral) and wrapped in
    `AnimatedStatus`. "Cerrar sesión" is an `OutlinedButton` (de-emphasized
    — it's not either screen's primary action). `SimulatedEvent.kt` has one
    presentation-only field, `isCritical` (`true` only for `EMERGENCY`) —
    does **not** travel to the backend, only makes `AlertaButton` render
    that one button filled with `colorScheme.error` (red, `pressScale`)
    while the other three render as `OutlinedButton`s (black border/text
    via `primary`, `pressScale`), so Emergencia visually stands out from
    Comida/Baño/Ayuda without a second accent color.
  - No new dependencies: no icon library was added — the app already used
    emoji as its icon system since Phase 5/7 (🚨🔔✓✕ etc.), which the skill
    explicitly endorses ("use icons, emojis... to make information
    digestible"), so the redesign leaned on that instead of adding
    `material-icons-core`/`-extended`.

- **Phase 9** (done): bottom-tab navigation (Inicio/Historial/Ajustes,
  `design/guardian-navigation.png` reference — adapted, not copied) for
  **both** roles — the user explicitly chose this over "Usuario only" or
  "Apoderado only" after the brief's own text contradicted the reference
  image about which role "Inicio"/its alert buttons belonged to (worth
  re-reading the chat if this phase's scope ever looks surprising).
  - **New packages** (flat, not nested under `ui/`, per the user's requested
    `navigation/`/`screens/`/`components/` shape): `navigation/` (`AppTab.kt`,
    `BottomNavBar.kt` — a plain `enum` + `when`, no `androidx.navigation`
    dependency added, same reasoning Phase 2 used for the original role
    switch: sibling screens, no back-stack/args/deep-link need) and
    `screens/usuario/`, `screens/apoderado/` (3 screens each: `*InicioScreen.kt`,
    `*HistorialScreen.kt`, `*AjustesScreen.kt`). Shared list/card/status
    building blocks went into the **existing** `ui/components/` (not a new
    top-level `components/`) since that package already existed for exactly
    this purpose (`Animations.kt`, from the redesign pass) — extended with
    `SectionCard.kt` (promoted out of a private copy that used to live only
    in `ApoderadoScreen.kt`), `SettingsRow.kt`, `HistorialEntryCard.kt`,
    `FilterChipsRow.kt`, `EventCategory.kt` (emoji+color per event `type`,
    the one place in the app with more than the primary/error accents —
    category tags on an otherwise-monochrome list, not a theme change), and
    `DateGrouping.kt` (pure Kotlin, no Compose — groups a
    newest-first-sorted list into "Hoy"/"Ayer"/date via `java.time`, native
    since API 26, no desugaring needed).
  - `UsuarioBleScreen.kt` / `ApoderadoScreen.kt` are now thin "hosts": they
    still own every repository/singleton `remember{}`, the BLE
    `DisposableEffect`, and the BLE-event `LaunchedEffect` **completely
    unchanged**, plus a `var selectedTab by remember { mutableStateOf(AppTab.INICIO) }`
    and a `Scaffold(bottomBar = { BottomNavBar(...) })` that dispatches to
    the 3 screens per role. Because the repositories live *above* the
    `when(selectedTab)`, switching tabs never recreates BLE/FCM/vinculación
    state — only per-tab UI state (e.g. a selected filter chip) resets on
    revisit, a deliberate, documented trade-off of not using
    `androidx.navigation`. `MainActivity.kt` did not need to change at all
    (still calls `UsuarioBleScreen(onCerrarSesion=...)` /
    `ApoderadoScreen(onCerrarSesion=...)` exactly as before).
  - **Historial — the minimal-modification analysis the user asked for
    before writing code**: `events` in Firestore has `allow read: if false`
    (no client can query it) and doesn't even record which Usuario/Apoderado
    a document belongs to — reconstructing history from there would have
    needed a rules change *and* a schema change, i.e. not minimal, and the
    user explicitly vetoed inventing new persistence without justifying it
    first. Instead, both histories are built from data the app already
    computes on-device: `GuardianNotificationCenter` (Apoderado — every FCM
    event the phone was ever told about, Phase 5) gained a `history: List<NotifiedEvent>`
    (`mutableStateListOf`, newest-first) alongside the pre-existing
    `lastEvent`, appended in the same `onEventReceived` both existing call
    sites (`GuardianFirebaseMessagingService`, `MainActivity`) already call
    — neither needed to change. `BackendEventRepository` (Usuario — every
    alert *this device sent*, Phase 5/7) gained `sentHistory: List<SentEvent>`
    the same way, recorded through one new private `setTerminalState(type,
    message, state)` that replaced the repeated `notifyState = ...`
    assignments in `notifyEvent()` (still the exact same states, same
    method signature, same HTTP call). `NotifiedEvent` gained a
    `receivedAtMillis: Long = System.currentTimeMillis()` field with a
    default value specifically so its two existing constructor call sites
    didn't need to change.
  - **Known, accepted limitation** (documented in both files' doc comments):
    both histories live in memory only — they reset if the process dies
    (not just backgrounded), since neither is persisted. The natural next
    step, if durability across restarts is wanted, is `SharedPreferences`
    (same mechanism `LocalIdentity.kt` already uses) — not implemented now,
    to keep this phase's change to "the minimum necessary," per the user's
    own instruction.
  - Filter chips (`FilterChipsRow`, Material3 `FilterChip`, no new
    dependency) use `SimulatedEvent?` as the filter value (`null` = "Todos")
    on both Historial screens — an event whose `type` isn't one of the 4
    known ones (e.g. a real future `ESP32_EVENT`) always shows under
    "Todos" with the generic 🔔/"Evento" fallback from `categoryFor()`,
    never matches a specific chip.

Still not implemented (explicitly deferred to later phases — don't add unless
asked): Firebase Authentication (would replace both `EVENTS_API_KEY` and the
unauthenticated pairing code — see Phase 6 above), a general-purpose
backend/API beyond this one small events endpoint, sensors, geolocation,
SMS/WhatsApp/email, QR-code or invitation-based pairing (today's 6-char code
is deliberately the simple version), real ESP32/BLE-triggered simulated
events (Phase 7's buttons are a stand-in until the hardware exists), an admin
panel, `androidx.navigation` (still a plain enum + `when`, see Phase 9),
persisted history across app restarts (Phase 9's history is in-memory only
— see the "known, accepted limitation" note there). The next phase is final
testing, fixes, and producing the release APK for the university presentation.

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
  `backend/` below) and `SimulatedEvent.kt` (Phase 7, the 4 test-alert
  types for the "Enviar alerta" buttons — unrelated to `EventConstants.kt`:
  that one describes the real ESP32 event, this one describes the manual
  stand-ins used until the hardware exists); BLE logic separated into the `ble/` subpackage
  (`BleManager.kt`, `BlePermissions.kt`, `BleConstants.kt`, `BleModels.kt`);
  Firebase/Firestore logic in the `firebase/` subpackage
  (`FirebaseRepository.kt`, `EventUploadState.kt`, `FirebaseUtils.kt`,
  `VinculacionRepository.kt`, `VinculacionState.kt`); FCM *receiving* logic
  in the `fcm/` subpackage (`FcmTokenRepository.kt`,
  `GuardianFirebaseMessagingService.kt`, `GuardianNotifications.kt`,
  `GuardianNotificationCenter.kt`, `DeviceRegistrationState.kt`,
  `NotifiedEvent.kt`); the HTTP client that talks to the `backend/` project
  below is in the `backend/` subpackage (`BackendEventRepository.kt`,
  `BackendNotifyState.kt`, `SentEvent.kt` — actually a top-level class in
  `BackendEventRepository.kt`, not its own file) — same subpackage name as
  the top-level `backend/` Node project one level up, don't confuse the
  two; anonymous per-install pairing IDs (Phase 6) in the `identity/`
  subpackage (`LocalIdentity.kt`); Phase 9 added `navigation/` (`AppTab.kt`,
  `BottomNavBar.kt`), `screens/usuario/` + `screens/apoderado/` (3 screens
  each: Inicio/Historial/Ajustes), and extended the existing `ui/theme/`
  (Phase 8 redesign) with `ui/components/` — now `Animations.kt` plus
  `SectionCard.kt`, `SettingsRow.kt`, `HistorialEntryCard.kt`,
  `FilterChipsRow.kt`, `EventCategory.kt`, `DateGrouping.kt`.
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
