# KanjiLens Development Tracker

> **Updated by**: jworks:43 (KanjiLens agent)
> **Last updated**: 2026-02-25

---

## Current Status

- **Version**: 1.6.0 (versionCode 15)
- **Platform**: Android (Kotlin + Jetpack Compose)
- **Build**: Passing
- **Branch**: master
- **Stage**: Store-ready, pre-launch

---

## Feature Matrix

| Feature | Status |
|---------|:------:|
| **Phase 1: MVP Core** | |
| CameraX real-time preview | DONE |
| ML Kit Japanese OCR (offline) | DONE |
| Text overlay with bounding boxes | DONE |
| Settings UI (text size, colors, overlay) | DONE |
| Unit tests (22) | DONE |
| **Phase 2: Furigana** | |
| JMDict database (215K entries) | DONE |
| Kuroshiro backend (morphological analysis) | DONE |
| Context-aware furigana lookup | DONE |
| Per-kanji-segment rendering | DONE |
| Jitter stabilization | DONE |
| Partial mode (dual-filter OCR) | DONE |
| Vertical text support | DONE |
| **Phase 3: Monetization & Auth** | |
| Google Sign-In + Supabase auth | DONE |
| Google Play Billing ($1.99/mo, $14.99/yr) | DONE |
| Free tier (5 scans/day, 60s max) | DONE |
| Paywall screen | DONE |
| Admin override | DONE |
| **Phase 3b: UI Enhancements** | |
| Feedback system (Supabase + FCM) | DONE |
| Profile screen | DONE |
| App icon (adaptive) | DONE |
| Draggable button cluster (8 buttons) | DONE |
| Onboarding tutorial + Help/About | DONE |
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
| Wired to UI triggers | IN PROGRESS |
| **Phase 4: Advanced** | |
| Traditional kanji variant dictionary | - |
| Cloud Vision API fallback | - |
| Screenshot/save mode | - |
| History tracking | - |
| Flashcard integration (KanjiQuest) | - |
| **Phase 5: Pre-Launch** | |
| Supabase credentials configured | - |
| Google OAuth client ID | - |
| Play Console subscription products | - |
| QA testing checklist | - |
| Beta testing program | - |
| Signed AAB upload | - |

**Legend**: DONE | IN PROGRESS | - (not started) | N/A

---

## Current Sprint

- **Current work**: Spatial filtering refinements for kanji/jukugo disambiguation
- **Next**: Traditional kanji variant dictionary (high priority)
- **Blockers**:
  - Traditional kanji recognition (ML Kit fails on kyujitai: 萬, 國, 學)
  - Supabase credentials not yet configured
  - Play Console account needed

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2026.01.01) |
| Camera | CameraX 1.5.3 |
| OCR | ML Kit Japanese 16.0.1 |
| Morphology | Kuromoji 0.9.0 |
| Database | Room 2.6.1 + SQLite |
| Backend | Retrofit 2.11.0 |
| Auth | Supabase 2.1.5 + Google Sign-In 21.3.0 |
| Billing | Google Play Billing 7.1.1 |
| DI | Hilt 2.54 |

---

## Performance

| Metric | Current | Target |
|--------|---------|--------|
| OCR Processing | ~180ms | <150ms |
| Frame Rate | 30 FPS | 30 FPS |
| Battery Drain | ~20%/hr | <15%/hr |
| App Size | ~25MB | <30MB |
