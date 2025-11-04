package com.samplebillingapp.data.billing.repository

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.samplebillingapp.data.billing.datasource.BillingClientWrapper
import com.samplebillingapp.data.billing.datasource.ProductDataSource
import com.samplebillingapp.data.billing.datasource.PurchaseDataSource
import com.samplebillingapp.domain.model.BillingConnectionState
import com.samplebillingapp.domain.model.ProductList
import com.samplebillingapp.domain.model.PurchaseUpdate
import com.samplebillingapp.domain.repository.BillingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepositoryImpl @Inject constructor(
    private val billingClientWrapper: BillingClientWrapper,
    private val productDataSource: ProductDataSource,
    private val purchaseDataSource: PurchaseDataSource,
) : BillingRepository {
    private val _connectionState = MutableStateFlow<BillingConnectionState>(BillingConnectionState.Disconnected)
    override val connectionState: Flow<BillingConnectionState> = _connectionState.asStateFlow()

    override val purchaseUpdate: Flow<PurchaseUpdate> = purchaseDataSource.observePurchaseUpdates()

    override suspend fun initialize(): Result<Unit> {
        _connectionState.value = BillingConnectionState.Connecting

        return billingClientWrapper.connect()
            .onSuccess {
                _connectionState.value = BillingConnectionState.Connected
            }
            .onFailure {
                _connectionState.value = BillingConnectionState.Error(
                    it.message ?: "Unknown error"
                )
            }

    }

    override suspend fun getProducts(productIds: List<String>): Result<ProductList> {
        return productDataSource.queryProducts(productIds)
            .map { products ->
                ProductList(products = products)
            }
    }

    override suspend fun getPurchases(): Result<List<Purchase>> {
        return productDataSource.queryPurchase()
    }

    override suspend fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails
    ): Result<Unit> {
        return purchaseDataSource.launchPurchaseFlow(activity, productDetails)
    }

    override suspend fun consumePurchase(purchase: Purchase): Result<Unit> {
        return purchaseDataSource.consumePurchase(purchase)
    }

    override suspend fun acknowledgePurchase(purchase: Purchase): Result<Unit> {
        return purchaseDataSource.acknowledgePurchase(purchase)
    }

    override fun disconnect() {
        billingClientWrapper.disconnect()
        _connectionState.value = BillingConnectionState.Disconnected
    }
}