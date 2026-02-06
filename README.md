# KanjiLens Android App

**Camera-based Japanese Reading Assistant**

Real-time furigana overlay for Japanese text using phone camera.

## Quick Info

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Camera**: CameraX
- **OCR**: Google ML Kit (Japanese)
- **Target SDK**: Android 8.0+ (API 26+)
- **GitHub**: https://github.com/Jay-Network/KanjiLens

## Project Documentation

Full project documentation, planning, and technical specs are maintained in the agent workspace:
- **Location**: `/home/takuma/1_jworks/A_ai/4_Apps/KanjiLens/`
- **Planning**: `planning/2026-02-05-kanjilens-ecosystem.md`
- **Technical Spec**: `docs/technical-spec.md`
- **Phase Tasks**: `docs/phase1-tasks.md`

## Development

This is the Android app source code, managed as a GitHub repository for regular backups.

**Active Development Agent**: jworks:43 (KanjiLens-Dev)

## Build

```bash
./gradlew assembleDebug
```

## Structure

```
KanjiLens/
├── app/                   # Main Android app module
├── build.gradle.kts       # Root build configuration
├── settings.gradle.kts    # Gradle settings
└── gradle.properties      # Gradle properties
```

---

**Part of**: JWorks Apps Division
**Agent Workspace**: ~/1_jworks/A_ai/4_Apps/KanjiLens/
**GitHub Backup**: This repository
