package com.gymable.data.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.gymable.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:ApplicationScope private val appScope: CoroutineScope,
    private val prefsRepository: PrefsRepository
) : PurchasesUpdatedListener {

    companion object {
        private val PRODUCT_IDS = listOf("donation_ron_5", "donation_ron_10", "donation_ron_20")
        val PRODUCT_TIERS = mapOf(
            "donation_ron_5"  to 1,
            "donation_ron_10" to 2,
            "donation_ron_20" to 3
        )
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    private val _isBillingAvailable = MutableStateFlow(false)
    val isBillingAvailable: StateFlow<Boolean> = _isBillingAvailable.asStateFlow()

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private val _purchaseSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val purchaseSuccess: SharedFlow<Unit> = _purchaseSuccess.asSharedFlow()

    private val _purchaseError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val purchaseError: SharedFlow<String> = _purchaseError.asSharedFlow()

    init {
        connectBillingClient()
    }

    private fun connectBillingClient() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isBillingAvailable.value = true
                    appScope.launch { queryProducts() }
                }
            }

            override fun onBillingServiceDisconnected() {
                _isBillingAvailable.value = false
                connectBillingClient()
            }
        })
    }

    private fun queryProducts() {
        val productList = PRODUCT_IDS.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _products.value = queryProductDetailsResult.productDetailsList
                    .sortedBy { PRODUCT_IDS.indexOf(it.productId) }
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
            )
            .build()
        billingClient.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    ?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> { /* no-op */ }
            else -> {
                appScope.launch {
                    _purchaseError.emit("Purchase failed (${billingResult.responseCode})")
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.consumeAsync(consumeParams) { result, _ ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val tier = purchase.products.firstNotNullOfOrNull { PRODUCT_TIERS[it] }
                if (tier != null) prefsRepository.setMaxDonationTier(tier)
                appScope.launch { _purchaseSuccess.emit(Unit) }
            }
        }
    }
}
