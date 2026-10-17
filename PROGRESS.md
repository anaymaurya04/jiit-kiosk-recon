# PROGRESS — updated-designs rebuild (2026-10-17, branch `feature/redesign`)

Living plan for the master brief: rebuild the UI from the updated designs, finish the new
features and their data layers, and make login persistent. The full evidence history of the
earlier rounds lives in `docs/PROGRESS.md` (Phases 0–6 redesign, Phase 2 features, Phase 3/4
widget); this file is the current plan, the checklist state, the deviations and the opens.

## Workspace facts (2026-10-17)

- App: `KioskApp/` — Kotlin + Jetpack Compose, 5-tab shell (Attend · Exams · Class · Code · Me),
  versionCode 6 / 0.2.4; releases are signed with the debug key on purpose (ledger).
- Portal client + crypto: vendored `in.codelif.ktjiit` Kotlin code under
  `app/src/main/java/`. The Rust `jiit-core/` crate exists at the repo root but is **not wired
  into the app** — nothing in `KioskApp` builds against it.
- Design source of truth: `design/Updated Design.html` (master canvas). Re-extracted 2026-10-17
  → **27 boards** in `design/boards/*.html`; each board also rendered to a plain DOM under
  `%TEMP%\opencode\dom\` for text reading. `design/` and `docs/` are local-only (gitignored).
- Docs: `docs/SPEC.md` (357 lines) and `docs/design-system.md` (the **older 14-section copy**).
  `art-library.md` and `motion-spec.md` are not on disk anywhere (checked repo, Desktop,
  Downloads) — see Open questions.

## Reality check — brief vs repo

The brief reads as if the repo were near-empty ("only a few commits"); it is not. Already built,
committed and device-verified (evidence in `docs/PROGRESS.md`) — **not rebuilt**:

- every screen of the previous design set + its states, theme, motion, 17-art-token library;
- persistent login: Keystore-sealed session, plaintext migration, two-phase process-death
  tests, refresh taxonomy test, sign-out scope; 401 → live refresh inside `KioskViewModel.call()`;
- Class tab: Classroom connect (3 read-only scopes), coursework list with filters, hero,
  add-to-calendar; Reminders engine (24/6/1h, quiet hours, alarms, boot receiver);
- Calendar (official 2026-27 asset + user events), LeetCode, GitHub, update checker, widget 0.2.4.

New in this brief, not built yet:

1. **Class 3-way switch** (Assignments / Schedule / Notes) and the **Schedule family**
   (boards 10–12: first run, builder, day view) plus **Notes** (board 13) — the four+ boards
   added in the new master file.
2. **Art library at 36 motifs**: 17 exist in `ui/theme/Art.kt`; **19 missing**.
3. §4 hardening: **soft-expired session** (failed refresh currently hard-signs-out),
   **proactive refresh** (resume + WorkManager), crypto SPEC-vector + midnight-rollover tests.
4. A match pass of every built screen against the 27 new boards (boards 04/05 and 09 may carry
   deltas beyond the switch).

## Plan (screens, files to touch)

**R1 — Persistent-login completion (§4).**
- Soft-expired state: a refresh failure keeps the local session and every cache; the shell stays
  up with the non-blocking banner `SESSION EXPIRED · TAP TO RECONNECT`; tapping opens the
  sign-in flow; Log out remains the only thing that clears.
  Files: `ui/KioskViewModel.kt` (`sessionEnded()` → soft flag; `call()`/`cached()` serve cached
  data when expired), `ui/AppShell.kt` (banner), `MainActivity.kt` (reconnect path).
- Proactive refresh: silent `auth.refresh` attempt on app resume; fold a portal-token refresh
  into the existing 15-min worker (`sync/ClassroomWorker.kt` or a sibling worker).
- Tests: JVM state-machine test for refresh → soft-expired → reconnect; midnight rollover test
  for the date key + `LocalName` against the SPEC vectors (add if absent).
- Unverifiable here: how long a refresh chain actually lasts (no real account on the emulator);
  will log honestly and list it.

**R2 — Class section (§5).**
- Board 09: `Classroom` compact title + 3-way `SegmentedSwitch` (Assignments / Schedule /
  Notes); filters, hero and cards stay; the card's third line becomes the reminder line
  (`NEXT REMINDER IN …` / `REMINDER SENT … AGO`) fed by the existing `ReminderPlanner`.
  Files: `ui/classroom/ClassroomScreen.kt`, `ui/components/Kit.kt`.
- Board 13 Notes: `courses.courseWorkMaterials.list` + `courses.topics.list` in
  `classroom/ClassroomClient.kt`, new models, grouping by course → topic, MIME type badge,
  `NEW` tag until opened, search, "new this week" count, open via ACTION_VIEW / YouTube intent.
  Files: `classroom/*`, `ui/classroom/NotesScreen.kt`.
- Boards 10–12 Schedule (user-created; the college publishes no timings): versioned JSON in the
  existing `Cache` (one active version + history, `createdAt`, survives updates, included in
  device transfer); builder (weekday, subject from attendance data, start/end, room, type,
  repeat weekly, copy-to-days); first-run single action; day view (NOW line, "next class in",
  hours today, weekly BarChart, edit); hours math per day/week/subject; attendance marker
  SAFE / CLOSE / LOW / NO DATA from that subject's live percentage via one centralized function;
  Classroom↔subject link screen (fuzzy name/code match, stored locally).
  Files: `classroom/Schedule.kt`, `ui/classroom/ScheduleScreen.kt`, `ui/components/Kit.kt`
  (`ScheduleBlock`), marker in `domain/`.
- Reminders reachable from the Assignments screen as well (Settings entry stays).
- Tests: schedule store round-trip + versioning, hours math, marker thresholds, builder logic.

**R3 — Exams / Calendar delta (§6).** Compare boards 04/05 against the built `ExamsScreen` /
`CalendarScreen`; fix any deltas found. No new sources; nothing invented.

**R4 — Art (§8.3). [done]** All 19 missing board-27 motifs ported into `ui/theme/Art.kt` (SVG paths
extracted from the rendered board; literal colours kept as parameters over theme-token defaults; new
`Cursed` token for the cursed-campus shadow). All 36 now exist. Cassette = Notes hero art (board 13),
tablet = Schedule first-run art (board 10). Family tints + rules recorded in `design-system.md` §17;
remaining swaps (pigeon/bolt/glitch/…) come with the R6 match pass.

**R5 — Kit leftovers (§8.2/8.4).** Verify every component named in §8.4 exists; add the small
missing ones (`SegmentedSwitch`, `ScheduleBlock`; check the Donut/AttendanceRing naming).

**R6 — Match pass (§12 + §10).** Every board against the emulator with screenshots; the new
screens carry all §10 states from the start (skeleton, cached-refreshing, empty, failed,
locked, missing field, not-connected, soft-expired, long names, 2× font, 360dp, dark, pure black).

**R7 — Closing (§11/§12).** Unit tests green; `PROGRESS.md` current; deviations and anything
unverified listed; final build + emulator pass.

**Risks.** No account on the emulator → live-portal paths stay device-lite (refresh chains,
real sheets). Classroom verification needs the emulator's college account (signed in during
Phase 2 — re-check). Schedules are user data: screens must be honestly empty until built.
No screenshot-test framework exists (checked) → emulator screenshots stand in.

## Open questions (need a ruling)

1. `art-library.md` and `motion-spec.md` are missing (repo + Desktop + Downloads). Using board
   27 and the brief's §9 for motion. Drop the files in any time; I will re-check against them.
2. Attendance thresholds for the Schedule markers: the app has no SAFE/CLOSE/LOW rule yet.
   The brief says centralize one function and ask — give me the numbers (proposal:
   SAFE ≥ 80%, CLOSE 70–79%, LOW < 70%; unmatched subject stays `NO DATA`).
3. Schedule storage: the brief says Room; the repo convention and the "ask before adding a
   dependency" rule point to versioned JSON through the existing `Cache` (what the calendar's
   user events already do). Confirm Cache-JSON, or approve Room.
4. `docs/design-system.md` is the older 14-section copy. I follow it where it does not
   contradict the new boards; the boards win (the doc's own rule). Replace it if a newer one exists.

## Checklist (brief §4–§12, current state)

### §4 Persistent login — [R1 built, visual pass pending — no device attached]
- [x] local session vs server session split; cold start opens the cached shell (existing)
- [x] Keystore AES-GCM sealing; backup/transfer excluded (existing, device-tested)
- [x] refresh on 401 inside `KioskViewModel.call()`; `RefreshTaxonomyTest` pins the taxonomy
- [x] tests: two-phase process-death restore, network error never clears the session
- [x] soft-expired state + `SESSION EXPIRED · TAP TO RECONNECT` banner (built; sign-in opens
      over the cached shell and Back returns to it; visual pass pending)
- [x] proactive refresh: throttled on resume (10-min floor) + folded into the 15-min worker;
      build + JVM tests green; the `kiosk-auth` log records each refresh outcome on device
- [ ] how long a refresh chain lasts — recorded honestly (blocked: no real account)
- [x] SPEC-vector crypto test + midnight-rollover test (`PortalCipherTest` 5 + `SessionStateTest`
      4, all green — SPEC §9 LocalName vector included)

### §5 Class section — [R2 family built (R2a/R2b/R2c); device visual pass pending]
- [x] Classroom connect, assignments, filters, hero, add-to-calendar, silent re-auth (existing)
- [x] Reminders engine + screen (Settings entry), boot/update receivers (existing)
- [x] 3-way switch + `Classroom` top per board 09 (R2a — freshness line hoisted above the switch;
      header fixed, each pane scrolls under it)
- [x] card third line per board 09 (R2c) — `NEXT REMINDER IN [n]H` / `REMINDER SENT [n]H AGO` / submitted
      date from one pure `ReminderPlanner.timeline`; 8dp warning-window bar (100% crimson missing,
      elapsed share of the 72h window due-soon, 0 submitted)
- [x] Notes (materials) per board 13 — per-course materials + topics calls, `Material`/`MaterialKind`,
      badges, NEW tag (`NotesPrefs` seen ids), search + kind chips, course→topic groups (R2a)
- [x] Schedule first run (board 10) + builder (board 11) (R2a) — `schedule/` package, day chips, dashed
      add box, subject picker off the attendance sheet, copy-to, one-step undo
- [x] Schedule day view per board 12 + hours chart + SAFE/CLOSE/LOW markers (R2b) — week strip,
      hour grid with offset blocks (verdict-coloured rails), crimson NOW line ticking every 30s,
      6-bar hours card, legend; markers read the attendance sheet (subjectId, else name); no
      percentage → no marker, grey rail
- [x] attendance line user-settable in Settings; one `verdictOf(percent, target, band)` drives the
      Attendance stamps (+ subject screen) and the schedule's markers — defaults 75 ± 5
- [ ] one-time Classroom↔subject link confirm (deferred; builder stores subjectId when picked)
- [x] Reminders entry from the Assignments screen (R2c) — footer action jumps to the Me tab's Reminders
      page (board 09 has no such element; brief requirement)

### §6 Calendar — [x] rebuilt to board 05 (R3): exam papers merged with the official asset + user events;
device visual pass pending
### §7 Code — [x] LeetCode + GitHub built and cross-checked; [ ] re-verify vs boards 15/16 (R6)
### §8 Build order — theme [x]; art [x] R4; components [x main set, ] R5 leftovers; motion [x];
screens [x previous set, ] R6 pass; Class [R2a–R2c done: switch, Schedule (first run / builder /
day view), Notes, card reminder line + bar, Reminders entry]; Exams/Calendar [R3: board 04 no delta,
board 05 rebuilt on exam data]; Code [x]; screenshot tests
[none exist — ledger]
### §9 Motion — [x] `Motion.kt` + wiring (the bank digit-roll was never shipped — ledger)
### §10 States — [x] for the existing screens (Phase 6 pass); [ ] for all new screens (R6)
### §11 Method — branch `feature/redesign` [x]; commits per step; this file kept current
### §12 Done means — [ ] after R1–R7

## Ledger (this round; older entries live in `docs/PROGRESS.md`)

- 2026-10-17: boards re-extracted from the new master file (27, up from 25) — `design/boards/`
  is the canonical pixel reference now; boards win over `design-system.md` (its own rule).
- 2026-10-17: `design-system.md` on disk is the older copy (14 sections, 4-tab examples); the
  brief's "sections 1–17" version is not on disk. Following the copy + the boards.
- 2026-10-17: `art-library.md` / `motion-spec.md` missing everywhere → board 27 + brief §9.
- 2026-10-17: `jiit-core/` is not part of the app's build; the "don't break crypto/auth" rule is
  being honored against the vendored ktjiit code that actually runs on device.
- 2026-10-17: Google sign-in runs the portal's own GSI button inside the WebView
  (`signin/PortalWeb.kt` over the portal's pages) with the portal's client id — Android
  Credential Manager cannot mint a Google ID token for a client id the app does not own, so the
  current flow is the correct one and stays. (Brief: "find out and tell me".)
- 2026-10-17 (R2a): schedule stored as JSON in `files/schedule.json` — deliberately NOT the portal
  cache, because sign-out (even the clearing kind) wipes the cache and the schedule is the phone's own
  data. One active version; every save rotates the old one into `schedule.prev.json`; `UNDO LAST SAVE`
  swaps them (so the undo itself is undoable). `ScheduleStore` unit-tested via round-trip.
- 2026-10-17 (R2a): Notes row shows the date alone where board 13 shows `[SIZE] · [DATE]` — the
  Classroom REST API exposes no file size. Badge from the file's own extension (pdf / ppt / doc
  families); anything else is badged URL because the tap opens it as a link anyway. A material post
  with no attachment gets no row (the board has no text-only shape).
- 2026-10-17 (R2a): board 11's "Repeat every week" toggle is omitted — every entry is day-of-week and
  therefore weekly by construction; an OFF state would have to mean dated one-offs, which the model
  cannot honestly express yet. Entry rows use mono EDIT / REMOVE instead of the board's pencil/× icons
  (no icon set; text reads clearer). Both are for the R6 match pass once the art library (R4) is in.
- 2026-10-17 (R2a): builder subject picker reads the attendance sheet through the same `rememberSheet`
  cache path (attempt 0, never blocks the builder, works offline from cache); picking stores the
  subjectId so the R2b markers can find that subject's percentage. Typed-in subjects carry no id and
  no marker — honest by design.
- 2026-10-17 (R2c): board 09's card completes — the third line is reminder-centric and the 8px bar is the
  warning-window fill (missing 100% crimson; due-soon the elapsed share of the 72h window, which is what
  the board's own 70% sample shows; submitted 0). `REMINDER SENT [n]H AGO` prints the scheduled instant,
  not a delivery receipt — the app keeps no fired-ping log (shortcut note in `ReminderPlanner.timeline`).
  A ping the quiet window would move is left off the card entirely: the plan is re-laid every 15 minutes
  and a moved ping only survives when nothing re-plans before it, so promising it would be a lie. The
  card falls back to the old status line (`MISSING · NOT SUBMITTED` etc.) whenever reminders are off, the
  course is muted, or no ping qualifies.
- 2026-10-17 (R2c): the Assignments footer sentence ("…set up in Settings") is replaced by a real
  `REMINDERS` action — board 09 has no such element; this is the brief's "entry from Assignments". It
  crosses tabs via a one-shot flag in `AppShell` (no nav-framework change): the Me tab opens its
  Reminders page; back then walks Settings → Overview as usual.
- 2026-10-17 (R3): board 04 vs `ExamsScreen` diffed detail by detail — no delta beyond R6 nits (the screen
  already carries the NEXT PAPER hero, week strip, Then cards, room/seat line and the caption). Board 05
  vs `CalendarScreen` was a real rebuild, done: exam papers now feed the calendar (grid outlines + dots,
  rows with `Subject · Test` and a mono time, the day card, ADD TO CALENDAR as a timed event) through the
  *same cache keys the Exams tab uses*, so one fetch feeds both; the registration asked about is the one
  today falls in (nearest when between, first when the portal dates are missing). Rows became board-shaped
  (52dp bone day box, 26sp day, 1px bottom rule, no kind tag); the grid went 46dp / mono numbers / weekend
  days muted / single 6dp dot; today stays crimson-filled with a bone border; the hero is 110dp with the
  hourglass running off the bottom-right corner; the week track marks the present week with a crimson
  outline on a dark tile; the axis is one mono line `START ————— TODAY ————— END`. Kept deliberately:
  official-asset rows (the board samples none but dropping them would drop the official calendar), the two-tap
  REMOVE on the student's own rows, and ADD TO CALENDAR on the day card. New `CalendarRowsTest` pins the
  row mapping and the registration pick. 73 tests green.
- 2026-10-17 (R4): board 27's 19 missing motifs ported into `Art.kt` with a one-off DOM→Kotlin converter
  (literal SVG fills kept as parameters defaulting to theme tokens; `Cursed #241338` added to `Theme.kt`).
  Board rules — pick the motif by meaning, tint by family (crimson knights / violet space / graphite band
  / steel archive), never the same motif twice on one screen — are in `design-system.md` §17. Cassette
  took the Notes hero (board 13 draws it 150px off the corner) and tablet the Schedule first-run empty
  state (board 10). `AttendanceScreen.Ghost`'s four-motif cycle over the subject list is a standing
  exception until boards 02/03 are re-checked at R6.
- Carried from earlier rounds (still true): debug-key release signing; no screenshot-test infra;
  notification timing under Doze not assertable headlessly; GitHub token path not device-run
  with a live PAT.

## Status log

- 2026-10-17: recon complete. 27 boards re-extracted + rendered to DOMs; code, docs and old
  progress read; gaps identified (this file); branch `feature/redesign` created; baseline
  `assembleDebug + testDebugUnitTest` green before any change. Next: R1.
- 2026-10-17 (R1): soft-expired session implemented — a failed refresh keeps the local session
  and every cache; the shell shows the reconnect banner and the sign-in screen opens over it
  (Back returns). Proactive refresh: resume (10-min floor) + inside the existing 15-min worker.
  New tests green: `PortalCipherTest` (SPEC §9 vector, key derivation, 32-byte nonce, IST
  midnight rollover) and `SessionStateTest`. No device was attached, so the banner's visual
  pass is pending. Next: R2 (Class switch / Notes / Schedule).
- 2026-10-17 (R2a): Class tab rebuilt around the board-09 switch (fixed header; panes below).
  Assignments = the old body verbatim behind it. Notes pane live: per-course materials + topics
  fetched by the same client/worker (one cache, one freshness line), `Material`/`MaterialKind`,
  `NotesPrefs` seen ids, crimson NEW-this-week hero, search, kind chips, course→topic groups, legend.
  Schedule pane live: first run (board 10) + builder (board 11) — day chips, dashed add box, subject
  picker off the attendance sheet, LECTURE/TUTORIAL/LAB chips, copy-to, one-step undo, save to
  `files/schedule.json`. Attendance line user-settable in Settings; `verdictOf(percent, target, band)`
  (defaults 75 ± 5 = the mockup samples) now drives the Attendance tab and subject screen. New
  components: `SegmentedSwitch`, public `dashedBorder`; `design-system.md` §15 documents the family.
  Tests: 66 green (ScheduleTest 5 + VerdictTest 3 new). No device attached — visual pass still pending.
  Next: R2b day view (board 12) + hours + markers.
- 2026-10-17 (R2b): schedule day view built (board 12). Landing for a saved schedule = the day view;
  EDIT SCHEDULE opens the builder. Week strip, hour grid (blocks offset by start, 6dp verdict rail,
  grey rail when the subject has no percentage), crimson NOW line + `Now h:mm a` stamp ticking every
  30s, NEXT CLASS IN / TODAY pair, `HOURS THIS WEEK` card with the 6-bar chart, mono legend. Markers:
  `markFor()` matches subjectId first, name second, then `verdictOf(percent, target, band)` — the same
  line as Settings and the Attendance tab. Grid fits the day's span (floor/ceil to the hour, 4h min;
  9AM–5PM when empty). Build + 66 tests green. Still no device — visual pass pending. Next: R2c
  (card reminder lines, Reminders entry from Assignments), then R3.
- 2026-10-17 (R2c): board 09 cards finished — third line from the new pure `ReminderPlanner.timeline`
  (`NEXT REMINDER IN [n]H`, `REMINDER SENT [n]H AGO`, else the old status text; submitted keeps its
  date) and the 8dp warning-window bar. `REMINDERS` footer action on Assignments jumps to the Me tab's
  Reminders page (one-shot `AppShell` flag + `MeScreen` params). `asReminderWork()` lifted to file level
  so the alarms and the card share one mapping. Build + 69 tests green (3 new timeline tests). No
  device — visual pass pending. Next: R3 (boards 04/05 exam + calendar delta).
- 2026-10-17 (R3): board 04 needed nothing (screen already matches; nits for R6). Board 05: `CalendarScreen`
  rebuilt — exam papers merged in from the same cache the Exams tab fills, board-row/grid/hero/track/axis
  styling applied, weekend days muted, day card and ADD TO CALENDAR cover exams. `CalendarRowsTest` (4)
  pins the mapping + registration pick. Build + 73 tests green. Next: R4 (19 missing motifs from board 27).
- 2026-10-17 (R4): art library complete — the 19 missing motifs (pennant, sentinel, rocket, moon, guitar,
  cassette, headphones, amp, key, bolt, pigeon, backup, basilica, glitch, coffee, bonearchive,
  necromancer, tablet, bonsai) added to `Art.kt` from board 27's own SVG paths; `Cursed` token added to
  `Theme.kt`. Cassette = Notes hero art, tablet = Schedule first-run art (was dossier). Build + 73 tests
  green; only the 7 pre-existing warnings. Still no device — visual pass pending. Next: R5 (kit leftovers,
  §8.4 inventory).
