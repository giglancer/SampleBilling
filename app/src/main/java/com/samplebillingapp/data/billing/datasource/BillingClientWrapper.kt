package com.samplebillingapp.data.billing.datasource

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BillingClientWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var _billingClient: BillingClient? = null
    val billingClient: BillingClient
        get() = _billingClient ?: throw IllegalStateException("Billing client is not initialized.")

    val isReady: Boolean
        get() = _billingClient?.isReady == true

    fun initialize(purchasesUpdatedListener: PurchasesUpdatedListener) {
        _billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()
    }

    suspend fun connect(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        if (_billingClient == null) {
            continuation.resume(Result.failure(Exception("BillingClient not initialized")))
            return@suspendCancellableCoroutine
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                // 自動再接続が有効
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(Result.success(Unit))
                } else {
                    continuation.resume(Result.failure(Exception(billingResult.debugMessage)))
                }
            }
        })
    }

    fun disconnect() {
        _billingClient?.endConnection()
        _billingClient = null
    }

    fun isInitialized(): Boolean = _billingClient != null
}