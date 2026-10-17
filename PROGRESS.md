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

**R4 — Art (§8.3).** Add the 19 missing motifs from board 27 as Compose `Canvas`/`ImageVector`
(paths extracted from the rendered board); use them by meaning in the new screens; audit that no
motif repeats twice on one screen.

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

### §5 Class section — [R2]
- [x] Classroom connect, assignments, filters, hero, add-to-calendar, silent re-auth (existing)
- [x] Reminders engine + screen (Settings entry), boot/update receivers (existing)
- [ ] 3-way switch + `Classroom` top per board 09
- [ ] card reminder line per board 09
- [ ] Notes (materials) per board 13 — new client calls, models, badges, NEW tag, search
- [ ] Schedule: first run / builder / day view per boards 10–12, hours tracking, markers, linking
- [ ] Reminders entry from the Assignments screen

### §6 Calendar — [x] built from the official PDF asset + user events; [ ] delta pass (R3)
### §7 Code — [x] LeetCode + GitHub built and cross-checked; [ ] re-verify vs boards 15/16 (R6)
### §8 Build order — theme [x]; art [ ] R4; components [x main set, ] R5 leftovers; motion [x];
screens [x previous set, ] R6 pass; Class [ ] R2; Code [x]; screenshot tests [none exist — ledger]
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
