# Syncthing KT

🇧🇷 [Leia em português](README.md)

A reimplementation of the Syncthing Android app in Kotlin Multiplatform + Compose Multiplatform
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
| `core:design-system` | Tokens, fonts (Chakra Petch, IBM Plex), chamfered corners, icons and components |
| `core:presentation` | `UiText`, formatters, `ObserveAsEvents`, `PlatformActions`, core status callout |
| `feature:home` | Home: status and device map |
| `feature:sharing` | Who shares what (devices ↔ folders graph) |
| `feature:folders` | Folder list, folder offers, add/accept folder, ignored files |
| `feature:devices` | Devices, approving pending ones, 3-step flow with QR |
| `feature:settings` | Settings (name, ID, start/stop the core, folder auto-accept, language) |
| `feature:conflicts` | `domain` (Myers diff, merge model), `data` (on-disk scan and resolution), `presentation` |
| `app` | NavHost, tab bar, aggregation of the Koin modules |
| `androidApp` | Activity, service, core process manager, QR scanner (ZXing) |
| `syncthing-native` | Task that builds the Go core for Android |

## Conflict resolution

Syncthing keeps the losing version as `name.sync-conflict-YYYYMMDD-HHMMSS-<device>.ext`.
The app scans the folders (again whenever a folder's index changes), identifies who made each version
(`/rest/db/file` for the current one, the short ID in the name for the copy) and classifies it:

- **Text** (UTF-8, up to 2 MB): line-based diff; each differing section is resolved with
  "this phone", "the other device" or "both"; each button has the color of the section it picks
  (green for the first version, blue for the second). There is a final-result view and **manual editing**
  starting from text with `<<<<<<<`/`=======`/`>>>>>>>` markers, as on GitHub; it only finishes once the markers are gone.
- **Binary / image**: pick one version or keep both (`name (copy).ext`), with a preview for images.

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

## Known folders

When a device you already added offers a folder you already share with another device (same ID, Send & Receive
type), the app adds it to the folder without asking and shows a brief notice. Offers for new folders, unknown
devices or Send Only/Receive Only folders still ask you. The option is under **Settings → Known folders** (on by
default; off, everything asks again). The pure rule is `knownFolderOffers` in `core:domain`; `FolderOfferAutoAccepter`
in `core:data` applies it and is started by `SyncthingApplication`. Preferences live in `AppPreferences`
(`core:domain`), stored in SharedPreferences.

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

## Play Store screenshots

`store-screenshots/<language>/` holds the images (1080×2160, 2:1 ratio). To redo them: emulator with
`wm size 1080x2160`, install the debug build, enable demo mode, set the language with
`adb shell cmd locale set-app-locales com.adamfoerster.syncthing.debug --locales en-US` and capture with
`adb exec-out screencap -p`. Only `en/` exists today.

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

### Demo mode (debug only)

To see the interface without the core: create the flag file and reopen the app. The REST API is simulated, but the
conflicts are real files in `Android/data/<app>/files/Documents`, so detection and resolution run for real.

```bash
adb shell run-as com.adamfoerster.syncthing.debug touch files/demo
```

## Tests

```bash
./gradlew testAndroidHostTest
```

## Versioning and contributing

Current version: **1.2.0** (`app.version` in `gradle.properties`, SemVer). History in [CHANGELOG.md](CHANGELOG.md) (Portuguese).

Every change needs tests, a version bump, a CHANGELOG entry and updated documentation;
the full rules are in [CLAUDE.md](CLAUDE.md). `./gradlew verifyVersion` (which runs before every
app build) fails if the version and the CHANGELOG disagree.

## Licenses

Chakra Petch and IBM Plex fonts under SIL OFL 1.1 (`licenses/`). The core build process follows
syncthing-android's `build-syncthing.py`.
