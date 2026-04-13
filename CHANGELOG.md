# KanjiSage Changelog

All notable changes to KanjiSage are documented here.
Version format: `vMAJOR.MINOR.PATCH` (MAJOR=stage [0=Alpha, 1=Beta, 2=Store], MINOR=feature, PATCH=deploy).
Single source of truth for version: `VERSION` file at repo root.

---

## v1.6.3 (2026-04-13)

### Fixed
- L1 compliance: Migrated remaining inline `Color(0x...)` literals to `KanjiSageColors` tokens across 10 files — DictionaryDetailView, RewardsScreen, PaywallScreen, OnboardingScreen, SplashScreen, AuthScreen, HandlePromptDialog, CrossPromoBanner, CameraScreen, ScanOverlays, ButtonCluster

### Changed
- Expanded `KanjiSageColors` with 14 tokens: `PosTagBg`, `CoinGradientEnd`, `CoinLetterBrown`, `ErrorLight`, `CardBgDark`, `CardBrownStart`, `CardBrownEnd`, `ActiveCardLight`, `AbyssBg`, `GoldGradientMid`, `GoldGradientDeep`, `GoogleBlue`, `DarkGreyText`, `ChallengePurple`

### Security
- chmod 600 on `.credentials`, `google-oauth-credentials.json`, `google-oauth-android-credentials.json`; added `SECRETS.md` documenting location + pre-launch hardening plan (move-out-of-repo + key rotation)

## v1.6.2 (2026-04-10)

### Fixed
- L1 compliance: Raised all font sizes below 12sp to 12sp (CameraScreen, KanjiDetailView)
- L1 compliance: Fixed onSurfaceVariant contrast — replaced alpha-based white with solid Color(0xFFCAC4D0)
- L1 compliance: Migrated hardcoded hex colors to KanjiSageColors tokens in HelpScreen, ProfileScreen, BookmarksScreen, KanjiDetailView

### Changed
- Expanded KanjiSageColors with 16 new design tokens (accent, semantic, muted, cross-promo)

## [Unreleased]

### Added
### Changed
### Fixed
### Removed

## v1.6.1 (2026-03-01)

### Changed
- Renamed package `com.jworks.kanjilens` → `com.jworks.kanjisage` across 95 .kt source files
- Renamed `KanjiLensApplication` → `KanjiSageApplication` (class + AndroidManifest)
- Renamed `KanjiLensColors` → `KanjiSageColors` (object in `ui/theme/`)
- Renamed `KanjiLensTheme` → `KanjiSageTheme` (composable function in `Theme.kt`)
- Updated `namespace` and `applicationId` in `app/build.gradle.kts` to `com.jworks.kanjisage`
- Updated `rootProject.name` in `settings.gradle.kts` to `"KanjiSage"`
- Updated keystore alias from `kanjilens` to `kanjisage` in `app/build.gradle.kts`
- Updated `app_name` in `res/values/strings.xml` from `KanjiLens` to `KanjiSage`
- Updated `Theme.KanjiLens` → `Theme.KanjiSage` in `res/values/themes.xml` and `AndroidManifest.xml`
- Updated `source_business` in `JCoinClient.kt` from `"kanjilens"` to `"kanjisage"` (3 occurrences)
- Updated `PREFS_NAME` in `JCoinEarnRules.kt` from `"kanjilens_jcoin"` to `"kanjisage_jcoin"`
- Updated product IDs in `BillingManager.kt` from `kanjilens_premium_*` to `kanjisage_premium_*`
- Updated ProGuard keep rules from `com.jworks.kanjilens` to `com.jworks.kanjisage`
- Updated `package_name` in `google-services.json` to `com.jworks.kanjisage`
- Updated all `KanjiQuest` cross-promo references to `KanjiJourney`
- Updated 32 documentation files (.md) with new naming

### Added
- `CHANGELOG.md` with full version history
- `ROADMAP.md` with planned versions through v2.4.0
- `VERSION` file (single source of truth for current version)

## v1.6.0 (2026-02-20)

### Added
- J Coin unified API client (`JCoinClient.kt`) with `source_business="kanjilens"` identifier
- Cumulative milestone rewards (scan count → J Coin bonuses)
- Share trigger: earn J Coin for sharing app via Android share sheet
- J Coin Store UI screen (`RewardsScreen.kt`) with balance display and earn history
- J Coin balance badge on camera screen (bottom-left, tappable)
- AI provider layer: `ClaudeProvider.kt`, `GeminiProvider.kt`, `AiProviderManager.kt`
- `GeminiOcrCorrector.kt` for AI-assisted OCR text cleanup
- `OcrTextMerger.kt` for combining ML Kit + AI OCR results
- `AiAnalysisPanel.kt` UI component for AI-powered text analysis
- `DraggablePanel.kt` reusable draggable bottom sheet component
- `SecureKeyStore.kt` for encrypted API key storage
- `AiModule.kt` Hilt DI module for AI providers
- `AiProvider.kt` interface and `ScopeLevel.kt` enum in domain layer

### Changed
- Adaptive OCR overlay: furigana auto-hides when detected lines exceed density threshold
- `SettingsScreen.kt` expanded with AI provider selection and API key management
- `SettingsViewModel.kt` updated for new AI-related settings
- `AppSettings.kt` data class expanded with AI provider preferences
- `SettingsDataStore.kt` persistence for AI settings
- `SettingsRepositoryImpl.kt` updated for new settings fields

## v1.5.2 (2026-02-13)

### Changed
- Splash screen animation durations doubled (~1.8s → ~3.6s total) for smoother branded intro
- Removed portrait orientation lock from `AndroidManifest.xml`
- Added `configChanges="orientation|screenSize"` to `MainActivity` for smooth rotation handling

## v1.5.1 (2026-02-13)

### Added
- `HandlePromptDialog.kt`: user display name prompt (3-20 chars, alphanumeric + underscore)
- `AuthUser.handle` field persisted in SharedPreferences
- Handle prompt triggers after 3rd scan via `incrementTotalScans()` in `CameraViewModel`
- "Maybe later" dismissal (permanent, re-accessible from Profile)
- Profile screen shows handle as primary display with avatar initial derived from handle
- "Set/Change display name" row in `ProfileScreen.kt` opens `HandlePromptDialog`

## v1.5.0 (2026-02-13)

### Added
- Anonymous-first auth: `createAnonymousSession()` generates local UUID in `AuthRepository.kt`
- `ensureSession()` auto-creates anonymous session on app launch
- `AuthState.SignedIn(user, isAnonymous)` flag to distinguish guest vs linked accounts
- `OnboardingScreen.kt`: 4-page HorizontalPager (Welcome, How It Works, Features, Get Started)
- `HelpScreen.kt`: app info, usage tips, privacy/rate/feedback links, credits section
- `hasSeenOnboarding` flag in `SettingsDataStore.kt` (shown once on first launch)
- "Help & About" row in `SettingsScreen.kt`
- `onboarding` and `help` composable routes in `MainActivity` NavHost

### Changed
- Navigation flow: Splash → onboarding (first launch only) → camera (no auth wall)
- `AuthScreen.kt` repurposed as "Link Account" screen with back button and privacy note
- `SettingsScreen.kt`: anonymous users see "Sign In / Link Account", linked users see "Sign Out"
- `ProfileScreen.kt`: shows "Guest" for anonymous, email for linked, "Link Account" CTA
- Sign out creates fresh anonymous session and stays on camera screen

### Removed
- Mandatory auth wall from app launch flow
- Google `displayName` and `avatarUrl` storage (privacy-first: email-only)

## v1.4.0 (2026-02-15)

### Added
- Adaptive OCR overlay density detection in `OverlayCanvas.kt`
- Density-based rendering thresholds: furigana hidden when line count exceeds limit

## v1.3.0 (2026-02-13)

### Added
- Native offline Kuromoji dictionary integration (`KuromojiTokenizer.kt`)
- `JapaneseToken.kt` domain model for morphological analysis results

### Changed
- Button layout redesign on `CameraScreen.kt`: 2x3 grid repositioned
- Subscription pricing updated in `PaywallScreen.kt`
- Rewards flow stabilized: fixed race condition in J Coin earn tracking
- Profile developer-tier controls: 2-state switch for admin premium toggle

## v1.2.0 (2026-02-11)

### Added
- Admin premium toggle in Profile → Developer Tools (`ProfileScreen.kt`)
- `user_roles` Supabase table query for developer role detection

### Changed
- J Coin earn rules stabilized in `JCoinEarnRules.kt`: fixed duplicate reward prevention

## v1.1.0 (2026-02-09)

### Added
- Google Play Billing integration (`BillingManager.kt`, `billing-ktx:6.1.0`)
- Free-tier scan limits: 5 scans/day, 60-second countdown timer per scan
- Timer pill UI (top-center, turns red at ≤10 seconds)
- Scan-expired overlay with "Start New Scan" and "Upgrade to Premium" buttons
- `PaywallScreen.kt` with localized prices from Google Play Store
- Pause/play scan feature in `CameraViewModel.kt` (`startScan`/`stopScan` state)
- Supabase Edge Function feedback system (`FeedbackRepositoryImpl.kt`, `FeedbackDialog.kt`)
- Flash toggle icon (`ic_flashlight_on.xml`, `ic_flashlight_off.xml`) with yellow tint when active
- `ProfileScreen.kt` with user info, ecosystem links, subscription badge, sign-out
- `CameraScreen.kt` 2x3 draggable button grid (top-right), feedback + J Coin (bottom-left)
- "Manage Subscription on Google Play" link in `SettingsScreen.kt`
- Release APK signing config with `keystore/kanjisage-release.jks`
- ProGuard billing keep rules in `proguard-rules.pro`
- Release + debug SHA-1 fingerprints registered in GCP OAuth

### Changed
- `PaywallScreen.kt`: "Maybe later" always visible + BackHandler for dismiss

### Removed
- Stripe SDK (`stripe-android:20.35.0`)
- AndroidX Browser (`browser:1.7.0`) for Stripe Custom Tabs
- Supabase edge functions for Stripe checkout/verify/cancel

## v1.0.1 (2026-02-06)

### Added
- Vertical text mode (縦書き): `verticalTextMode` setting with DataStore persistence
- 縦/横 quick-toggle floating button on camera screen
- Vertical rendering: charHeight-based segment positioning (top-to-bottom)
- Vertical furigana: each kana character stacked in tall narrow pills to the right of kanji
- Partial mode detection area filtering (`PartialModeConstants.kt`)
- Draggable boundary divider + floating buttons for partial/full mode switching
- Kuromoji morphological analyzer (`KuromojiTokenizer.kt`) for context-aware furigana
- UI customization: font size range slider, opacity range slider, bounding box toggle
- Bold text toggle + text color toggles in Settings
- `calculateVisibleRegion()` with `isVerticalMode` parameter for FILL_CENTER inverse mapping
- Result persistence: keep previous detections for 3 sparse frames to reduce flickering
- `_visibleRegion` StateFlow for jukugo segment-level position filtering
- 10 new unit tests (32 total, all passing)

### Fixed
- Z Flip 7 NoSuchMethodError crash: Compose BOM `2024.01.00` → `2024.02.02`, compiler `1.5.8` → `1.5.10`
- Division by zero in `OverlayCanvas` when image width/height is 0
- Empty `Rect()` bounding boxes rendered: added `bounds.isEmpty` check
- `drawText` crash on negative text positions: added guard
- Toggle button animation race condition: `remember(modeKey)` + `LaunchedEffect` fix
- FILL_CENTER coordinate mapping for square camera sensor aspect ratio
- Overlay stability: off-screen bounds checks for right-side and top-side ghost furigana

## v1.0.0 (2026-02-05)

### Added
- CameraX integration with tap-to-focus, flash toggle, rotation handling (`CameraPreview.kt`)
- ML Kit Japanese OCR with offline text recognition (`ProcessCameraFrameUseCase.kt`)
- Japanese text filtering using Unicode ranges: CJK Unified (4E00-9FFF), Extension A (3400-4DBF), Hiragana, Katakana (`JapaneseTextUtil.kt`)
- Text overlay Canvas rendering with labels and debug HUD (`OverlayCanvas.kt`)
- Furigana integration with JMDict SQLite database (`JMDictDatabase.kt`, `JMDictDao.kt`)
- Per-kanji-segment rendering with positional furigana alignment (`EnrichWithFuriganaUseCase.kt`)
- Element-level kanji rendering with line-context enrichment
- Settings UI with frame skip control, debug HUD toggle, system insets
- Hilt dependency injection (`AppModule.kt`, `AuthModule.kt`, `DatabaseModule.kt`)
- Room + SQLite local dictionary (`UserDataDatabase.kt`, `DictionaryDao.kt`, `BookmarkDao.kt`)
- Frame skipping (every 3rd frame) + `isProcessing` guard to prevent OCR backpressure
- Rolling 30-frame OCR stats with debug HUD overlay
- 22 unit tests (JapaneseTextUtil: 16, CoordinateMapping: 6)

## v0.1.0 (2026-02-05)

### Added
- Android project scaffolding (`build.gradle.kts`, `settings.gradle.kts`, `AndroidManifest.xml`)
- Gradle wrapper (8.5)
- Project documentation: `README.md`, `CLAUDE.md`, strategic planning document
- `.gitignore` for Android project
- Git repository initialized
