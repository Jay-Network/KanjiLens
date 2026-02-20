package com.jworks.kanjilens.data.jcoin

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * KanjiLens J Coin earn rules with daily caps.
 *
 * | Action                | Coins | Frequency            |
 * |-----------------------|-------|----------------------|
 * | First scan of day     | 5     | Daily                |
 * | Save to favorites     | 2     | Per action, cap 10/d |
 * | Dictionary lookup     | 1     | Per action, cap 5/d  |
 * | Scan Challenge        | 10    | Per challenge, cap 3/d|
 * | 10 scans milestone    | 10    | Daily                |
 * | 7-day streak          | 50    | Weekly               |
 * | Share scan result     | 5     | Per action, cap 2/d  |
 *
 * Daily cap: 200 coins (engagement source cap from backend)
 */
@Singleton
class JCoinEarnRules @Inject constructor() {

    companion object {
        const val DAILY_CAP = 200
        private const val PREFS_NAME = "kanjilens_jcoin"
        private const val KEY_DATE = "jcoin_date"
        private const val KEY_DAILY_EARNED = "jcoin_daily_earned"
        private const val KEY_FIRST_SCAN_CLAIMED = "first_scan_claimed"
        private const val KEY_FAVORITES_COUNT = "favorites_coin_count"
        private const val KEY_MILESTONE_CLAIMED = "milestone_claimed"
        private const val KEY_SHARE_COUNT = "share_coin_count"
        private const val KEY_SCAN_COUNT_TODAY = "scan_count_today"
        private const val KEY_STREAK_DAYS = "streak_days"
        private const val KEY_LAST_SCAN_DATE = "last_scan_date"
        private const val KEY_STREAK_CLAIMED_WEEK = "streak_claimed_week"
        private const val KEY_LOOKUP_COUNT = "lookup_coin_count"
        private const val KEY_CHALLENGE_COUNT = "challenge_count"
        private const val KEY_STREAK_30_CLAIMED = "streak_30_claimed"
        private const val KEY_STREAK_90_CLAIMED = "streak_90_claimed"
    }

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun ensureToday(context: Context) {
        val prefs = getPrefs(context)
        val today = java.time.LocalDate.now().toString()
        if (prefs.getString(KEY_DATE, null) != today) {
            // Update streak tracking
            val yesterday = java.time.LocalDate.now().minusDays(1).toString()
            val lastScanDate = prefs.getString(KEY_LAST_SCAN_DATE, null)
            val currentStreak = prefs.getInt(KEY_STREAK_DAYS, 0)

            val newStreak = if (lastScanDate == yesterday) currentStreak + 1 else 1

            prefs.edit()
                .putString(KEY_DATE, today)
                .putInt(KEY_DAILY_EARNED, 0)
                .putBoolean(KEY_FIRST_SCAN_CLAIMED, false)
                .putInt(KEY_FAVORITES_COUNT, 0)
                .putBoolean(KEY_MILESTONE_CLAIMED, false)
                .putInt(KEY_SHARE_COUNT, 0)
                .putInt(KEY_LOOKUP_COUNT, 0)
                .putInt(KEY_CHALLENGE_COUNT, 0)
                .putInt(KEY_SCAN_COUNT_TODAY, 0)
                .putInt(KEY_STREAK_DAYS, newStreak)
                .apply()
        }
    }

    fun getDailyEarned(context: Context): Int {
        ensureToday(context)
        return getPrefs(context).getInt(KEY_DAILY_EARNED, 0)
    }

    fun getRemainingDaily(context: Context): Int {
        return (DAILY_CAP - getDailyEarned(context)).coerceAtLeast(0)
    }

    private fun addDailyEarned(context: Context, amount: Int): Int {
        val remaining = getRemainingDaily(context)
        val actual = amount.coerceAtMost(remaining)
        if (actual > 0) {
            val prefs = getPrefs(context)
            prefs.edit().putInt(KEY_DAILY_EARNED, getDailyEarned(context) + actual).apply()
        }
        return actual
    }

    /** First scan of the day: 5 coins */
    fun checkFirstScan(context: Context): EarnAction? {
        ensureToday(context)
        val prefs = getPrefs(context)
        if (prefs.getBoolean(KEY_FIRST_SCAN_CLAIMED, false)) return null

        val coins = addDailyEarned(context, 5)
        if (coins > 0) {
            prefs.edit().putBoolean(KEY_FIRST_SCAN_CLAIMED, true).apply()
            return EarnAction("first_scan_of_day", coins)
        }
        return null
    }

    /** Save to favorites: 2 coins, cap 10/day */
    fun checkFavorite(context: Context): EarnAction? {
        ensureToday(context)
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_FAVORITES_COUNT, 0)
        if (count >= 10) return null // cap: 10 saves * 2 coins = 20/day

        val coins = addDailyEarned(context, 2)
        if (coins > 0) {
            prefs.edit().putInt(KEY_FAVORITES_COUNT, count + 1).apply()
            return EarnAction("save_to_favorites", coins)
        }
        return null
    }

    /** 10 scans milestone: 10 coins (once per day) */
    fun checkScanMilestone(context: Context): EarnAction? {
        ensureToday(context)
        val prefs = getPrefs(context)
        if (prefs.getBoolean(KEY_MILESTONE_CLAIMED, false)) return null

        val scanCount = prefs.getInt(KEY_SCAN_COUNT_TODAY, 0)
        if (scanCount < 10) return null

        val coins = addDailyEarned(context, 10)
        if (coins > 0) {
            prefs.edit().putBoolean(KEY_MILESTONE_CLAIMED, true).apply()
            return EarnAction("ten_scans_milestone", coins)
        }
        return null
    }

    /** 7-day streak: 50 coins (once per streak cycle) */
    fun checkStreak(context: Context): EarnAction? {
        ensureToday(context)
        val prefs = getPrefs(context)
        val streak = prefs.getInt(KEY_STREAK_DAYS, 0)
        if (streak < 7) return null

        // Only award once per 7-day cycle
        val weekNum = streak / 7
        val lastClaimedWeek = prefs.getInt(KEY_STREAK_CLAIMED_WEEK, 0)
        if (weekNum <= lastClaimedWeek) return null

        val coins = addDailyEarned(context, 50)
        if (coins > 0) {
            prefs.edit().putInt(KEY_STREAK_CLAIMED_WEEK, weekNum).apply()
            return EarnAction("seven_day_streak", coins)
        }
        return null
    }

    /** 30-day streak: 100 coins (once) */
    fun checkStreak30(context: Context): EarnAction? {
        ensureToday(context)
        val prefs = getPrefs(context)
        val streak = prefs.getInt(KEY_STREAK_DAYS, 0)
        if (streak < 30) return null
        if (prefs.getBoolean(KEY_STREAK_30_CLAIMED, false)) return null

        val coins = addDailyEarned(context, 100)
        if (coins > 0) {
            prefs.edit().putBoolean(KEY_STREAK_30_CLAIMED, true).apply()
            return EarnAction("streak_30_days", coins)
        }
        return null
    }

    /** 90-day streak: 300 coins (once) */
    fun checkStreak90(context: Context): EarnAction? {
        ensureToday(context)
        val prefs = getPrefs(context)
        val streak = prefs.getInt(KEY_STREAK_DAYS, 0)
        if (streak < 90) return null
        if (prefs.getBoolean(KEY_STREAK_90_CLAIMED, false)) return null

        val coins = addDailyEarned(context, 300)
        if (coins > 0) {
            prefs.edit().putBoolean(KEY_STREAK_90_CLAIMED, true).apply()
            return EarnAction("streak_90_days", coins)
        }
        return null
    }

    /** Dictionary lookup: 1 coin, cap 5/day */
    fun checkDictionaryLookup(context: Context): EarnAction? {
        ensureToday(context)
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_LOOKUP_COUNT, 0)
        if (count >= 5) return null // cap: 5 lookups * 1 coin = 5/day

        val coins = addDailyEarned(context, 1)
        if (coins > 0) {
            prefs.edit().putInt(KEY_LOOKUP_COUNT, count + 1).apply()
            return EarnAction("dictionary_lookup", coins)
        }
        return null
    }

    /** Scan Challenge completed: 10 coins (cap 3/day) */
    fun checkScanChallenge(context: Context): EarnAction? {
        ensureToday(context)
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_CHALLENGE_COUNT, 0)
        if (count >= 3) return null // cap: 3 challenges/day

        val coins = addDailyEarned(context, 10)
        if (coins > 0) {
            prefs.edit().putInt(KEY_CHALLENGE_COUNT, count + 1).apply()
            return EarnAction("scan_challenge", coins)
        }
        return null
    }

    fun getLookupCountToday(context: Context): Int {
        ensureToday(context)
        return getPrefs(context).getInt(KEY_LOOKUP_COUNT, 0)
    }

    fun isChallengeCompletedToday(context: Context): Boolean {
        ensureToday(context)
        return getPrefs(context).getInt(KEY_CHALLENGE_COUNT, 0) >= 3
    }

    fun getChallengeCountToday(context: Context): Int {
        ensureToday(context)
        return getPrefs(context).getInt(KEY_CHALLENGE_COUNT, 0)
    }

    /** Share scan result: 5 coins, cap 2/day */
    fun checkShare(context: Context): EarnAction? {
        ensureToday(context)
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_SHARE_COUNT, 0)
        if (count >= 2) return null // cap: 2 shares * 5 coins = 10/day

        val coins = addDailyEarned(context, 5)
        if (coins > 0) {
            prefs.edit().putInt(KEY_SHARE_COUNT, count + 1).apply()
            return EarnAction("share_scan_result", coins)
        }
        return null
    }

    /** Record a scan (for milestone tracking) */
    fun recordScan(context: Context) {
        ensureToday(context)
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_SCAN_COUNT_TODAY, 0)
        prefs.edit()
            .putInt(KEY_SCAN_COUNT_TODAY, count + 1)
            .putString(KEY_LAST_SCAN_DATE, java.time.LocalDate.now().toString())
            .apply()
    }

    fun getStreakDays(context: Context): Int {
        ensureToday(context)
        return getPrefs(context).getInt(KEY_STREAK_DAYS, 0)
    }

    fun getScanCountToday(context: Context): Int {
        ensureToday(context)
        return getPrefs(context).getInt(KEY_SCAN_COUNT_TODAY, 0)
    }
}

data class EarnAction(
    val sourceType: String,
    val coins: Int
)
