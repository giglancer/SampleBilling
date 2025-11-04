package com.samplebillingapp.domain.usecase

import com.samplebillingapp.domain.repository.BillingRepository
import javax.inject.Inject

class InitializeBillingUseCase @Inject constructor(
    private val billingRepository: BillingRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return billingRepository.initialize()
    }
}