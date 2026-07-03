# Android app

A thin Android wrapper around the dashboard. The repo-root `index.html` remains the
single source of truth — a Gradle task copies it into app assets at build time, so
there is no second copy to keep in sync.

## How it works

- `MainActivity` hosts a full-screen `WebView` with JavaScript and DOM storage enabled.
- The page is served via `WebViewAssetLoader` from the
  `https://appassets.androidplatform.net` origin (not `file://`), which gives it:
  - working `localStorage` for the saved API key + channel list, and
  - a real https origin for YouTube Data API calls.
- Channel/video links (`target="_blank"`) open externally in the browser or YouTube app.
- Back button navigates the WebView history before exiting the app.

## Build & run

Requirements: Android Studio (or the Android SDK command-line tools) and JDK 17+.
Min SDK 26 (Android 8.0), target SDK 35.

- **Android Studio:** open the `android/` folder, let it sync, press Run.
- **Command line:**

  ```bash
  cd android
  ./gradlew assembleDebug          # APK at app/build/outputs/apk/debug/
  ./gradlew installDebug           # install on a connected device/emulator
  ```

## API key restrictions

If your YouTube Data API key uses an *HTTP referrers* application restriction, add
`https://appassets.androidplatform.net/*` to the allowlist — that is the origin the
in-app page runs on. (Or use *API restriction → YouTube Data API v3* with no
application restriction, as the root README suggests.)
