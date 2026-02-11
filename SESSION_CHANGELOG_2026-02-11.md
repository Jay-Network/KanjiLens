# KanjiLens Session Changelog (2026-02-11)

## Scope
This changelog records changes made during the Codex takeover session, with emphasis on **kanji/furigana recognition** and UI updates.

## Recognition Pipeline (Kanji/Furigana)
### File
- `app/src/main/java/com/jworks/kanjilens/domain/usecases/EnrichWithFuriganaUseCase.kt`

### Timeline
1. Baseline behavior (last committed):
- Kuromoji path when tokenizer is ready.
- JMDict fallback path when Kuromoji is not ready.
- Candidate extraction originally targeted multi-char-first matching.

2. Temporary changes introduced in-session:
- Added hybrid fill logic (Kuromoji first, then JMDict fill for missing readings).
- Expanded JMDict candidate extraction to include single-kanji candidates (`len=1`).

3. User requested rollback of only recognition logic:
- **Recognition file was restored to the last committed version** using git checkout.
- Temporary hybrid/single-kanji candidate modifications are no longer present in this file.

## UI / Navigation / Scan Flow Changes (Kept)
### Main app flow
- `app/src/main/java/com/jworks/kanjilens/MainActivity.kt`
- `app/src/main/java/com/jworks/kanjilens/ui/auth/AuthScreen.kt`
- `app/src/main/java/com/jworks/kanjilens/ui/paywall/PaywallScreen.kt`

Changes kept:
- Login-first flow on app launch.
- Paywall dismissal/back handling adjustments.

### Camera screen + controls
- `app/src/main/java/com/jworks/kanjilens/ui/camera/CameraViewModel.kt`
- `app/src/main/java/com/jworks/kanjilens/ui/camera/CameraScreen.kt`

Changes kept:
- Scan session timer/paywall state flow.
- Paywall loop prevention behavior.
- Controls layout updates (including latest request-driven positioning edits).
- Bottom-left feedback button moved higher.

## Build/Delivery
- Debug builds were repeatedly validated with `./gradlew assembleDebug`.
- APK was served via HTTP on port `8888` as:
  - `kanjilens-debug.apk`

## Current State Summary
- Recognition logic file has been reverted to committed state.
- Other app updates from this session remain in working tree.

## Late Session Updates (Post-takeover)
### J Coin / Rewards stability
- `app/src/main/java/com/jworks/kanjilens/ui/rewards/RewardsScreen.kt`
- `app/src/main/java/com/jworks/kanjilens/data/subscription/SubscriptionManager.kt`

Changes:
- Added explicit back handling on Rewards screen.
- Refactored rewards content branching to stable composable sections (`SignedOut`, `PremiumRequired`, `Balance`) to address Compose group imbalance crash seen when tapping `J`.
- Wired Rewards premium gating to reactive `isPremiumFlow` (override + billing combined state).

### Profile / Developer Tools
- `app/src/main/java/com/jworks/kanjilens/ui/profile/ProfileScreen.kt`

Changes:
- Added/kept back navigation behavior for profile/jcoin paths.
- Removed `Real` mode from Developer Tools tier override.
- Developer override is now strictly two-state: `Premium` or `Free`.

### Camera button layout updates
- `app/src/main/java/com/jworks/kanjilens/ui/camera/CameraScreen.kt`

Changes:
- J Coin button moved to lower-left area (just above feedback).
- Feedback button raised.
- Right-side 2x3 control cluster shifted downward to align lower row with feedback-row height.

### Build + deploy verification
Commands run:
- `./gradlew assembleDebug` (success)
- `adb -s zflip7-ts:37399 install -r app/build/outputs/apk/debug/app-debug.apk` (success)
