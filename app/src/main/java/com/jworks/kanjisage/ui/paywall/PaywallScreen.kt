package com.jworks.kanjisage.ui.paywall

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jworks.kanjisage.data.billing.BillingManager
import com.jworks.kanjisage.ui.theme.KanjiSageColors
import androidx.compose.ui.res.stringResource
import com.jworks.kanjisage.R
import com.jworks.kanjisage.ui.theme.focusBorder

@Composable
fun PaywallScreen(
    billingManager: BillingManager,
    activity: Activity,
    remainingScans: Int,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    var selectedPlan by remember { mutableStateOf(BillingManager.PRODUCT_MONTHLY) }
    val productDetails by billingManager.productDetails.collectAsState()

    // Get localized prices from Play Store, fallback to defaults
    val monthlyDetails = productDetails[BillingManager.PRODUCT_MONTHLY]
    val annualDetails = productDetails[BillingManager.PRODUCT_ANNUAL]
    val monthlyPrice = monthlyDetails?.subscriptionOfferDetails?.firstOrNull()
        ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$0.99"
    val annualPrice = annualDetails?.subscriptionOfferDetails?.firstOrNull()
        ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$4.99"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        KanjiSageColors.DarkBg,
                        KanjiSageColors.HeaderBg
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = stringResource(R.string.paywall_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (remainingScans > 0) {
                Text(
                    text = if (remainingScans != 1) stringResource(R.string.paywall_scans_left_plural, remainingScans) else stringResource(R.string.paywall_scans_left_singular, remainingScans),
                    fontSize = 14.sp,
                    color = KanjiSageColors.CoinAccent
                )
            } else {
                Text(
                    text = stringResource(R.string.paywall_scans_used),
                    fontSize = 14.sp,
                    color = KanjiSageColors.TimerWarning
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Features list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureRow(stringResource(R.string.paywall_feature_scanning), stringResource(R.string.paywall_feature_scanning_desc))
                FeatureRow(stringResource(R.string.paywall_feature_dictionary), stringResource(R.string.paywall_feature_dictionary_desc))
                FeatureRow(stringResource(R.string.paywall_feature_history), stringResource(R.string.paywall_feature_history_desc))
                FeatureRow(stringResource(R.string.paywall_feature_bookmarks), stringResource(R.string.paywall_feature_bookmarks_desc))
                FeatureRow(stringResource(R.string.paywall_feature_jcoin), stringResource(R.string.paywall_feature_jcoin_desc))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Plan cards
            PlanCard(
                title = stringResource(R.string.paywall_plan_monthly),
                price = monthlyPrice,
                period = stringResource(R.string.paywall_period_month),
                isSelected = selectedPlan == BillingManager.PRODUCT_MONTHLY,
                onClick = { selectedPlan = BillingManager.PRODUCT_MONTHLY }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PlanCard(
                title = stringResource(R.string.paywall_plan_annual),
                price = annualPrice,
                period = stringResource(R.string.paywall_period_year),
                savings = stringResource(R.string.paywall_save_percent),
                isSelected = selectedPlan == BillingManager.PRODUCT_ANNUAL,
                onClick = { selectedPlan = BillingManager.PRODUCT_ANNUAL }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Subscribe button
            Button(
                onClick = {
                    billingManager.launchPurchaseFlow(activity, selectedPlan)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KanjiSageColors.PrimaryAction
                ),
                shape = RoundedCornerShape(28.dp),
                enabled = productDetails.isNotEmpty()
            ) {
                Text(
                    text = stringResource(R.string.paywall_subscribe),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (productDetails.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.paywall_loading),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dismiss
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (remainingScans > 0) stringResource(R.string.paywall_keep_free) else stringResource(R.string.paywall_not_now),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.paywall_cancel_note),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            // Bundle promo
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(KanjiSageColors.ActiveCard)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.paywall_bundle_title),
                        color = KanjiSageColors.CoinAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.paywall_bundle_desc),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "\u2713",
            color = KanjiSageColors.PrimaryAction,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    period: String,
    savings: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) KanjiSageColors.PrimaryAction else Color.White.copy(alpha = 0.2f)
    val bgColor = if (isSelected) KanjiSageColors.ActiveCard else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .focusBorder(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (savings != null) {
                    Text(
                        text = savings,
                        color = KanjiSageColors.Primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = price,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = period,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }
        }
    }
}
