package com.samplebillingapp.domain.usecase

import com.samplebillingapp.domain.model.ProductList
import com.samplebillingapp.domain.repository.BillingRepository
import javax.inject.Inject

class GetProductsUseCase @Inject constructor(
    private val billingRepository: BillingRepository
) {
    suspend operator fun invoke(productIds: List<String>): Result<ProductList> {
        return billingRepository.getProducts(productIds)
    }
}