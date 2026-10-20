# JIIT Kiosk

[![release](https://github.com/anaymaurya04/jiit-kiosk-recon/actions/workflows/release.yml/badge.svg)](https://github.com/anaymaurya04/jiit-kiosk-recon/actions/workflows/release.yml)

An unofficial Android client for the JIIT student portal and Google Classroom,
built on a reverse-engineered understanding of the portal's API. Kotlin +
Jetpack Compose, single module, offline-first: everything you see is cached
locally and refreshed in the background.

> Not affiliated with or endorsed by JIIT Noida. For personal/educational use.
> The portal is not a public API — if it changes, the app breaks when it breaks.

## Features

- **Attend** — attendance per subject, today's classes
- **Exams** — exam schedule, marks, SGPA/CGPA, grade cards
- **Class** — Google Classroom, read-only: courses, notes, material
- **Code** — LeetCode and GitHub activity
- **Me** — profile, fees, hostel, bank details, feedback, reminders, calendar
- **Six home-screen widgets** — Next, Attendance, Pending (2×2), Today (4×2),
  Classes (4×2), Dashboard (4×5). Cache-only (never touch the network or block
  login), per-widget appearance settings, driven updates (they repaint at every
  class start/end and at midnight, not on a dumb timer), and a privacy mode
  that hides subject names.
- **Persistent login** — sign in once; credentials live in the Android Keystore.

## Install

Grab the APK from [Releases](https://github.com/anaymaurya04/jiit-kiosk-recon/releases).
Each release is built and signed by GitHub Actions from its `v*` tag and
installs directly over previous builds.

## Build from source

Requirements: JDK 21, Android SDK (compileSdk 37), Gradle 9.6.

```
gradle -p KioskApp assembleDebug          # build
gradle -p KioskApp testDebugUnitTest      # unit tests
```

Releasing: bump `versionCode`/`versionName` in `KioskApp/app/build.gradle.kts`,
commit, push, then push a matching `v*` tag — Actions builds and publishes the
release APK.

## Docs

- [ARCHITECTURE.md](ARCHITECTURE.md) — modules, data flow, widget pipeline, tests
- [PRIVACY.md](PRIVACY.md) — what the app stores and what leaves the device

## Status

Work in progress; see [PROGRESS.md](PROGRESS.md) for the current ledger.
