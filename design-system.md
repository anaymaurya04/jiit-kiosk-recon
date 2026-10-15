# Design System: "Nocturnal Dossier" (JPortal Android redesign)

Source of truth: the uploaded HTML artboards (Login, Attendance=Main, Subject, Exams=Notices, Grades=Mobile, Me, Settings). If this doc and the HTMLs disagree, the HTMLs win. Use this doc to apply the same look to every other screen and component.

Target: native Android, Kotlin + Jetpack Compose, Material 3 components restyled. Phone portrait, 390dp reference width.

## 1. Personality (one paragraph)

Dark, alternative, slightly industrial. A poster-and-dossier feel: flat illustrated art, halftone texture, condensed uppercase headings, monospace metadata, hard 2dp borders, sharp corners. Fun but readable. Information first, art second. Never skulls, flames, glitch effects, or copied franchise art.

## 2. Color tokens

| Token | Hex | Use |
|---|---|---|
| bg | `#0B0B0D` | app background |
| surface | `#141418` | cards, nav bar |
| surface-2 | `#1C1C22` | secondary tiles |
| line | `#26262E` | dividers, card borders |
| line-strong | `#3A3A44` | dashed/empty borders, inactive chips |
| text | `#F0EEE9` | primary text (warm white) |
| text-muted | `#A09DA6` | metadata, captions |
| accent (crimson) | `#C23B4E` | primary accent: active nav pill, hero gradients, headings rules, toggles on |
| accent-deep | `#2A0A12` / `#5A1522` | hero gradient end, tile backgrounds |
| violet | `#7B5CD6` | secondary accent, planet art only; `#2B2060` as tile bg |
| steel | `#14202E` | trooper tile bg |
| ok | `#4CC38A` | attendance safe, present |
| warn | `#E0A030` | attendance close |
| bad | `#E0566A` | attendance low, absent, urgent text |

Rules:
- Crimson and violet are decorative/brand. ok/warn/bad are status only; never use them decoratively.
- Status is never color alone: always pair with a word tag (SAFE / CLOSE / LOW) and a line of text.
- Body text must stay at or above 4.5:1. Metadata uses text-muted on bg or surface only.
- Pure black mode (existing app feature) maps bg to `#000000`, surface to `#0B0B0D`.
- Dynamic color (Material You) is off by default in this theme. If kept, it may only tint non-brand surfaces; crimson and the art palette stay fixed.

## 3. Typography

| Role | Font | Notes |
|---|---|---|
| Display / headings | Barlow Condensed 600/700 | UPPERCASE, letter-spacing +0.03em, line-height 0.9 |
| Body | IBM Plex Sans 400/500 | 15-16sp, line-height ~1.4 |
| Metadata | IBM Plex Mono 400/500 | 11sp, text-muted; timestamps, IDs, counts, "synced" lines, tags |

Bundle the three fonts as app resources (no runtime Google Fonts download).

Scale used: screen title 56sp display; hero number 64-96sp display; card heading 22sp display; stat value 32sp display; row text 15sp; metadata 11sp mono.

## 4. Shape, borders, spacing

- Corners: sharp (0dp) for cards, tiles, tags, hero blocks, inputs. Only exceptions: nav active pill (16dp), rings.
- Borders: 2dp. Cards `line`; hero blocks and art tiles 2dp `text` (warm white); urgent/highlighted card 2dp `accent`.
- Screen padding 20dp. Card padding 12dp. Gap between cards 12dp. Row min height 48dp. All touch targets at least 48dp.
- Dividers: 1dp `line`. Section header = display 22sp in text-muted with a 1dp rule; the first/primary section may use a 1dp accent rule.
- Slight rotation (-1.5 to +1.5 deg) allowed on art tiles on the home/hub only, never on list rows or text-heavy cards.
- No drop shadows. Depth comes from layered surfaces and borders.

## 5. Halftone texture

Overlay on hero blocks and art tiles, never on text cards.

CSS reference: `background-image: radial-gradient(#00000055 1.2px, transparent 1.4px); background-size: 7px 7px;`

Compose: draw a dot grid (7dp spacing, ~1.2dp radius, black at ~33% alpha) in a `drawBehind` or as a tiled drawable, layered above the gradient and below the art/text.

## 6. Art system (original vector illustrations)

Four motifs, all flat, drawn in a 100x100 viewBox. Reuse them contextually; do not add new motifs without matching this flat, two-to-three-color style. Export as Android VectorDrawables.

| Motif | Meaning in the app | Colors |
|---|---|---|
| Great helm (knight, crimson plume) | safe/important/urgent items, auth, "guard" | text-white body, crimson plume, bg-colored visor slit |
| Planet with ring | time, exams, countdowns, schedule | violet planet, white ring, tiny white stars |
| Vinyl record | grades, CGPA, app icon, resources, "music" | near-black disc, grey grooves, crimson label |
| Trooper helmet (visor, side vents) | people: profile, faculty, avatar | white shell, violet visor, crimson side vents |

SVG source (100x100, `bg` is the surface the art sits on):

```svg
<!-- HELM -->
<path d="M50 8 Q92 -2 96 22 Q74 14 56 24Z" fill="#C23B4E"/>
<path d="M24 92 V42 Q24 12 50 12 Q76 12 76 42 V92Z" fill="#F0EEE9"/>
<rect x="47" y="12" width="6" height="80" fill="BG" opacity=".35"/>
<rect x="28" y="44" width="44" height="7" fill="BG"/>
<g fill="BG"><circle cx="62" cy="66" r="2"/><circle cx="66" cy="74" r="2"/><circle cx="34" cy="66" r="2"/><circle cx="38" cy="74" r="2"/></g>

<!-- PLANET -->
<g fill="#F0EEE9"><circle cx="12" cy="14" r="1.5"/><circle cx="84" cy="10" r="1"/><circle cx="90" cy="70" r="1.5"/></g>
<circle cx="50" cy="50" r="24" fill="#7B5CD6"/>
<path d="M30 40 Q50 30 70 42 M27 56 Q50 48 73 58" stroke="#0B0B0D" stroke-width="3" fill="none" opacity=".4"/>
<ellipse cx="50" cy="50" rx="46" ry="11" transform="rotate(-22 50 50)" fill="none" stroke="#F0EEE9" stroke-width="2.5"/>

<!-- VINYL -->
<circle cx="50" cy="50" r="42" fill="#111"/>
<g fill="none" stroke="#3A3A44"><circle cx="50" cy="50" r="35"/><circle cx="50" cy="50" r="28"/><circle cx="50" cy="50" r="21"/></g>
<circle cx="50" cy="50" r="12" fill="#C23B4E"/><circle cx="50" cy="50" r="2.5" fill="#0B0B0D"/>

<!-- TROOPER -->
<path d="M20 92 V46 Q20 10 50 10 Q80 10 80 46 V92Z" fill="#F0EEE9"/>
<path d="M27 38 H73 Q78 38 78 48 V56 Q78 64 70 64 H30 Q22 64 22 56 V48 Q22 38 27 38Z" fill="#7B5CD6"/>
<rect x="8" y="48" width="12" height="22" fill="#C23B4E"/><rect x="80" y="48" width="12" height="22" fill="#C23B4E"/>
<path d="M38 76 H62 M38 82 H62" stroke="#0B0B0D" stroke-width="3"/>
```

Usage patterns:
- **Hero block**: gradient (accent to accent-deep for crimson, `#2B2060` to bg for violet) + halftone + one large motif (150-300dp) cropped off an edge + display text on top-left.
- **Art tile** (hub/grid): colored bg + halftone + motif 120dp cropped top-right + display name and mono count bottom-left, 2dp white border.
- **Row/card ghost art**: the same motif at ~18% opacity, 90dp, cropped top-right behind a card's content. Content stays on top and must keep contrast.
- **Thumbnail**: 72dp square, colored bg + halftone + motif.
- **Empty/error state**: big motif in `line-strong` grey on a dashed 2dp `line-strong` bordered surface, display heading, one muted sentence.
- Art is decorative: `contentDescription = null`. Never put information only in art.

## 7. Components

- **Top of screen**: back arrow (48dp target) when nested; screen title display 56sp; under it a mono 11sp muted line (always the data freshness line on data screens, e.g. `SYNCED [n] MIN AGO · WORKS OFFLINE`).
- **Bottom nav**: 84dp, surface bg, 1dp `line` top border. Material 3 NavigationBar restyled: active item shows a 16dp-radius crimson pill behind the icon and white label; inactive icon/label text-muted. Four items: Attendance, Exams, Grades, Me. Nested screens (Subject, Settings) hide or keep the parent tab active.
- **Tag/sticker**: mono 11sp medium, 4dp/8dp padding, solid fill (white on dark text, or status color), no radius. Filter chips: selected = crimson fill white text, unselected = `#26262E` fill white text.
- **Card (`c`)**: surface bg, 2dp `line` border, 12dp padding.
- **List row (`r`)**: 48dp min, 1dp bottom divider, label left, value or chevron right.
- **Ring (attendance)**: 64dp, 6dp stroke, track `line`, progress in status color, center text mono 14sp percent. Starts at 12 o'clock.
- **Stat box**: card with mono 11sp label and display 32sp value (Lecture / Tutorial / Practical).
- **Calendar grid**: 7 columns, 30dp square cells, 4dp gap; ok = present, bad = absent, `line` = no class. Legend in text line below the title, not color-only.
- **Bars (SGPA)**: one bar per semester, white; latest in crimson; projected/future semesters at ~35% opacity and draggable.
- **Toggle**: 44x26dp, sharp square thumb; on = crimson track, off = `line` track.
- **Masked value**: `•••• ••••` plus "TAP TO REVEAL" in mono muted; revealing is an explicit tap.
- **Primary button**: full width, 52dp, warm-white fill, near-black text, no radius.
- **Search field (if used)**: 52dp, surface bg, 2dp white border, search icon, muted hint text.

## 8. Screen patterns (apply to any new screen)

1. **List screens**: title, freshness line, optional hero block, then cards or rows. Most urgent item first, visually stronger (hero or accent border).
2. **Detail screens**: back arrow, title, large ring/number, stat boxes, then rows. No art bigger than the data.
3. **Hub/overview**: hero block + 2x2 art tiles.
4. **Settings/About**: toggles and rows only; one small vinyl icon next to app name; unofficial disclaimer in mono muted at top ("UNOFFICIAL · NOT AFFILIATED WITH JIIT").
5. **Auth**: full-bleed crimson gradient + halftone + oversized helm, huge display name bottom-left, one primary button, one privacy line.

Every data screen must show data age and work offline-first, matching the existing app behavior.

## 9. States to design for every screen

Loading (skeleton blocks in `surface`/`line`, no spinners over art), empty (empty-state pattern above), error/failed sync (keep cached data visible with a crimson mono banner `SYNC FAILED · SHOWING DATA FROM [time]`), long text (wrap to 2-3 lines, never truncate dates/numbers), 2x font scale (cards grow, rings keep size), predictive back.

## 10. Motion and accessibility

- Short (150-250ms), functional: ring fill on load, countdown tick, pill slide on nav change. Respect "remove animations".
- Visible focus indicator on all interactive items (2dp warm-white outline).
- Rings and charts get text equivalents (`aria-label` / `contentDescription` like "86 percent attendance, safe").
- Logical heading order; one screen title per screen.

## 11. Do / Don't

Do: keep corners sharp, borders 2dp, mono for metadata, art cropped off edges, one hero per screen, status words beside colors.
Don't: add rounded cards, shadows, gradients outside heroes, new accent colors, emoji, glitch/distress fonts, skulls/flames/chains, franchise logos or character art, or put important data only inside artwork.

## 12. Notes for implementation

- Build a `NocturnalTheme` with a custom `ColorScheme` and `Typography`; expose extra tokens (ok/warn/bad, line, line-strong, surface-2) via a `CompositionLocal`.
- Reusable composables: `HeroBlock`, `ArtTile`, `GhostArt`, `StatusTag`, `AttendanceRing`, `StatBox`, `DataRow`, `FreshnessLine`, `EmptyState`, `NocturnalNavBar`.
- Convert the four SVGs to VectorDrawables once; tint-free (colors baked in), and provide a variant with the visor/bg color parameterized (the `BG` fill) for use on different surfaces.
- The HTML boards use illustrative placeholders (`[n]`, `[DATE]`); real data comes from the app's existing data layer. Do not hardcode sample values into shipped screens.
- Update the existing Roborazzi screenshot tests for light/dark/2x font after the restyle; record new baselines deliberately.

## 13. Data-viz and richer list patterns (added from Marks / Grade Cards / Exams v2)

Replace plain list rows with one data-viz element per card. Charts use only white, crimson (below-threshold or "latest"), muted grey and the track color; never rainbow.

- **Bar chart card**: surface card (optionally violet gradient + halftone), mono 11sp caption, vertical bars 36dp wide, white; value label above each bar in mono; short axis label below (SE / IP / FDA); dashed grey threshold line with label (e.g. HALF). Bars under threshold use `bad`. Missing value = bar absent and "–" label.
- **Segmented score meter**: one block per mark (20 blocks, 2dp gap, 12dp tall), filled white proportional to score (half-blocks allowed), unfilled `line`. Score text `11/20` in mono on the right. Pending/not-yet-available = dashed `line-strong` tag reading PENDING or NOT YET, never a bare dash.
- **Grade badge**: 44dp square, 2dp white border, display letter. Filled white with dark letter for B+ or better, outline for the rest (shape/fill difference, not color).
- **Credit pips**: one 8dp white square per credit, right-aligned on course rows.
- **Grade distribution**: bar chart of courses per grade for the semester.
- **Summary pair**: crimson hero block (halftone + vinyl art, big SGPA number) beside a stat card (courses, credits).
- **Week strip**: 7 equal day cells (weekday mono, date display 24sp), 2dp border; days with an exam get a white border and a dot, today is crimson-filled.
- **Timeline event row**: left date block (display day + mono month, 2dp white border), right card with ghost art, title, mono time line, tag ("TEST-2 · 20 MARKS") and room/seat in mono.
- **Ghost art on every card** (18% opacity, cropped top-right) rotating through the four motifs; content always sits above it.
- Charts get a text equivalent (contentDescription listing values) and values always shown as text, not only as bar height.
- The Next Paper hero (planet, countdown) is final; do not restyle it.

## 14. Identity and profile patterns (Me / Profile v2)

- **Student file card**: 2dp white border, steel-to-bg gradient + halftone, trooper art cropped right, `STUDENT FILE` tag, name in display 46sp, enrolment in mono, and a decorative barcode strip along the bottom (bars derived from the enrolment digits, `aria-hidden`; never scannable, never the only place the number appears).
- **Semester track**: N equal segments, completed = white, current = crimson, future = `line`; caption `SEMESTER 3 OF 4`; contentDescription states the same.
- **Hub tiles on Me**: 2-column grid of art tiles (Profile, Fees, Hostel, Bank, Feedback, Settings), same motif tiles as the home hub, mono sub-line for status. Bank tile sub-line stays masked until tapped.
- **Profile stat cards**: label in mono, value in display 40sp, ghost art at 16% behind; use for short codes (batch, section, semester, academic year). Long or free-text values stay in plain rows.
- **Contact card**: label in mono, value wrapped (word-break) in mono 12sp, a COPY tag on the right (48dp target).
- Keep every value that comes from the portal exactly as received; do not reformat codes like `2526` or `J128`.
