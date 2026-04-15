# KanjiSage Development Tracker

> **Updated by**: jworks:43 (KanjiSage agent)
> **Last updated**: 2026-04-15

---

## Current Status

- **Version**: 1.7.1 (versionCode 19)
- **Platform**: Android (Kotlin + Jetpack Compose)
- **Build**: Passing (45 unit tests, 0 failures)
- **Branch**: master
- **Stage**: Pre-launch polish — all code work complete, awaiting Play Console setup

---

## Feature Matrix

| Feature | Status |
|---------|:------:|
| **Phase 1: MVP Core** | |
| CameraX real-time preview | DONE |
| ML Kit Japanese OCR (offline) | DONE |
| Text overlay with bounding boxes | DONE |
| Settings UI (text size, colors, overlay) | DONE |
| Unit tests (45) | DONE |
| **Phase 2: Furigana** | |
| JMDict database (215K entries) | DONE |
| Kuromoji morphological analysis (offline) | DONE |
| Context-aware furigana lookup | DONE |
| Per-kanji-segment rendering | DONE |
| Jitter stabilization | DONE |
| Partial mode (dual-filter OCR) | DONE |
| Vertical text support (縦書き) | DONE |
| **Phase 3: Monetization & Auth** | |
| Anonymous-first auth (no auth wall) | DONE |
| Google Sign-In + Supabase auth | DONE |
| Handle system (display name) | DONE |
| Google Play Billing ($0.99/mo, $4.99/yr) | DONE |
| Free tier (5 scans/day, 60s max) | DONE |
| Paywall screen | DONE |
| Admin override | DONE |
| **Phase 3b: UI Enhancements** | |
| Feedback system (Supabase + FCM) | DONE |
| Profile screen | DONE |
| App icon (adaptive) | DONE |
| Draggable button cluster | DONE |
| Onboarding tutorial + Help/About | DONE |
| Animated splash screen (~3.6s) | DONE |
| **Phase 3c: Native Dictionary** | |
| Enriched JMDict (221K entries) | DONE |
| Room schema v2 | DONE |
| Native Compose dictionary view | DONE |
| Offline LRU cache | DONE |
| POS tags, kanji breakdown, Jisho fallback | DONE |
| **Phase 3d: J Coin System** | |
| Coin earning rules | DONE |
| Rewards screen | DONE |
| Bookmarks (Recent/Saved tabs) | DONE |
| Wired to UI triggers | DONE |
| **L1 Accessibility Compliance** | |
| Focus indicators on all 44 interactive elements | DONE |
| Font sizes ≥12sp | DONE |
| Contrast ratios (onSurfaceVariant fix) | DONE |
| All hex colors migrated to KanjiSageColors tokens | DONE |
| **Cross-App Integration** | |
| KanjiJourney deep link (DictionaryDetailView) | DONE |
| KanjiJourney deep link (CrossPromoBanner) | DONE |
| Play Store fallback if KanjiJourney not installed | DONE |
| **Security** | |
| Secret files chmod 600 | DONE |
| StaticFieldLeak fix (applicationContext) | DONE |
| Lint crash workaround (3 detectors disabled) | DONE |
| **Pre-Launch** | |
| Play Developer Account (jay@jworks-ai.com) | DONE |
| Privacy policy URL | DONE |
| Play Store listing text | DONE |
| Data safety form answers | DONE |
| CameraX ≥1.4 (16KB alignment) | DONE |
| Play Console subscription products | - |
| Signed AAB upload | - |
| Internal testing track | - |
| Staged rollout → LIVE | - |
| Secrets moved out of repo | - |

**Legend**: DONE | IN PROGRESS | - (not started)

---

## Current Sprint

- **Current work**: All autonomous code work complete. Awaiting Play Console access for launch tasks.
- **Next**: Create subscription products in Play Console → build signed AAB → internal testing
- **Blockers**:
  - Play Console subscription product creation (needs Jay)
  - Secrets migration out of repo before store launch (needs Jay's approval)

---

## Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Kotlin | 2.0.21 |
| UI | Jetpack Compose (BOM) | 2026.01.01 |
| Camera | CameraX | 1.5.3 |
| OCR | ML Kit Japanese | 16.0.1 |
| Morphology | Kuromoji | 0.9.0 |
| Database | Room + SQLite | 2.6.1 |
| Backend | Retrofit | 2.11.0 |
| Auth | Supabase + Google Sign-In | 2.1.5 / 21.3.0 |
| Billing | Google Play Billing | 7.1.1 |
| DI | Hilt | 2.54 |
| Build | AGP / Gradle | 8.7.3 / 8.9 |
| compileSdk / targetSdk | | 35 |
| minSdk | | 26 |

---

## Performance

| Metric | Current | Target |
|--------|---------|--------|
| OCR Processing | ~180ms | <150ms |
| Frame Rate | 30 FPS | 30 FPS |
| Battery Drain | ~20%/hr | <15%/hr |
| App Size | ~25MB | <30MB |
