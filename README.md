# Stapik Calendar (Android)

A read-only mobile companion to [Stapik Calendar](https://github.com/Stapik-Group/stapik-calendar), the desktop calendar app. Written in Kotlin with Jetpack Compose, styled after the same retro old-school aesthetic as the desktop version.

## Features

- **Weekly view** — swipe horizontally between weeks, each day shown as a full-width row for easy reading on a phone screen
- **Quick navigation** — jump a month forward/backward with a single tap; swiping still moves week by week
- **Today at a glance** — the current day is highlighted when you open the app
- **Cloud sync (read-only)** — fetches calendar entries from the same self-hosted API the desktop app writes to
- **Pull-to-refresh** — drag down to fetch the latest data on demand
- **Encrypted connection config** — server URL and API key are encrypted with a key held in the Android Keystore before being stored on device
- **Multilingual UI** — Polish, English and German, following the phone's system language
- **Retro aesthetic** — same raised-button, blue-navbar, grey-cell look as the desktop app

This app does not edit or delete entries — all editing happens in the desktop app. It only reads and displays whatever is currently in the cloud.

## Requirements

- Android 8.0 (API 26) or newer
- A running instance of the same self-hosted sync API used by the desktop app (see [Cloud Sync](#cloud-sync) below)

## Building

Requires [Android Studio](https://developer.android.com/studio) (Kotlin, Jetpack Compose).

```bash
git clone https://github.com/Stapik-Group/stapik-calendar-android
```

Open the project folder in Android Studio, let Gradle sync, then build/run via the green Run button or:

```bash
./gradlew assembleDebug
```

The resulting APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Installation

### Option 1 — install via Android Studio

Connect a device (USB or [wireless debugging](https://developer.android.com/tools/wireless-debugging)) or start an emulator, then hit Run in Android Studio.

### Option 2 — install a built APK manually

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or transfer the APK to the device and open it directly (requires allowing installs from the source you used).

## Cloud Sync

On first launch, open the settings menu (gear icon, bottom right) → **Connect**, and enter the same server URL and API key configured on the desktop app (**File → Connect** there).

The app fetches `calendar.json` from the server on startup, whenever you return to the calendar after connecting, and whenever you pull to refresh. There is no local cache and no offline mode on this first version — every refresh is a fresh fetch from the cloud.

Since this app is read-only, there's no write conflict to worry about on the mobile side — all conflict resolution (last-write-wins by timestamp) happens between desktop instances, as described in the [desktop app's README](https://github.com/Stapik-Group/stapik-calendar#cloud-sync).

The API must expose:
- `GET /read?filename=calendar.json` — returns `{ "content": "..." }`

## Data Storage

The server URL and API key are encrypted with AES-256/GCM using a key generated in the Android Keystore, then stored in Jetpack DataStore. The encryption key never leaves the device's secure hardware in plaintext form, and is not included in device backups. See the app's settings screen to update or clear the connection at any time.

No calendar entries are cached locally — data is only ever held in memory while the app is open.

## TODO

- [ ] Offline cache (last successful fetch, shown if a refresh fails)
- [ ] Clickable entry links
- [ ] Home screen widget
- [ ] Notifications for upcoming entries
- [ ] Better icon lol 