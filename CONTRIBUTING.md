# Contributing to Portal Counters

Thanks for your interest in improving Portal Counters! This is a hobby project for repurposing Meta Portal devices as tabletop gaming companions.

## Quick Start

1. Fork the repo and create your branch from `main`.
2. Make your changes.
3. Ensure the project builds: `./gradlew assembleDebug`
4. Open a Pull Request with a clear description.

## Development Setup

- **JDK 17** required
- **Android SDK** with platform `android-29`
- Connect a Portal Go via USB-C (or use an Android tablet emulator)

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Portal Design Guidelines

If you're adding UI, please follow the [STYLE_GUIDE.md](STYLE_GUIDE.md) which covers:

- Dark theme (forced, no pure black/white)
- 52dp minimum touch targets
- 64dp top padding for Portal system overlay
- Inter font family at 18sp body / 24sp headings / 140sp life totals
- MTG color palette tokens
- Landscape-first layout

## Code Style

- Kotlin, following official style guide (`kotlin.code.style=official`)
- Single-activity Compose architecture
- State management via Compose `mutableStateOf` and sealed class actions

## Reporting Bugs

Open a [GitHub issue](https://github.com/pgedeon/portal-counters/issues) with:
- Device model and Android version
- Steps to reproduce
- Expected vs actual behavior

## License

By contributing, you agree your contributions will be licensed under the MIT License.
