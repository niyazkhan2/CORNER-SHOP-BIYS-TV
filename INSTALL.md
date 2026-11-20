# Corner Shop Boys — Android TV / Fire TV launcher

A clean ecru home screen for your Toshiba Fire TV (50QF5D53DB): centred CSB logo,
serif clock, a bottom bar with a red **PS5** button and **Shop**, your apps, a full
apps grid, a logo-and-clock screensaver, PS5 wake-over-Wi-Fi, and a phone remote.

Nothing here was pre-built into an `.apk` — this is the source project. Step 1 turns it
into an installable APK. Steps 2–3 put it on the TV. Step 4 is the Fire TV part: making
it the real home screen so Amazon's screen never shows. **That step is unsupported by
Amazon and can be undone by a forced update — you've said you're fine with that.**

---

## 1. Build the APK

1. Install **Android Studio** (free) on your Mac/PC.
2. **File > Open** this folder (`TVLauncher`). Let it sync — the first sync downloads
   the Gradle wrapper and the AndroidX/ZXing libraries automatically.
3. **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. The file lands at `app/build/outputs/apk/release/app-release.apk` (debug-signed, so it
   sideloads without extra steps).

No Mac/PC? Tell me and I'll walk you through building it on the TV itself, or finish the
compile for you if you can give this session access to an Android SDK.

---

## 2. Turn on sideloading on the Fire TV

**Settings > My Fire TV > Developer options**
- **Apps from Unknown Sources** → ON
- **ADB debugging** → ON (needed for step 4)

If you don't see Developer options, go to **Settings > My Fire TV > About**, highlight the
device name and click it 7 times.

---

## 3. Install the APK

Easiest is the **Downloader** app (free, in the Amazon appstore):
1. Put `app-release.apk` somewhere with a short link (Google Drive share link, Dropbox, etc.).
2. Open Downloader, type the link, let it download, then **Install**.

Or over USB/ADB from a computer on the same Wi-Fi:
```
adb connect <TV-IP>:5555
adb install -r app-release.apk
```
(The TV's IP is in **Settings > My Fire TV > About > Network**.)

At this point the launcher shows up as an app in Amazon's row — open it and it all works.
Only the next step removes Amazon's home screen.

---

## 4. Make it the home screen (remove Amazon's) — the risky part

Fire OS doesn't offer a "default launcher" setting, and Amazon re-asserts its own home on
updates. There are two routes. **Route A** is reversible and low-risk. **Route B** is the
full takeover you asked for.

### Route A — one-button access, Amazon home still behind HOME (no risk)
Leave the launcher as an app and just open it. If you want it to open on a single press,
pair a Fire TV remote button to it with an app like **Remapper**. Honest trade-off: pressing
the round HOME button still goes to Amazon. Nothing can brick here.

### Route B — full replacement (what you want; can be undone by updates)
This uses the community tool **Launcher Manager** (the current one that works around
Amazon's block as of late 2025). The method Amazon keeps patching, so it may already need a
newer build by the time you read this — check `aftvnews.com` for the latest Launcher Manager
version and whether your firmware is still exploitable.

1. **Freeze updates first**, or Amazon will undo this overnight. With ADB connected:
   ```
   adb shell pm disable-user --user 0 com.amazon.device.software.ota
   adb shell pm disable-user --user 0 com.amazon.tv.forcedotaupdater.v2
   ```
2. Sideload **Launcher Manager** (from the XDA/aftvnews link) and open it.
3. In Launcher Manager: set **Corner Shop Boys** as the **custom launcher / default launcher**.
   It handles the privilege escalation that lets a non-Amazon home screen take over.
4. Reboot. The HOME button should now land on your screen.

If a future update slips through and resets it, re-run Launcher Manager (or re-apply from
step 2). If the TV ever gets stuck, a factory reset from recovery returns it to stock Amazon —
you have the other TVs to fall back on meanwhile.

> I've given you the honest current state of this rather than a guarantee: Amazon has
> patched every version of this within weeks each time, so treat Route B as something you
> re-do after updates, not a one-and-done.

---

## 5. Set up the extras

**Screensaver:** Settings > Display & Sounds > Screensaver > **Screensaver** → pick
*Corner Shop Boys*. (Its logo goes solid ecru on black by design.)

**Shop button:** edit the URL in `app/src/main/res/values/strings.xml` →
`shop_url`. It's set to `https://cornershopboys.co.uk` for now — change it when the store is live.

**Phone remote:** it's already running. Scan the QR on the home screen with your phone
(same Wi-Fi), no app needed. It moves focus, opens apps, opens the Shop and wakes the PS5.
Controlling *inside* other apps (e.g. Netflix) needs system-level key injection — doable on
the rooted device from Route B, tell me if you want that wired in.

**PS5 button:** to make it wake the console (not just switch input), you pair it once like
Remote Play:
1. On the PS5: Settings > System > Power Saving > Features Available in Rest Mode →
   turn on **Stay Connected to the Internet** and **Enable Turning On PS5 from Network**.
2. Get your PS5's local IP (PS5 > Settings > Network > Connection Status) and your
   **user-credential** (from a one-time PSN registration — the same value the open-source
   *Chiaki* app asks for; I can add an in-app pairing screen so you don't need Chiaki).
3. Until there's an in-app settings screen, these go in `Prefs` — easiest is I add a small
   settings page. Say the word.

The red PS5 dot on the home screen shows live status over Wi-Fi: green = on, amber = Rest
Mode, hollow = off/not found.

---

## What works vs what's honest-to-flag

- **Works as a normal app:** home screen, clock, logo, dock, app launching, all-apps grid,
  pin/unpin (hold OK), Shop in-app, screensaver, phone remote (for the launcher), PS5
  status + wake packet.
- **Needs the Route B takeover:** HOME button landing on your screen instead of Amazon's.
- **Needs your PS5 credential pasted in:** actual wake from Rest Mode.
- **Not promised:** forcing a specific HDMI input on every TV from a normal app, and
  universal remote control inside other apps — both want system privileges.
