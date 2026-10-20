# Privacy

Short version: everything stays on your device. The app talks only to JIIT and
Google — the same endpoints your browser would hit when you use those services
yourself — and has no servers of its own.

## What is stored on the device

- Your login tokens — Android Keystore
- Cached portal/Classroom data (attendance, grades, schedule, …) — app-private storage
- Reminder and widget settings — app-private preferences

Nothing is collected by the app: no analytics, no crash reporting, no
telemetry, no account of its own beyond your own JIIT/Google credentials.

## What leaves the device

- Requests to JIIT portal endpoints — sign-in and data fetch
- Google sign-in and a read-only Google Classroom OAuth scope

No other network traffic.

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | portal and Google requests |
| `RECEIVE_BOOT_COMPLETED` | re-arm the widget update schedule after reboot |
| `POST_NOTIFICATIONS` | reminder notifications |

## Caveats

- Credentials are as safe as your device: Keystore-protected, but keep your
  phone locked.
- The portal protocol is reverse-engineered, not officially documented or
  independently audited. Request signing mirrors the portal's own scheme
  (AES-CBC with a date-derived key, Bearer tokens). Treat the app like the
  portal itself: use your own account, read-only in spirit.
