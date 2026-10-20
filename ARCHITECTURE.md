# Architecture

Single Kotlin module (`KioskApp/app`), Jetpack Compose (Material 3) UI,
vendored portal client, RemoteViews widgets. No DI framework, no multi-module
split — the app is small enough that explicit structure does the job.

## Stack

- Kotlin, Jetpack Compose (BOM 2026.09.00), Material 3
- kotlinx-serialization + coroutines only (vendored `ktjiit` portal client)
- WorkManager for background sync and reminder sync
- RemoteViews for widgets (not Glance) — six providers on a shared abstract base
- Android Keystore for credential storage
- `testOptions.unitTests.isReturnDefaultValues = true` so `android.util.Log`
  no-ops on the JVM test classpath

## Layers

| Path | Role |
|---|---|
| `ui/` | per-tab screens (Attend, Exams, Class, Code, Me) + shared kit components |
| `session/` | login (WebView Google sign-in → SSO token exchange), token storage, portal crypto |
| `sync/` | `ClassroomWorker` and friends: fetch → cache → repaint widgets |
| caches | app-private snapshot files; cache-first UI reads, network refreshes |
| `widget/` | six providers, `WidgetRender` painters, config screen, update scheduler |

## Data flow

Portal requests are signed the same way the official web portal signs them
(AES-CBC with a date-derived key, per-request identity, Bearer JWT). Responses
land in local caches. The UI and widgets always render from cache; refresh
states are shown honestly — widgets never touch the network, and the app
launches fine with no network at all.

1. `ClassroomWorker` fetches new data → writes caches → sets the shared
   `widget_updating` flag → paints all six widgets.
2. `WidgetPlanner` schedules a repaint at every remaining class start/end today
   and the next seven midnights; `WidgetTickReceiver` repaints and re-lays at
   every tick.
3. The boot receiver re-arms the schedule after reboot; opening the app
   re-syncs the widgets no matter which tab it resumes on.

## Widgets

| Widget | Size | Shows |
|---|---|---|
| Next | 2×2 | next class |
| Attendance | 2×2 | attendance headline |
| Pending | 2×2 | pending assignments |
| Today | 4×2 | today's schedule |
| Classes | 4×2 | classes grid (row-capped when wide) |
| Dashboard | 4×5 | mini dashboard |

`WidgetConfigActivity` (reachable from any widget) edits rows shown and
appearance, per widget kind. Every widget handles every state: expired strip
("portal not fresh"), no schedule, all clear, and the updating skeleton.

## Tests

`gradle -p KioskApp testDebugUnitTest` — JVM unit tests only (18 suites /
107 tests), covering the widget painters/planners, the auth state machine,
formatting, and cache behaviour. Keystore-bound flows (persistent login) run
as instrumented tests on a device.
