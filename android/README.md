# DuoPlan 💜

A small Android app for two people to decide **what to eat** and **what to do**.
One partner suggests; the other **approves**, **declines**, or **asks for another
idea**. Everything syncs through a **shared Google Calendar**, so both phones —
and any other calendar app you use — stay on the same page.

> Part of this repo's `android/` directory. The single-file YouTube dashboard in
> the repo root is unrelated and untouched.

## Why a shared calendar instead of a server?

DuoPlan has no backend to run or pay for. Each suggestion is stored as an **event
on a Google Calendar you both share**:

| In the app            | On the calendar                                            |
|-----------------------|------------------------------------------------------------|
| Pending suggestion    | `tentative` event, tagged with private extended properties |
| Approved              | `confirmed` event (shows up as a normal plan for both)     |
| Declined              | `cancelled` event (kept as history until removed)          |
| "Ask for another"     | `tentative` event flagged `REVISION_REQUESTED`             |

Because both partners point the app at the **same** calendar, a write by one
shows up for the other. Approved plans also appear in Google Calendar itself, on
watches, widgets, and notifications — for free.

## Architecture

Single module, MVVM, Jetpack Compose, no Hilt (a tiny manual DI container).

```
app/src/main/java/com/duoplan/app/
├── DuoPlanApp.kt            Application + AppContainer (manual DI)
├── MainActivity.kt          Compose host, navigation, Google auth launcher
├── auth/
│   └── GoogleAuthManager.kt Play Services AuthorizationClient (OAuth access tokens)
├── data/
│   ├── SettingsRepository.kt  DataStore: signed-in email + chosen calendar
│   ├── PlanRepository.kt      Maps Calendar events <-> Plan; all reads/writes
│   ├── model/Plan.kt          Domain model (PlanCategory, PlanStatus, Plan)
│   └── remote/
│       ├── CalendarApi.kt     Retrofit interface for Calendar API v3 + userinfo
│       ├── Dtos.kt            Calendar JSON DTOs (kotlinx.serialization)
│       └── NetworkModule.kt   OkHttp + bearer-token interceptor + Retrofit
└── ui/
    ├── MainViewModel.kt     UiState + all actions, reloads after each mutation
    ├── Format.kt            "Tonight · 7:00 PM" etc.
    ├── components/PlanCard.kt
    └── screens/             Auth, Home, ComposeRequest (new/revise), Detail, Settings
```

Every Google API call goes through `CalendarApi`; `AuthInterceptor` attaches a
fresh OAuth access token (auto-refreshed by Play Services) to each request.

## One-time Google Cloud setup

The app talks to the Google Calendar API on the user's behalf, so it needs an
OAuth client. This is a per-developer step (the SHA-1 is tied to your signing
key), which is why it isn't checked in.

1. **Create / pick a project** at <https://console.cloud.google.com>.
2. **Enable the API:** APIs & Services → Library → enable **Google Calendar API**.
3. **OAuth consent screen:** User type *External*. Add the scopes
   `.../auth/calendar`, `.../auth/userinfo.email`, `.../auth/userinfo.profile`.
   Add **both partners' Google accounts** as *Test users* (no verification needed
   while in testing).
4. **Create an OAuth client ID** → type **Android**:
   - Package name: `com.duoplan.app`
   - SHA-1 of your debug key:
     ```bash
     keytool -list -v -alias androiddebugkey \
       -keystore ~/.android/debug.keystore -storepass android -keypass android
     ```
   (Add a second client for your release SHA-1 when you sign a release build.)

No client secret or `google-services.json` is required — `AuthorizationClient`
identifies the app by package name + SHA-1 and returns access tokens directly.

## Create the shared calendar (one time, per couple)

1. In Google Calendar on the web, one partner: **Other calendars → +→ Create new
   calendar** (e.g. "Us").
2. Open that calendar's **Settings → Share with specific people**, add your
   partner, and grant **"Make changes to events"**.
3. The other partner accepts the invite so the calendar appears in their account.
4. In DuoPlan, each of you signs in and selects this calendar on first launch
   (or later in Settings).

## Build & run

Requires **Android Studio** (Ladybug or newer) and the **Android SDK** (compile
SDK 35). JDK 17 is bundled with Android Studio.

```bash
# From android/, with ANDROID_HOME / local.properties pointing at your SDK:
./gradlew :app:installDebug
```

Or just open the `android/` folder in Android Studio, let it sync, and press Run.

> **Note:** This project was authored in a sandbox where Google's Maven repo and
> the Android SDK were network-blocked, so it has **not been compiled there**.
> The code is reviewed and self-consistent, but expect to resolve the usual
> first-sync nudge (e.g. an SDK/AGP version bump for your installed tooling).

## Roadmap / not in v1

- **Push notifications.** v1 refreshes on open, after each action, and via the
  refresh button. Real-time nudges would mean FCM + a Calendar push channel
  (`events.watch`), which needs a tiny server endpoint to receive webhooks.
- **Per-partner avatars / names** (currently "you" vs "your partner").
- **Suggestion history & favorites** ("the usual").

`minSdk` 26 · `targetSdk`/`compileSdk` 35 · Kotlin 2.1 · Compose.
