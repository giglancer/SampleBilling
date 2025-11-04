package com.samplebillingapp.data.billing.datasource

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.samplebillingapp.domain.model.PurchaseUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class PurchaseDataSource @Inject constructor(
    private val billingClientWrapper: BillingClientWrapper
) {
    fun observePurchaseUpdates(): Flow<PurchaseUpdate> = callbackFlow {
        val listener = PurchasesUpdatedListener { billingResult, purchases ->
             when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    if (purchases.isNullOrEmpty()) {
                        trySend(PurchaseUpdate.Failure(BillingClient.BillingResponseCode.ERROR))
                    } else {
                        purchases.forEach { purchase ->
                            trySend(PurchaseUpdate.Success(purchase))
                        }
                    }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    trySend(PurchaseUpdate.UserCancelled)
                }
                else -> {
                    trySend(PurchaseUpdate.Failure(billingResult.responseCode))
                }
            }
        }
        billingClientWrapper.initialize(listener)

        awaitClose {
            // 監視するためにFlowを終了させない
        }
    }

    // OSの購入フローを呼び出す
    suspend fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        if (!billingClientWrapper.isReady) {
            continuation.resume(Result.failure(Exception("Billing client is not ready")))
            return@suspendCancellableCoroutine
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClientWrapper.billingClient.launchBillingFlow(
            activity,
            billingFlowParams
        )

        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            continuation.resume(Result.success(Unit))
        } else {
            continuation.resume(
                Result.failure(Exception("Failed to launch billing flow: ${billingResult.debugMessage}"))
            )
        }
    }

    // 非消費可能アイテムの承認
    suspend fun acknowledgePurchase(purchase: Purchase): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!billingClientWrapper.isReady) {
                return@withContext Result.failure(Exception("Billing client not ready"))
            }

            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && purchase.isAcknowledged) {
                return@withContext Result.success(Unit)
            }

            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            val billingResult = billingClientWrapper.billingClient.acknowledgePurchase(
                acknowledgePurchaseParams
            )

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to acknowledge purchase: ${billingResult.debugMessage}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 消費可能アイテムの承認
    suspend fun consumePurchase(purchase: Purchase): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!billingClientWrapper.isReady) {
                return@withContext Result.failure(Exception("Billing client not ready"))
            }

            // 既に承認済みの場合はスキップ
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && purchase.isAcknowledged) {
                return@withContext Result.success(Unit)
            }

            val params = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            val consumeResult = billingClientWrapper.billingClient.consumePurchase(params)

            if (consumeResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Result.success(Unit)
            } else {
                Result.failure(
                    Exception("Failed to consume purchase: ${consumeResult.billingResult.debugMessage}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}