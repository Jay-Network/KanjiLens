# KanjiLens TODO & Future Enhancements

**Last Updated**: 2026-02-05
**Current Phase**: Phase 1 Polish

---

## 🔥 High Priority - Traditional Kanji Recognition Issue

### Problem
ML Kit OCR fails to recognize traditional/old kanji forms (kyūjitai 旧字体):
- Example: 萬 (traditional) vs 万 (modern) - "ten thousand"
- ML Kit was trained primarily on modern Japanese text
- Traditional forms are misread or skipped entirely

### Test Cases That Fail
| Traditional | Modern | ML Kit Result |
|-------------|--------|---------------|
| 萬 | 万 | ❌ Misread or skipped |
| 國 | 国 | ❌ Likely fails |
| 學 | 学 | ❌ Likely fails |
| 廣 | 広 | ⚠️ Maybe works |
| 變 | 変 | ❌ Likely fails |
| 龍 | 竜 | ⚠️ Maybe works |
| 經 | 経 | ❌ Likely fails |

### Solutions (Prioritized)

#### Option A: Kanji Variant Dictionary 📋 (Short-term - RECOMMENDED FIRST)
**Effort**: 4-6 hours
**Impact**: Helps with ~200 common traditional kanji

**Implementation**:
1. Create variant mapping dictionary (traditional → modern)
   ```kotlin
   val kanjiVariants = mapOf(
       "萬" to "万",
       "國" to "国",
       "學" to "学",
       // ... 200+ mappings
   )
   ```
2. Post-process OCR results
3. Display both forms: "萬→万" or "萬(万)"
4. Add Settings toggle: "Auto-convert traditional kanji"

**Pros**:
- Fast to implement
- Works offline
- No API costs
- Educates users (shows both forms)

**Cons**:
- Only works if ML Kit detects something
- Manual dictionary maintenance
- Limited to known mappings

**Priority**: Medium (implement after Phase 1 Polish)

---

#### Option B: Hybrid OCR with Cloud Vision Fallback ☁️ (Mid-term - WORTH TESTING)
**Effort**: 8-12 hours
**Impact**: Much better accuracy on ALL difficult kanji

**Implementation**:
1. Integrate Google Cloud Vision API
2. Add confidence-based fallback logic:
   ```kotlin
   if (mlKitConfidence < 0.7) {
       cloudResult = cloudVisionAPI.detect(region)
   }
   ```
3. Add Settings option:
   ```
   OCR Engine:
   ○ Fast (ML Kit only)
   ● Balanced (ML Kit + fallback)
   ○ Accurate (Cloud Vision)
   ```

**Costs**:
- Google Cloud Vision: $1.50 per 1,000 requests
- Estimated: <5% of detections need fallback
- Monthly cost for heavy user: ~$2-5

**Pros**:
- Significantly better accuracy
- Handles traditional kanji, handwriting, stylized fonts
- Only uses API when needed (cost-effective)

**Cons**:
- Requires internet for fallback
- API costs (small but ongoing)
- Additional complexity
- Slower (500ms-1s for Cloud Vision)

**Priority**: Medium-High (test after Option A)

---

#### Option C: Full Solution (Both A + B) 🎯 (Long-term - BEST UX)
**Effort**: 12-16 hours
**Impact**: Maximum accuracy + offline support

**Implementation**:
1. Default: ML Kit + Variant Dictionary (fast, offline)
2. Optional: Cloud Vision fallback mode
3. Manual correction: Tap to fix errors
4. Learning system: Remember user corrections

**User Experience**:
```
1. ML Kit detects text (fast, offline)
2. Variant dictionary maps traditional→modern
3. If confidence low (<70%), optional Cloud Vision
4. User can tap to correct any remaining errors
5. App remembers corrections for future
```

**Pros**:
- Best accuracy possible
- Works offline (with dictionary)
- Optional cloud accuracy boost
- User can override anything
- Learns from corrections

**Cons**:
- Most complex to implement
- Requires API setup
- More UI for settings/corrections

**Priority**: Low (implement after Phase 2 or later)

---

## 📋 Phase Roadmap

### ✅ Phase 1: MVP Core (COMPLETE)
- [x] Android project setup
- [x] CameraX integration
- [x] ML Kit Japanese OCR
- [x] Basic text overlay with boxes/labels
- [x] Unit tests (22 tests)
- [x] Bug fixes (Compose BOM, division-by-zero, empty bounds)
- [x] Testing on Z Flip 7

### 🚀 Phase 1 Polish (IN PROGRESS)
- [ ] Settings UI (text size, colors, overlay options)
- [ ] UX improvements (animations, feedback, visual polish)
- [ ] Performance optimization (battery, frame rate)
- [ ] Onboarding tutorial
- [ ] Help/About screen

**Estimated**: 1-2 weeks (30-50 hours)

### 📖 Phase 2: Furigana Integration (NEXT)
- [ ] Download and prepare JMDict database
- [ ] Integrate JMDict SQLite (local lookup)
- [ ] Build Kuroshiro backend service (Node.js)
- [ ] Implement furigana lookup logic
- [ ] Overlay furigana above detected kanji
- [ ] Caching and optimization
- [ ] Testing on real Japanese text

**Estimated**: 2-3 weeks (50-70 hours)

### 🎨 Phase 3: Advanced Features
- [ ] **Option A**: Kanji variant dictionary (traditional→modern) ⭐
- [ ] **Option B**: Hybrid OCR with Cloud Vision (test viability)
- [ ] Screenshot/save mode
- [ ] History of detected text
- [ ] Flashcard mode (detected kanji → study list)
- [ ] Vocabulary builder

**Estimated**: 2-4 weeks

### 🚀 Phase 4: Production Ready
- [ ] **Option C**: Full traditional kanji solution (if needed)
- [ ] Play Store optimization
- [ ] Privacy policy
- [ ] Beta testing program
- [ ] Marketing materials
- [ ] Launch on Google Play Store

**Estimated**: 1-2 weeks

---

## 🐛 Known Issues

### Critical
None currently

### Medium Priority
- [ ] Traditional kanji (萬, 國, 學) not recognized - **See Options A/B/C above**
- [ ] Handwritten text detection poor (expected ML Kit limitation)
- [ ] Very small text (<12pt) sometimes skipped

### Low Priority
- [ ] Stylized fonts (calligraphy) have low accuracy
- [ ] Vertical text orientation not optimized
- [ ] Mixed Japanese/English detection could be smarter

---

## 💡 Future Enhancement Ideas

### User-Requested Features
- [ ] Audio pronunciation (TTS for detected text)
- [ ] Translation mode (Japanese → English)
- [ ] Dark mode support
- [ ] Custom color schemes
- [ ] Export detected text to notes app
- [ ] Share detected text via SMS/email

### Technical Improvements
- [ ] Reduce app size (<50MB target)
- [ ] Faster cold start time
- [ ] Better memory efficiency
- [ ] Support for older Android versions (SDK 24→21)

### Advanced OCR
- [ ] Support Chinese characters (similar tech stack)
- [ ] Support Korean (Hangul)
- [ ] Mixed language detection (auto-switch)
- [ ] Document scanning mode (full page OCR)

### Gamification (KanjiQuest Integration)
- [ ] Count unique kanji detected
- [ ] "Kanji caught" achievement system
- [ ] Daily challenges (find specific kanji)
- [ ] Progress tracking

---

## 📊 Performance Targets

### Current Performance (Phase 1)
- OCR Processing: ~180ms average
- Frame Rate: 30 FPS
- Battery Drain: ~20% per hour (needs optimization)
- App Size: ~25MB

### Target Performance (After Optimization)
- OCR Processing: <150ms
- Frame Rate: 30 FPS sustained
- Battery Drain: <15% per hour
- App Size: <30MB (with JMDict database)

---

## 🔧 Technical Debt

### Code Quality
- [ ] Add more comprehensive unit tests (target 80% coverage)
- [ ] Add integration tests for camera→OCR→overlay pipeline
- [ ] Add UI tests for settings screen
- [ ] Improve error handling and edge cases
- [ ] Add logging and crash analytics

### Documentation
- [ ] Add inline code documentation
- [ ] Create architecture decision records (ADRs)
- [ ] Document API contracts
- [ ] Create developer setup guide
- [ ] Add troubleshooting guide

### Infrastructure
- [ ] Set up CI/CD pipeline (GitHub Actions)
- [ ] Automated APK builds
- [ ] Automated testing on commit
- [ ] Code quality checks (ktlint, detekt)

---

## 📝 Notes

### Traditional Kanji Recognition - Research Notes

**Why ML Kit fails**:
- Training data bias toward modern Japanese (post-2000)
- Shinjitai (新字体) heavily represented
- Kyūjitai (旧字体) underrepresented
- Historical documents rarely in training set

**Alternative OCR engines to consider**:
1. **Google Cloud Vision**: Best accuracy, costs money
2. **Tesseract 4.0+**: Open source, trainable, slower
3. **Azure Computer Vision**: Similar to Cloud Vision
4. **PaddleOCR**: Chinese-focused, good with traditional characters
5. **EasyOCR**: Python-based, supports many languages

**Custom model training** (advanced, future consideration):
- Could fine-tune ML Kit model with traditional kanji dataset
- Requires significant ML expertise
- Needs large dataset of traditional kanji examples
- Time investment: weeks to months
- May not be worth it vs Cloud Vision API

### Testing Resources

**Where to find traditional kanji for testing**:
- Old Japanese literature (青空文庫)
- Pre-1946 documents
- Taiwan/Hong Kong Chinese (uses traditional characters)
- Historical signs and monuments
- Classical texts (古典)

**Test dataset ideas**:
- Collect 100 sample images with traditional kanji
- Benchmark each solution (A, B, C) against dataset
- Measure accuracy improvement
- Document cost vs accuracy tradeoffs

---

## 🎯 Decision Log

### 2026-02-05: Traditional Kanji Solutions Documented
**Decision**: Prioritize Phase 1 Polish, defer traditional kanji to Phase 3
**Rationale**:
- Core OCR works well for modern Japanese (95%+ use case)
- Phase 2 (furigana) more valuable to users immediately
- Traditional kanji is edge case (<5% of daily usage)
- Options A/B/C documented for future implementation

**Implementation order**:
1. Phase 1 Polish (settings, UX) - 1-2 weeks
2. Phase 2 (furigana) - 2-3 weeks
3. Phase 3: Option A (variant dictionary) - 4-6 hours
4. Test Option B (Cloud Vision) - evaluate ROI
5. Option C if needed based on user feedback

---

**Maintained by**: KanjiLens-Dev (jworks:43)
**Review schedule**: Weekly during active development
**Next review**: After Phase 1 Polish completion
