# ZCode

[English](README.md) | [简体中文](README.zh-CN.md)

A fullscreen WebView wrapper that turns any website into an Android app. The site does **not** need to be a PWA.

## Features

- 📷 **QR setup** — scan a QR code to configure the URL on first launch (manual input also supported)
- 🖥 **Immersive fullscreen** — status bar and navigation bar hidden, no browser UI
- 🔒 **Persistent settings** — once configured, the app opens straight into the site
- ✋ **Long-press anywhere** to reopen setup and change the URL
- 🔙 Back key navigates web history
- ✨ Branded splash screen (once per cold start, with entrance animation)
- Full Cookie / localStorage / JS support; external schemes (`tel:`, `mailto:`…) handed to the system
- Cleartext HTTP allowed, so intranet URLs work (disable via `usesCleartextTraffic` in the Manifest if desired)

## Build

```bash
gradle wrapper && ./gradlew assembleDebug
```

Or simply open this directory in Android Studio.

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Customization

| What | Where |
|---|---|
| App name | `android:label` in `app/src/main/AndroidManifest.xml` |
| Package id | `applicationId` in `app/build.gradle.kts` |
| Splash texts / email | `app/src/main/res/layout/activity_main.xml` (splashScreen section) |
| Accent color | `res/values/themes.xml`, `res/drawable/bg_btn_*.xml` |
| Icon | replace `res/mipmap-*/ic_launcher.png` (rescale from a 1024×1024 source) |

## License

MIT
