# YouTube Channel Dashboard

A single-file, zero-install dashboard for tracking your YouTube channels' key public
metrics. Open `index.html` in a browser — that's it.

> **Also in this repo:** [`android/`](android/) — **DuoPlan**, a native Android app
> for couples to decide what to eat and what to do (suggest → approve / decline /
> ask-for-another), syncing through a shared Google Calendar. See
> [`android/README.md`](android/README.md). It's independent of the dashboard below.

## Use it
1. Open `index.html` (double-click, or `open index.html` on macOS).
2. Click **⚙ Settings**.
3. Paste your **YouTube Data API key**.
4. Add your **channels**, one per line. Any of these formats work:
   - Handle: `@mkbhd`
   - Channel ID: `UCXuqSBlHAE6Xw-yeJA0Tunw`
   - Full URL: `https://www.youtube.com/@veritasium`
5. **Save & Load.** Your key and channel list are stored only in your browser
   (localStorage) — nothing is sent anywhere except direct calls to the YouTube API.

## Metrics shown (per channel)
- Subscribers
- Total lifetime views
- Video count
- Recent videos with views / likes / comments + publish date

## API key notes
This uses the **YouTube Data API v3** with a plain API key, which exposes *public*
stats only. Deeper analytics (watch time, impressions, revenue, traffic sources,
demographics) require OAuth + the YouTube Analytics API — a much bigger build.

**Key restrictions:** If your key has an *Application restriction* set to "HTTP
referrers", requests from a local `file://` page will be rejected (403). Easiest fix:
set Application restriction to **None** and use **API restriction → YouTube Data API v3**
instead. Or serve the folder locally:

```bash
cd youtube-dashboard
python3 -m http.server 8000
# then open http://localhost:8000
```

## Quota
Each channel costs ~3 quota units per refresh (channel + playlist + video stats lookups).
The default daily quota is 10,000 units, so you can refresh freely.
