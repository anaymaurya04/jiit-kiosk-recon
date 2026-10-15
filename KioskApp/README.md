# KIOSK — JIIT portal app

An unofficial Android client for `webportal.jiit.ac.in`. Attendance with the
skip/attend maths, marks, exam seats, grade cards, profile and fees, in a native
Compose UI.

## Why the first attempt failed

The portal **moved students to Google sign-in in September 2026**. The first
version of this app implemented the old enrolment + password + captcha flow
reverse-engineered from the portal's JavaScript bundle. That flow is still in
the bundle but it is no longer how students get in, so nothing past the captcha
screen could work.

The fix is not a patch: it is a different way in. Sign-in now happens through
Google inside a WebView, and the resulting Google ID token is traded for a portal
session at `https://webportal.jiit.ac.in:6013/CLXSSOAPI/token/generate-token-google-signin`.
No password is ever typed into this app.

## What it is built on

`in/codelif/ktjiit` is vendored in as a library
([ktjiit](https://github.com/codelif/ktjiit), GPL-3.0). It is a pure-JVM client
that already knows the portal's protocol:

- the daily rotating AES key and the `LocalName` header, both derived from a
  date fingerprint in IST
- which endpoint wants a plain JSON body and which wants an encrypted one
- the portal's clock skew (its clock runs minutes slow, and both the key and the
  token expiry follow it), with a retry when a fresh process disagrees
- typed calls for every screen this app shows, plus the marks-PDF parser
  (the JSON marks endpoint returns one record per subject, so the PDF is the
  source of truth)

This is deliberately not reimplemented. It is the part that has to be exactly
right, and it is already exercised against the real portal.

The Google sign-in WebView (`signin/PortalWeb.kt`, `assets/signin.html`) follows
the approach proven in [JPortal for Android](https://github.com/codelif/jportal-android):
Google refuses to sign in when it detects a WebView, and the portal's client id
only trusts the portal's own origin, so the app looks like Chrome and serves its
own small page from that origin.

## Sign-in, in order

1. `GET /token/getlogoClientWise` gives the Google client id and whether Google
   sign-in is offered. It returns early, so this call is what makes the portal
   section usable at all.
2. A WebView loads `…/studentportal/kiosk-signin`, intercepted and served from
   `assets/signin.html`, which runs Google Identity Services.
3. Google hands back an ID token.
4. `POST …:6013/CLXSSOAPI/token/generate-token-google-signin` with
   `{googleToken, modulename}` returns the portal session.
5. The session is kept in app-private SharedPreferences and reused until the
   portal rejects it.

If Google blocks the button, the sign-in screen offers the real portal page in a
WebView as a fallback, and reads the login response out of it.

Sessions are extended on a 401 via `/token/refreshTokenRequest` before giving up.

## Building

Needs JDK 21 and the Android SDK, `compileSdk 37`.

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\build.ps1          # -> app\build\outputs\apk\debug\app-debug.apk
.\test.ps1
```

`build.ps1` re-runs Gradle because Maven Central's DNS is intermittent on this
machine; each pass caches more. `dl.google.com` is reliable.

### Toolchain notes

- **AGP 9 bundles Kotlin 2.3.x.** Do not also apply `org.jetbrains.kotlin.android`
  — it fails with `Cannot add extension with name 'kotlin'`. Only the compose and
  serialization compiler plugins are applied, both at `2.3.21`.
- `material-icons-core` is a curated subset; check an icon exists before using it.
- The APK is debug-signed, which is fine for sideloading.

## Privacy

Your password is never entered into this app. The only thing stored is the portal
token the portal itself issued, in app-private storage, and it is only ever sent
to the portal. Nothing is uploaded, and there is no analytics.

## Licence

GPL-3.0-only, because ktjiit is GPL-3.0. See the vendored `LICENSE`.
