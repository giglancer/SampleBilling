package com.samplebillingapp.domain.usecase

import com.samplebillingapp.domain.model.PurchaseUpdate
import com.samplebillingapp.domain.repository.BillingRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObservePurchaseUpdatesUseCase @Inject constructor(
    private val billingRepository: BillingRepository
) {
    operator fun invoke(): Flow<PurchaseUpdate> {
        return billingRepository.purchaseUpdate
    }
}