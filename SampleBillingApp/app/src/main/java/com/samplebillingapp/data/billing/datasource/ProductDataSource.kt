package com.samplebillingapp.data.billing.datasource

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class ProductDataSource @Inject constructor(
    private val billingClientWrapper: BillingClientWrapper
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun queryProducts(
        productIds: List<String>,
        productType: String = BillingClient.ProductType.INAPP
    ): Result<List<ProductDetails>> {
        if (!billingClientWrapper.isReady) {
            return Result.failure(Exception("Billing client is not ready"))
        }
        return suspendCancellableCoroutine { continuation ->
            val productList = productIds.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(productType)
                    .build()
            }

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            billingClientWrapper.billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(
                        Result.success(queryProductDetailsResult.productDetailsList)
                    )
                } else {
                    continuation.resume(
                        Result.failure(Exception(billingResult.debugMessage))
                    )
                }
            }
        }
    }

    suspend fun queryPurchase(
        productType: String = BillingClient.ProductType.INAPP
    ): Result<List<Purchase>> {
        if (!billingClientWrapper.isReady) {
            return Result.failure(Exception("Billing client is not ready"))
        }

        return suspendCancellableCoroutine { continuation ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(productType)
                .build()
            billingClientWrapper.billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(Result.success(purchases))
                } else {
                    continuation.resume(
                        Result.failure(Exception(billingResult.debugMessage))
                    )
                }
            }
        }
    }
}