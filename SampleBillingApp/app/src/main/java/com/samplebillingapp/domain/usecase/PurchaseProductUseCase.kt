package com.samplebillingapp.domain.usecase

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import com.samplebillingapp.domain.repository.BillingRepository
import javax.inject.Inject

class PurchaseProductUseCase @Inject constructor(
    private val billingRepository: BillingRepository
) {
    suspend operator fun invoke(activity: Activity, productDetails: ProductDetails): Result<Unit> {
        return billingRepository.launchPurchaseFlow(activity, productDetails)
    }
}