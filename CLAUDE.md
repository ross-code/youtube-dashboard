# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A zero-dependency, single-file dashboard for tracking public stats of YouTube channels.
All code, styles, and markup live in `index.html` — there is no build step, package
manager, server, or test suite. "Running" it means opening `index.html` in a browser.

## Running

- Open directly: `open index.html` (macOS).
- Or serve locally (needed when the API key has HTTP-referrer restrictions, since a
  `file://` page sends no referrer and gets a 403):
  ```bash
  python3 -m http.server 8000   # then open http://localhost:8000
  ```

## Architecture (all in `index.html`)

The app is a single `<script>` driving a settings-then-fetch-then-render flow:

- **Config & persistence** — `CONFIG` holds `{apiKey, channels[], recentCount}`,
  persisted to `localStorage` under key `ytDashConfig`. The API key never leaves the
  browser; it is only used to build YouTube Data API URLs. The Settings panel is the
  only writer of config.

- **`api(endpoint, params)`** — the single chokepoint for every YouTube Data API v3
  call. Injects the key, throws on non-2xx with the API's own error message.

- **`resolveChannel(token)`** — the trickiest piece. Accepts a handle (`@x`), a channel
  ID (`UC...`), or a full URL, and normalizes to a `channels.list` lookup
  (`id` / `forHandle` / `forUsername`). Falls back to `search.list` for `/c/` custom
  URLs or unresolved handles — note this fallback costs ~100 quota units vs ~1 for a
  direct lookup.

- **`recentVideos(channel, count)`** — reads the channel's uploads playlist
  (`contentDetails.relatedPlaylists.uploads`) → `playlistItems` for IDs → `videos.list`
  for per-video stats, then sorts by publish date.

- **`load()`** — orchestrates everything: renders a skeleton card per channel
  immediately, then fetches all channels in parallel via `Promise.allSettled` so one
  failing channel renders an inline error without blocking the others.

- **Rendering** — `renderCard` / `statBlock` build HTML strings. All user/API text
  passes through `esc()` before interpolation. `fmt()` abbreviates numbers (1.2M),
  with the exact value in the `title` attribute via `exact()`.

## Key constraints

- **API key only → public data only.** Subscribers, total views, video count, and
  per-video view/like/comment counts. Watch time, CTR, revenue, traffic sources, and
  demographics require OAuth + the **YouTube Analytics API** — a separate, larger build
  (auth flow + token storage, typically a backend). Do not attempt those with an API key.

- Any new YouTube API call should go through `api()` so key handling and error surfacing
  stay consistent.
