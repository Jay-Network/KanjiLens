package com.jworks.kanjilens.data.subscription

import android.content.Context
import com.jworks.kanjilens.data.billing.BillingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.jworks.kanjilens.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionManager @Inject constructor(
    private val billingManager: BillingManager
) {
    companion object {
        const val FREE_SCAN_LIMIT = 5
        const val SCAN_COUNT_KEY = "daily_scan_count"
        const val SCAN_DATE_KEY = "daily_scan_date"
    }

    private val _premiumOverride = MutableStateFlow<Boolean?>(if (BuildConfig.DEBUG) true else null)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val isPremiumFlow: StateFlow<Boolean> = combine(
        billingManager.isPremium,
        _premiumOverride
    ) { billing, override ->
        override ?: billing
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = billingManager.isPremium.value
    )

    fun isPremium(): Boolean = _premiumOverride.value ?: billingManager.isPremium.value

    fun setPremiumOverride(override: Boolean?, context: Context? = null) {
        _premiumOverride.value = override
        // Reset scan counter when developer toggles tier
        if (context != null) {
            val prefs = context.getSharedPreferences("kanjilens_usage", Context.MODE_PRIVATE)
            prefs.edit()
                .putString(SCAN_DATE_KEY, java.time.LocalDate.now().toString())
                .putInt(SCAN_COUNT_KEY, 0)
                .apply()
        }
    }

    fun getPremiumOverride(): Boolean? = _premiumOverride.value

    // Feature gates: FREE vs PREMIUM
    fun hasUnlimitedScans(): Boolean = isPremium()
    fun hasHistory(): Boolean = isPremium()
    fun hasOfflineDictionary(): Boolean = isPremium()
    fun hasJCoinEarning(): Boolean = isPremium()
    fun hasFavorites(): Boolean = isPremium()

    fun canScan(context: Context): Boolean {
        if (isPremium()) return true

        val prefs = context.getSharedPreferences("kanjilens_usage", Context.MODE_PRIVATE)
        val today = java.time.LocalDate.now().toString()
        val savedDate = prefs.getString(SCAN_DATE_KEY, null)

        if (savedDate != today) {
            prefs.edit()
                .putString(SCAN_DATE_KEY, today)
                .putInt(SCAN_COUNT_KEY, 0)
                .apply()
            return true
        }

        return prefs.getInt(SCAN_COUNT_KEY, 0) < FREE_SCAN_LIMIT
    }

    fun incrementScanCount(context: Context) {
        if (isPremium()) return

        val prefs = context.getSharedPreferences("kanjilens_usage", Context.MODE_PRIVATE)
        val today = java.time.LocalDate.now().toString()
        val savedDate = prefs.getString(SCAN_DATE_KEY, null)

        if (savedDate != today) {
            prefs.edit()
                .putString(SCAN_DATE_KEY, today)
                .putInt(SCAN_COUNT_KEY, 1)
                .apply()
        } else {
            val current = prefs.getInt(SCAN_COUNT_KEY, 0)
            prefs.edit().putInt(SCAN_COUNT_KEY, current + 1).apply()
        }
    }

    fun getRemainingScans(context: Context): Int {
        if (isPremium()) return Int.MAX_VALUE

        val prefs = context.getSharedPreferences("kanjilens_usage", Context.MODE_PRIVATE)
        val today = java.time.LocalDate.now().toString()
        val savedDate = prefs.getString(SCAN_DATE_KEY, null)

        if (savedDate != today) return FREE_SCAN_LIMIT

        return (FREE_SCAN_LIMIT - prefs.getInt(SCAN_COUNT_KEY, 0)).coerceAtLeast(0)
    }
}
