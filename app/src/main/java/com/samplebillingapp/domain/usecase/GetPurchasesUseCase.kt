package com.samplebillingapp.domain.usecase

import com.android.billingclient.api.Purchase
import com.samplebillingapp.domain.model.ProductList
import com.samplebillingapp.domain.repository.BillingRepository
import javax.inject.Inject


class GetPurchasesUseCase @Inject constructor(
    private val billingRepository: BillingRepository
) {
    suspend operator fun invoke(): Result<List<Purchase>> {
        return billingRepository.getPurchases()
    }
}