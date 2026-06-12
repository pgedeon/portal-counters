# Changelog

All notable changes to Portal Counters are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] — 2026-06-11

### Added
- **Game modes:** Standard (20/25/30 life) and Commander (30/40/50 life) with custom life option
- **2–4 player support** with adaptive layouts for each player count
- **Life counter** with ±1/±5 buttons and floating damage/heal text animations
- **Poison counters** (☠) and **Energy counters** (⚡) per player
- **Commander damage tracking** per opponent in Commander mode
- **Full undo history** — every action reversible via undo button
- **Dice roller** — D6 and D20 with result dialog
- **Game timer** — elapsed time displayed in control bar, saved to history
- **Game history & stats** — last 100 games stored locally with:
  - Per-player win/loss record, win rate, and streak tracking
  - Recent form display (W/L last 5 games)
  - Head-to-head matchup tracking
  - Average game duration, longest/shortest game
- **Player customization** — saved name roster with dropdown, MTG color identity selection (7 colors)
- **Animations** — screen shake on damage, glow pulse, scale bounce, floating number particles
- **Sound effects** — 10 synthesized sounds (5 damage + 5 heal), randomly selected per event
- **Portal Go optimization** — inverted player zones, 52dp touch targets, 64dp system overlay reservation, keep-screen-on
- **Dark theme** — atmospheric dark color palette optimized for Portal hardware
- **Inter font** — via XML downloadable fonts (Google Fonts provider)
- Landscape-locked orientation
- Winner auto-detection with victory dialog

### Technical
- Built on Meta Portal Sample App template
- Jetpack Compose with Material 3
- Single-activity architecture
- SharedPreferences for game persistence (JSON serialization)
- SoundPool for low-latency audio playback
- Gradle 9.4.1, AGP 9.2.1, Kotlin 2.2.10
- minSdk 24, targetSdk 29

[1.0.0]: https://github.com/pgedeon/portal-counters/releases/tag/v1.0.0
