package com.samplebillingapp.ui.billing.state

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.samplebillingapp.ui.billing.state.Status

data class BillingUiState(
    val status: Status,
    val products: List<ProductDetails> = emptyList(),
    val purchases: List<Purchase> = emptyList(),
    val pendingPurchases: List<Purchase> = emptyList(),
)

sealed interface Status {
    data object Loading: Status
    data object Success: Status
    data class Error(val message: String): Status
}