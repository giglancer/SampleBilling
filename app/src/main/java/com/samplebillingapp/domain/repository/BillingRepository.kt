package com.samplebillingapp.domain.repository

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.samplebillingapp.domain.model.BillingConnectionState
import com.samplebillingapp.domain.model.ProductList
import com.samplebillingapp.domain.model.PurchaseUpdate
import kotlinx.coroutines.flow.Flow

interface BillingRepository {
    val purchaseUpdate: Flow<PurchaseUpdate>
    val connectionState: Flow<BillingConnectionState>

    suspend fun initialize(): Result<Unit>
    suspend fun getProducts(productIds: List<String>): Result<ProductList>
    suspend fun getPurchases(): Result<List<Purchase>>

    suspend fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails): Result<Unit>

    suspend fun consumePurchase(purchase: Purchase): Result<Unit>

    suspend fun acknowledgePurchase(purchase: Purchase): Result<Unit>

    fun disconnect()
}