package com.gymable.ui.donation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.gymable.data.repository.BillingRepository
import com.gymable.data.repository.PrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DonationViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    private val prefsRepository: PrefsRepository
) : ViewModel() {

    val isBillingAvailable: StateFlow<Boolean> = billingRepository.isBillingAvailable
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val products: StateFlow<List<ProductDetails>> = billingRepository.products
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val maxDonationTier: StateFlow<Int> = prefsRepository.maxDonationTierFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _purchaseSuccess = MutableStateFlow(false)
    val purchaseSuccess: StateFlow<Boolean> = _purchaseSuccess.asStateFlow()

    private val _purchaseError = MutableStateFlow<String?>(null)
    val purchaseError: StateFlow<String?> = _purchaseError.asStateFlow()

    init {
        viewModelScope.launch {
            billingRepository.purchaseSuccess.collect {
                _purchaseSuccess.value = true
            }
        }
        viewModelScope.launch {
            billingRepository.purchaseError.collect { msg ->
                _purchaseError.value = msg
            }
        }
    }

    fun purchase(activity: Activity, product: ProductDetails) {
        billingRepository.launchBillingFlow(activity, product)
    }

    fun consumePurchaseSuccess() {
        _purchaseSuccess.update { false }
    }

    fun consumePurchaseError() {
        _purchaseError.update { null }
    }
}
