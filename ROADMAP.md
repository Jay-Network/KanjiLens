# KanjiSage Roadmap

Current: **v1.7.1** (Beta) | Stage: v1.x.x = Beta, v2.x.x = Store Release

---

## Completed Versions

### v1.6.2 — L1 Accessibility Compliance (2026-04-10) ✅
- Font sizes raised to ≥12sp across all screens
- onSurfaceVariant contrast fixed (solid color, no alpha)
- Hardcoded hex colors migrated to KanjiSageColors tokens

### v1.6.3 — Finish Color Token Migration (2026-04-13) ✅
- All remaining inline Color(0x...) migrated to KanjiSageColors tokens (10 files)
- 14 new KanjiSageColors tokens added
- Secret files hardened (chmod 600)

### v1.7.0 — KanjiJourney Deep Link Integration (2026-04-14) ✅
- "Send to KanjiJourney" deep link from DictionaryDetailView + CrossPromoBanner
- Falls back to Play Store if KanjiJourney not installed

### v1.7.1 — StaticFieldLeak + Lint Crash Fix (2026-04-15) ✅
- Fixed StaticFieldLeak in CameraViewModel (applicationContext)
- Disabled 3 crashing AndroidX lint detectors (Kotlin 2.0 UAST incompatibility)

---

## Upcoming

### v1.8.0 — AI Dialog Vision
- **Scan → AI understands → conversation**: tap detected kanji to start AI dialog
- Multi-turn conversation about scanned text (meaning, usage, context)
- Contextual grammar explanations for detected sentences
- Example sentences generated from scanned vocabulary
- History of scanned words with AI-generated study notes
- **Dependency**: Backend AI endpoint (Claude API or on-device LLM)

### v1.9.0 — Glass UI Theme
- Glass morphism UI across all screens (frosted panels, transparency, blur)
- Shared glass theme system with KanjiJourney / EigoSage / EigoJourney
- Dark mode support with glass overlays
- Redesigned Settings and Profile screens with glass cards

### v2.0.0 — Store Release
- Create subscription products in Play Console ($0.99/mo, $4.99/yr)
- Build signed AAB + upload to internal testing track
- Store listing assets: screenshots (6 devices), feature graphic
- IARC content rating questionnaire
- Secrets migrated out of repo + keys rotated
- Google Play Store publication (US + Japan)
- Staged rollout (10% → 50% → 100%)
- Production monitoring (Firebase Crashlytics)

---

## Already Completed (Previously on Roadmap)

| Item | Status | Version |
|------|--------|---------|
| CameraX upgrade to 1.4+ (16KB alignment) | ✅ Done | 1.5.3 at v1.6.1 |
| Animated splash screen | ✅ Done | v1.0.1 |
| L1 Accessibility (TalkBack, font, contrast) | ✅ Done | v1.6.2 |
| Color token migration (KanjiSageColors) | ✅ Done | v1.6.3 |
| Deep links between Sage ↔ Journey | ✅ Done | v1.7.0 |
| Data safety form answers | ✅ Done | docs |
| Play Store listing text | ✅ Done | docs |
| Play Developer Account | ✅ Done | jay@jworks-ai.com |
| Privacy policy URL | ✅ Done | jworks-ai.com |

---

## Future (v2.x.x — Post-Launch)

### v2.1.0 — Professional Tools
- Medical terminology mode (医学用語): specialized kanji dictionary for healthcare interpreters
- Legal terminology mode (法律用語): contract/court document scanning
- Conference interpreter assist: real-time scan + translation overlay
- Custom dictionary import (user-defined term lists)

### v2.2.0 — Advanced OCR
- Handwritten kanji recognition (ML Kit handwriting or custom model)
- Traditional kanji (旧字体) support
- Multi-column vertical text (newspaper/magazine layout)
- Document scanning mode (photo → full-page OCR → structured output)

### v2.3.0 — Social & Community
- Share scanned passages with friends
- Community word lists (curated vocabulary packs)
- Leaderboards via J Coin ecosystem
- Teacher dashboard for classroom use (B2B licensing)

### v2.4.0 — Platform Expansion
- iOS native port (SwiftUI + Vision framework)
- iPad companion app with larger display features
- Raspberry Pi kiosk mode (museum/school installations)
- Wear OS quick-scan widget

---

## Cross-App Dependencies

| Feature | Depends On | App |
|---------|-----------|-----|
| J Coin cross-app rewards | Shared J Coin backend | All apps |
| Glass UI theme | Shared theme library | All apps |
| Shared vocabulary format | Common schema definition | EigoSage, EigoJourney |

---

## Milestones

| Milestone | Version | Target |
|-----------|---------|--------|
| Play Store submission | v2.0.0 | 2026-05-15 |
| 100 active users | v2.0.x | 2026-07-01 |
| AI Dialog Vision | v1.8.0 | TBD |
| Professional tools | v2.1.0 | 2026-08-01 |
| iOS launch | v2.4.0 | 2026-Q4 |

---

Last updated: 2026-04-15
