# MeshSync

🇧🇷 [Leia em português](README.md)

MeshSync is a reimplementation of the Syncthing Android app in Kotlin Multiplatform + Compose Multiplatform
(Android is the only target for now), with the "Neon Grid" redesign interface and a GitHub-style
conflict resolver.

## Highlights

Two features that `syncthing-android` is missing and that this project focuses on:

- **Conflict resolution inside the app**: compare the two versions, pick section by section or edit by hand,
  without opening an editor or deleting files blindly. See [Conflict resolution](#conflict-resolution).
- **Ignored files for everyone**: choose what a folder should not sync with one tap (temporary files, system
  leftovers, thumbnails) or by typing a name or type, and open the **advanced mode** only if you want to edit
  the `.stignore` directly. See [Ignored files](#ignored-files).

## How it works

Same as `syncthing-android`: the official Syncthing core (Go) runs as a child process and the app
talks to it through the [REST API](https://docs.syncthing.net/dev/rest.html) on `127.0.0.1`.
The app generates the API key and the port and passes them to the core (`--gui-address`, `--gui-apikey`),
so it never needs to read `config.xml`. A foreground service (`specialUse`) keeps the process alive.

## Modules

| Module | Contents |
|---|---|
| `build-logic` | Convention plugins (`syncthing.kmp.library`, `.compose`, `.feature`) |
| `core:domain` | Models, `Result`/`DataError`, the `SyncthingRepository`, `SyncthingCore` and `ConflictSummary` contracts |
| `core:data` | Ktor REST API client, DTOs, repository with long-polling on `/rest/events` |
| `core:design-system` | Tokens, fonts (Chakra Petch, IBM Plex), chamfered corners, icons and components; MeshSync logo as the app icon (`androidApp/res/drawable/ic_launcher_*`) |
| `core:presentation` | `UiText`, formatters, `ObserveAsEvents`, `PlatformActions`, core status callout |
| `feature:home` | Home: status and device map |
| `feature:sharing` | Who shares what (devices ↔ folders graph) |
| `feature:folders` | Folder list, folder offers, add/accept folder, ignored files |
| `feature:files` | File manager: `domain` (`FileManager`), `data` (Okio), `presentation` |
| `feature:devices` | Devices (pause, rename, remove), approving pending ones, 3-step flow with QR (suggests the machine name when it has already asked to connect) |
| `feature:onboarding` | First-run introduction (explains the app and asks whether to share or fetch a folder) |
| `feature:settings` | Settings (name, ID, start/stop the core, folder auto-accept, light/dark theme, language) |
| `feature:conflicts` | `domain` (Myers diff, merge model), `data` (on-disk scan and resolution), `presentation` |
| `app` | NavHost, tab bar, aggregation of the Koin modules |
| `androidApp` | Activity, service, core process manager, QR scanner (ZXing) |
| `desktopApp` | Windows app (JVM): tray, core process, autostart, MSI/EXE installer |
| `syncthing-native` | Tasks that build the Go core (Android and Windows) |

## Conflict resolution

Syncthing keeps the losing version as `name.sync-conflict-YYYYMMDD-HHMMSS-<device>.ext`.
The app scans the folders (again whenever a folder's index changes), identifies who made each version
(`/rest/db/file` for the current one, the short ID in the name for the copy) and classifies it:

- **Text** (UTF-8, up to 2 MB): line-based diff; each differing section is resolved with
  "this phone", "the other device" or "both"; each button has the color of the section it picks
  (green for the first version, blue for the second). The "All from …" buttons show each version’s date and time. There is a final-result view and **manual editing**
  starting from text with `<<<<<<<`/`=======`/`>>>>>>>` markers, as on GitHub; it only finishes once the markers are gone.
- **Binary / image**: pick one version or keep both (`name (copy).ext`), with a preview for images.

Conflicts whose two versions are byte-for-byte identical (for example after moving a folder) are
resolved automatically during the scan: the older file is kept and the other goes to `.stversions/`.
If the texts have no differing section but the bytes are not equal, the list and the resolver screen say why
(`TextDifference`: line endings or the final line break).

To avoid conflicts nobody edited, folders are created with `ignorePerms = true` (Android storage reports every
file as `0660`; without the flag the phone announces a metadata-only version for each received file, which races
with the next edit made on another device). `FolderPermissionsMigrator` fixes existing folders.

Nothing is deleted: replaced versions go to `.stversions/` inside the folder, using the same name
format as Syncthing's own versioning (which never syncs that directory). If the file changes on disk
while the user is deciding, the write is refused.

There is no common ancestor (Syncthing does not keep one), so every merge is two-way.

## Ignored files

Each card on the **Folders** tab has an **Ignored files** button that opens the editor for that folder's
`.stignore` rules. The app reads and writes the rules through the REST API (`GET`/`POST /rest/db/ignores`)
without touching the file; the core rescans on its own after saving. Rules apply only on the device where they
were saved, and files that were already synced are not deleted: they just stop being synced.

- **Simple mode** (default): on/off quick choices (temporary files, Windows/Mac leftovers, Android thumbnails
  and trash, programming folders), a field to ignore a file, a folder or a type (`*.mp3`), and the list of what
  is ignored, with a button to remove each item.
- **Advanced mode** (button at the bottom of the screen): a text editor with one rule per line, like `.stignore`
  itself, and a syntax cheat sheet: `*`, `**`, `?`, `{a,b}`, a leading `/` for the root, `!` for exceptions,
  `(?i)` to ignore case, `(?d)` to allow deleting the folder and `//` for comments. Syncthing uses wildcard
  (glob) patterns, **not regular expressions**.

Both modes edit the same text, so switching between them loses nothing; rules the simple mode does not
understand (exceptions, `(?i)`, wildcards, comments) show up as "advanced rule" and are kept as they are.
The logic lives in `core:domain` (`IgnoreRules`, `IgnorePreset`) and the screen in `feature:folders` (`ignore` package).

## File manager

The **Browse files** button below the list on the **Folders** tab opens `FilesRoot` (`feature:files`) on the device's
shared storage. Tapping opens a folder; long-press selects, and the bottom bar offers **Copy**, **Move**, **Rename**,
**Delete** and **Share** (files only, through the system dialog via `PlatformActions.shareFiles` and a `FileProvider`);
**Paste here** appears when something is copied or cut. Pasting never overwrites (`name (1).ext`) and a folder cannot go
into itself. Folders MeshSync syncs (path equal to a folder in `SyncOverview.folders`) use
`NgFolderIcon(shared = true)`, the folder icon with the app mark inside. For a folder that is not synced yet, **Sync**
navigates to `AddFolderRoute(path = …)`, which pre-fills the path and the name. It needs all-files access; operations are
plain file operations (Okio) in `feature:files:data`, not going through the core.

## Known folders

When a device you already added offers a folder you already share with another device (same ID, Send & Receive
type), the app adds it to the folder without asking and shows a brief notice. Offers for new folders, unknown
devices or Send Only/Receive Only folders still ask you. The option is under **Settings → Known folders** (on by
default; off, everything asks again). The pure rule is `knownFolderOffers` in `core:domain`; `FolderOfferAutoAccepter`
in `core:data` applies it and is started by `SyncthingApplication`. Preferences live in `AppPreferences`
(`core:domain`), stored in SharedPreferences.

## Declined offers

**Decline** on a folder offer is stored in the core (the device's `ignoredFolders`, through
`SyncthingRepository.declineFolderOffer`), so it does not come back when the device reconnects. The Folders tab lists
them under **Declined offers** (`SyncOverview.declinedFolders`); **Accept** calls `restoreDeclinedFolder` and opens the
usual accept-folder flow.

## Introduction (onboarding)

On first launch the app shows three short screens explaining how it works, a screen asking for all-files access (without it the core cannot
start, not even to connect to another device; `PlatformActions.hasStorageAccess` is re-read on every resume) and a final question: share a folder from
this device (opens *Add folder*), get a folder from another device (opens *Add device*) or decide later. Finishing or
tapping "Skip" stores `AppPreferences.onboardingCompleted`, so it never returns. The current page survives process
death through `SavedStateHandle`. The *Add folder* and *Add device* forms observe `SyncthingCore.state`: until the core is
`Running` they show the `CoreStatusCallout` and block save/confirm.

## Choosing the folder on the phone

In *Add folder*, the **Choose or create folder** button calls `PlatformActions.pickFolder`, which on Android opens the system
folder chooser (`OpenDocumentTree`, with a create-folder option). The chosen document ID becomes a file path
(`documentIdToPath` in `core:presentation`: `primary:` → internal storage, `XXXX-XXXX:` → SD card), since Syncthing works with
paths and the app holds all-files access. The text field stays editable.

## Light and dark theme

The app has two palettes, `NgColors.Dark` and `NgColors.Light` (the light one darkens the neon accents so they have
contrast on a white background). It follows the device mode by default; **Settings → Theme** forces **Light** or
**Dark** (**Device** follows the system again). The choice is `ThemeMode` in `AppPreferences`; `MainActivity` resolves
light/dark with `ThemeMode.isDark`, styles the system bars and passes the result to `App` → `SyncthingTheme`.
Components only use colors from `NgTheme.colors`, so every screen works in both themes.

## Languages

The app is translated into Portuguese (`pt`), English (`en`), Spanish (`es`), Italian (`it`), German (`de`),
French (`fr`) and Greek (`el`). It follows the device language; any other language falls back to **English**,
the default. **Settings → Language** overrides this inside the app only (**Device language** follows the system
again); `MainActivity` applies the choice in `attachBaseContext` and recreates itself when it changes. Texts live in each module's `composeResources/values/strings.xml` (English, the default) and in
`values-<language>/strings.xml` (the translations); `androidApp` uses the same layout in `res/` (notification, QR scanner)
and declares the languages in `res/xml/locales_config.xml` (Android 13+ per-app language picker).
`TranslationsTest` fails if a translation is missing, extra, or has placeholders (`%1$s`) that differ from English.
To add a language: create `values-<language>` in every module, add it to `locales_config.xml`,
to `localeFilters` (`androidApp/build.gradle.kts`) and to the list in `TranslationsTest`.

## Play Store listing

`store-listing/<locale>/` (`en-US`, `pt-BR`) holds `title.txt`, `short-description.txt` (≤ 80 characters) and
`full-description.txt` (≤ 4000); `store-listing/feature-graphic.png` is the feature graphic (1024×500) and `app-icon.png` (+ `.svg`) the 512×512 icon, square with no rounded corners (Play applies the mask).

`store-screenshots/en/{phone,tablet-7,tablet-10}/` holds the 8 screenshots of each format (dark mode):
phone 1080×2160, 7" tablet 1200×1920 and 10" tablet 1600×2560. To redo them: emulator with the format's
`wm size` and `wm density 320`/`420`, `adb shell cmd uimode night yes`, install the debug build, enable demo mode,
set the language with `adb shell cmd locale set-app-locales com.adamfoerster.meshsync.debug --locales en-US` and capture with
`adb exec-out screencap -p`. Only `en/` exists for now.

## Building

```bash
./gradlew :androidApp:assembleDebug
```

The APK only really syncs with the embedded core. Build it once (needs git, Go and the Android NDK):

```bash
./gradlew :syncthing-native:buildNative -Pabis=arm64-v8a,x86_64
```

This clones Syncthing `v2.1.6` into `syncthing-native/go/` and writes `androidApp/src/main/jniLibs/<abi>/libsyncthing.so`
(both outside git). Without the binary, the app shows "Syncthing core missing".

## Windows

MeshSync also runs on Windows (`desktopApp`): the same Compose app, with the `syncthing.exe` core as a child process and
a tray icon (the app icon; status is shown in the tooltip). Data and configuration live in `%LOCALAPPDATA%\MeshSync`; autostart uses `HKCU\...\Run` with `--minimized`.
OS-specific code stays in `desktopApp` only, so macOS can follow.

```bash
./gradlew :syncthing-native:buildWindows   # syncthing.exe in desktopApp/resources/windows-x64 (needs Go >= 1.26.2)
./gradlew :desktopApp:run                  # run the app
./gradlew :desktopApp:packageMsi :desktopApp:packageExe   # installers in desktopApp/build/compose/binaries/main/{msi,exe}
```

The installer is per-user (no administrator) and **unsigned**, so SmartScreen warns. The `upgradeUuid` in
`desktopApp/build.gradle.kts` must never change: it is how Windows recognises a newer version as an upgrade.

### Release build (R8 + signing)

The release build uses R8 (obfuscation, minification and `shrinkResources`; rules in `androidApp/proguard-rules.pro`).
To sign it, point `meshsync.signing` (in `~/.gradle/gradle.properties`, never in the repo) at a `.properties` file with
`storeFile`, `storePassword`, `keyAlias` and `keyPassword`; without it the APK is left unsigned.
Upload `mapping.txt` (`androidApp/build/outputs/mapping/release/`) to Play/Amazon to deobfuscate crashes.

```bash
./gradlew :androidApp:bundleRelease   # AAB for Google Play
./gradlew :androidApp:assembleRelease # APK (Amazon Appstore)
```

### Demo mode (debug only)

To see the interface without the core: create the flag file and reopen the app. The REST API is simulated, but the
conflicts are real files in `Android/data/<app>/files/Documents`, so detection and resolution run for real.

```bash
adb shell run-as com.adamfoerster.meshsync.debug touch files/demo
```

## Tests

```bash
./gradlew testAndroidHostTest :desktopApp:test
```

## Versioning and contributing

Current version: **1.13.0** (`app.version` in `gradle.properties`, SemVer). History in [CHANGELOG.md](CHANGELOG.md) (Portuguese).

Every change needs tests, a version bump, a CHANGELOG entry and updated documentation;
the full rules are in [CLAUDE.md](CLAUDE.md). `./gradlew verifyVersion` (which runs before every
app build) fails if the version and the CHANGELOG disagree.

## Licenses

Chakra Petch and IBM Plex fonts under SIL OFL 1.1 (`licenses/`). The core build process follows
syncthing-android's `build-syncthing.py`.
