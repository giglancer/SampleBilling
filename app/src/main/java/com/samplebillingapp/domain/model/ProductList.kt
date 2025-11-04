package com.samplebillingapp.domain.model

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.UserChoiceDetails

data class ProductList(
    val products: List<ProductDetails>
)
