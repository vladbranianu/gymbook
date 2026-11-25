package com.gymable.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.billingclient.api.ProductDetails
import com.gymable.ui.components.PrimaryButton
import com.gymable.ui.components.ScreenScaffold
import com.gymable.ui.donation.DonationViewModel
import com.gymable.ui.theme.CardBorder
import com.gymable.ui.theme.RecordGold
import com.gymable.ui.theme.TextSecondary

private data class DonationTierInfo(
    val productId: String,
    val emoji: String,
    val label: String,
    val badge: String,
    val colorsUnlocked: String,
    val tier: Int
)

private val TIERS = listOf(
    DonationTierInfo("donation_ron_5",  "☕", "One Scoop",      "Supporter", "Unlock 2 premium colors", 1),
    DonationTierInfo("donation_ron_10", "🧴", "Gym Chalk",      "Gym Rat",   "Unlock 4 premium colors", 2),
    DonationTierInfo("donation_ron_20", "🏆", "Recovery Shake", "Legend",    "Unlock all 6 premium colors", 3)
)

@Composable
fun DonationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DonationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isBillingAvailable by viewModel.isBillingAvailable.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val purchaseSuccess by viewModel.purchaseSuccess.collectAsStateWithLifecycle()
    val purchaseError by viewModel.purchaseError.collectAsStateWithLifecycle()
    val maxDonationTier by viewModel.maxDonationTier.collectAsStateWithLifecycle()

    if (purchaseSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.consumePurchaseSuccess() },
            title = { Text("Thank you!") },
            text = {
                Text(
                    "Your support means a lot. New colors have been unlocked in Color Settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.consumePurchaseSuccess()
                    onBack()
                }) { Text("Awesome!") }
            }
        )
    }

    purchaseError?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.consumePurchaseError() },
            title = { Text("Purchase Failed") },
            text = { Text(error, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { viewModel.consumePurchaseError() }) { Text("OK") }
            }
        )
    }

    ScreenScaffold(
        title = "Support the Developer",
        onBack = onBack,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Gymbook is free and will always stay free. If you find it useful, a small donation helps keep development going — and unlocks premium accent colors as a thank-you.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 22.sp
            )

            if (!isBillingAvailable) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "In-app purchases are not available on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                TIERS.forEach { tier ->
                    val productDetails = products.find { it.productId == tier.productId }
                    val alreadyOwned = maxDonationTier >= tier.tier
                    DonationCard(
                        tier = tier,
                        productDetails = productDetails,
                        alreadyOwned = alreadyOwned,
                        onBuy = {
                            if (productDetails != null) {
                                val activity = context as? Activity ?: return@DonationCard
                                viewModel.purchase(activity, productDetails)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DonationCard(
    tier: DonationTierInfo,
    productDetails: ProductDetails?,
    alreadyOwned: Boolean,
    onBuy: () -> Unit
) {
    val price = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice ?: "—"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (alreadyOwned) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else CardBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tier.emoji,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = tier.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tier.badge,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = RecordGold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tier.colorsUnlocked,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Text(
                    text = price,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (alreadyOwned) {
                Text(
                    text = "You've already supported at this tier or higher. Thank you!",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                PrimaryButton(
                    text = "Donate $price",
                    onClick = onBuy,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
