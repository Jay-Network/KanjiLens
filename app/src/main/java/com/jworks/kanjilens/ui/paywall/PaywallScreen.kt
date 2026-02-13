package com.jworks.kanjilens.ui.paywall

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
import com.jworks.kanjilens.data.billing.BillingManager

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
        ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$2.99"
    val annualPrice = annualDetails?.subscriptionOfferDetails?.firstOrNull()
        ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$14.99"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1B1B1B),
                        Color(0xFF0D3B66)
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
                text = "Unlock Premium",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (remainingScans > 0) {
                Text(
                    text = "$remainingScans free scans remaining today",
                    fontSize = 14.sp,
                    color = Color(0xFFFFB74D)
                )
            } else {
                Text(
                    text = "You've used all 5 free scans today",
                    fontSize = 14.sp,
                    color = Color(0xFFFF5252)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Features list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureRow("Unlimited scans", "No daily limits or timers")
                FeatureRow("Offline dictionary", "215K+ entries, no internet needed")
                FeatureRow("Scan history", "Review past scans anytime")
                FeatureRow("Favorites", "Save words for later study")
                FeatureRow("J Coin rewards", "Earn coins with every scan")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Plan cards
            PlanCard(
                title = "Monthly",
                price = monthlyPrice,
                period = "/month",
                isSelected = selectedPlan == BillingManager.PRODUCT_MONTHLY,
                onClick = { selectedPlan = BillingManager.PRODUCT_MONTHLY }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PlanCard(
                title = "Annual",
                price = annualPrice,
                period = "/year",
                savings = "Save 37%",
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
                    containerColor = Color(0xFF4FC3F7)
                ),
                shape = RoundedCornerShape(28.dp),
                enabled = productDetails.isNotEmpty()
            ) {
                Text(
                    text = "Subscribe Now",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (productDetails.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Loading subscription options...",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dismiss
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (remainingScans > 0) "Continue with free tier" else "Maybe later",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Cancel anytime. Managed by Google Play.",
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
                    .background(Color(0xFF1A3A5C))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Bundle & Save",
                        color = Color(0xFFFFB74D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Get KanjiLens + KanjiQuest for \$5.99/mo (save \$1/mo)",
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
            color = Color(0xFF4FC3F7),
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
    val borderColor = if (isSelected) Color(0xFF4FC3F7) else Color.White.copy(alpha = 0.2f)
    val bgColor = if (isSelected) Color(0xFF1A3A5C) else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
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
                        color = Color(0xFF4CAF50),
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
