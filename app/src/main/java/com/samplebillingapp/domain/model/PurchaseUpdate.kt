package com.samplebillingapp.domain.model

import com.android.billingclient.api.Purchase

sealed class PurchaseUpdate {
    data class Success(val purchase: Purchase) : PurchaseUpdate()
    data class Failure(val errorCode: Int) : PurchaseUpdate()
    object UserCancelled : PurchaseUpdate()
}