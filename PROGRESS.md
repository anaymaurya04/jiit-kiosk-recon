# PROGRESS — JPortal ("Nocturnal Dossier") rebuild

Repo: `jiit-kiosk-recon`. App: `KioskApp/` (Kotlin + Jetpack Compose, 4-tab shell).
Reference: `design-system.md` (style bible) + `New Design/*.zip` artboards (pixel reference). If they disagree, artboards win.

## Plan (screens to build, files to touch, risks)

**Screens** — v2 artboards supersede v1 where both exist:
Sign in (`Login`), Attendance (`Main`), Subject detail (`Subject`), Exams (`ExamsV2` — supersedes `Notices`), Grades overview (`Mobile`), Marks (`MarksV2`), Grade cards (`GradeCards` v2), Me (`MeV2` — supersedes `Me`), Profile (`ProfileV2`), Settings (`Settings.dc.html`, received), Fees / Hostel / Bank / Feedback (**no artboards** — build from §8 patterns).

**Files to touch**
- New: `ui/Motion.kt` (durations/easings/springs, `rememberReducedMotion`, entrance + stagger, nav-pill slide, skeleton shimmer, count-up).
- Shared: `ui/theme/Theme.kt`, `theme/Art.kt`, `theme/Icons.kt`, `ui/components/Kit.kt` (add `WeekStrip`, `TimelineEvent`, `ScoreMeter`, `BarChart`, `GradeBadge`, `CreditPips`, `StudentFileCard`, `SemesterTrack`, `SkeletonBlock`, collapsing title, predictive back).
- Shell: `ui/AppShell.kt` (nav pill slide, tab crossfade).
- Screens: `signin/SignInScreen.kt`, `ui/attendance/*`, `ui/exams/ExamsScreen.kt`, `ui/grades/GradesScreen.kt`, `ui/me/MeScreen.kt`.
- Docs: `PROGRESS.md` (this file), `design-system.md`.

**Risks / deviations**
- `Settings.dc.html` is now in the bundles (`Settings & About-html.zip`, received 2026-10-16); read and used.
- Two Settings rows have no home in this app yet and need a ruling while building that screen: "Dynamic color" (theme wiring) and "Check GitHub for updates" (there is no release feed). Nearest honest alternative + ledger entry at build time; no invented network behaviour.
- No Roborazzi/screenshot-test infra in the app (checked: no test deps beyond JUnit). Regression coverage = emulator screenshots + unit tests.
- Real Google sign-in cannot be exercised here (no JIIT credentials); everything past the sign-in screen is verified with seeded cache + airplane mode.
- Camera/animation claims are only made after running on `emulator-5554`.
- `jiit-core` is untouchable (UI layer only). No dependency additions planned.

## Checklist

### Phase 0 — recon + docs
- [x] Read `SPEC.md`, `design-system.md`, artboards, app stack, `jiit-core`
- [x] Extract all 11 artboard bundles; identify v2 files (Exams, Marks, Me, Profile, Grade Cards)
- [x] `PROGRESS.md` written with plan

### Phase 1 — theme
- [x] Custom `ColorScheme`, typography, 0dp shapes, extra tokens via CompositionLocal
- [x] Pure-black mode (`bg → #000000`, `surface → #0B0B0D`)
- [ ] Re-verify tokens against `design-system.md` §2/§3 (accent-deep `#2A0A12`/`#5A1522`)

### Phase 2 — art
- [x] Four motifs (`HelmMark`, `PlanetMark`, `VinylMark`, `TrooperMark`) parameterized for surface fill
- [ ] Animate parts: planet ring tilt/float, vinyl rotation (used by motion hooks)

### Phase 3 — components
- [x] `BodyText`, `MonoText`, `D`, `Mono`, `Stamp`, `Card2` (ghost `art` slot), `RowLine`, `NavRow`, `DetailRow`, `KeyRow`, `BackRow`, `ChipRow`, `PrimaryButton`
- [x] `ScreenTitle`, `CollapsingTitle`, `SectionHead`, `FreshnessLine`, `StateBox`, `FullState`, `EmptyState`
- [x] `SkeletonBlock`, `SkeletonScreen`, `ScoreMeter`, `BarChart`, `GradeBadge`, `CreditPips`, `WeekStrip`, `TimelineEvent`, `SemesterTrack`, `StudentFileCard`, `ArtTile`
- [x] Tap feedback on every interactive kit element: 0.97 press scale + sharp crimson wash, no rounded ripples

### Phase 4 — motion (`ui/Motion.kt`)
- [x] Tokens (durations, easings, springs, stagger), `rememberReducedMotion` (animator scale 0 → plain fades, no loops)
- [x] `PageFlip` (forward/back transitions) + `BackLayer` (predictive back, follows the gesture)
- [x] Bottom-nav pill slide + stretch, tab crossfade
- [x] `CollapsingTitle` + `parallax` modifier
- [x] `entrance` stagger (once per navigation entry, saveable so rotation does not replay), `shimmer`, `RollingText`, `rememberCountUp`, `rememberSpin`, `rememberOrbit`
- [x] `BarChart` growth + value labels + threshold line + under-threshold pulse; `ScoreMeter` fill; `SemesterTrack` landing; `Ring` sweep + synced count
- [ ] Screen wiring: entrances per screen, hero art slide, tag pop, countdown roll, planet float, vinyl idle/sync, pull-to-refresh, bank digit roll, COPY morph, toggle motion — lands with each screen (Phase 5)

### Phase 5 — screens (build, run, screenshot, compare, commit each)
- [ ] Sign in
- [ ] Attendance (home)
- [ ] Subject detail
- [ ] Exams (v2)
- [ ] Grades overview
- [ ] Marks (v2)
- [ ] Grade cards (v2)
- [ ] Me (v2)
- [ ] Profile (v2)
- [ ] Settings (no artboard)
- [ ] Fees / Hostel / Bank / Feedback (no artboards)

### Phase 6 — tests
- [ ] Unit tests green (`AttendanceMathTest`, `CacheTest`)
- [ ] Screenshot pass on emulator: light/dark, pure black, 2x font, 360dp width
- [ ] State coverage: loading / cached-refreshing / empty / failed / long names / missing fields

### Ledger (deviations + rulings)
- 2026-10-16: `design-system.md` did not exist on disk; written verbatim from the doc supplied in conversation.
- 2026-10-16: No screenshot-test framework present → emulator screenshots stand in.
- 2026-10-16: Ring number is drawn on the canvas with the sweep (one redraw, no per-frame recomposition) rather than a `Text` that recomposes per frame.
- 2026-10-16: Generic skeleton (hero slab + three cards) stands in for a per-screen skeleton until each screen lands; the mockups have no designed loading state.

### Status log
- 2026-10-16: recon done (11 bundles, 5 v2), `design-system.md` + `PROGRESS.md` written. Nothing else started this pass.
- 2026-10-16: Settings artboard received and read (3 toggles, vinyl/version block, three link rows).
- 2026-10-16: `ui/Motion.kt` written; kit extended (skeletons, score meter, bar chart, grade badge, credit pips, week strip, timeline event, semester track, student file card, collapsing title); nav pill slides and stretches; tabs crossfade; all tap targets use the press-scale + crimson wash. `assembleDebug` + unit tests green. Not yet run on the emulator.
