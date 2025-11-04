package com.samplebillingapp.domain.usecase

import com.android.billingclient.api.Purchase
import com.samplebillingapp.domain.repository.BillingRepository
import javax.inject.Inject


class ConsumePurchaseUseCase @Inject constructor(
    private val billingRepository: BillingRepository
) {
    suspend operator fun invoke(purchase: Purchase): Result<Unit> {
        return billingRepository.consumePurchase(purchase)
    }
}