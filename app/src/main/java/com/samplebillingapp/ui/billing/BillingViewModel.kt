package com.samplebillingapp.ui.billing

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.samplebillingapp.domain.model.PurchaseUpdate
import com.samplebillingapp.domain.usecase.AcknowledgePurchaseUseCase
import com.samplebillingapp.domain.usecase.ConsumePurchaseUseCase
import com.samplebillingapp.domain.usecase.GetProductsUseCase
import com.samplebillingapp.domain.usecase.GetPurchasesUseCase
import com.samplebillingapp.domain.usecase.InitializeBillingUseCase
import com.samplebillingapp.domain.usecase.ObservePurchaseUpdatesUseCase
import com.samplebillingapp.domain.usecase.PurchaseProductUseCase
import com.samplebillingapp.ui.billing.state.BillingUiState
import com.samplebillingapp.ui.billing.state.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val initializeBillingUseCase: InitializeBillingUseCase,
    private val getProductsUseCase: GetProductsUseCase,
    private val getPurchasesUseCase: GetPurchasesUseCase,
    private val observePurchaseUpdatesUseCase: ObservePurchaseUpdatesUseCase,
    private val purchaseProductUseCase: PurchaseProductUseCase,
    private val acknowledgePurchaseUseCase: AcknowledgePurchaseUseCase,
    private val consumePurchaseUseCase: ConsumePurchaseUseCase,
) : ViewModel() {
    private val _billingUiState = MutableStateFlow(BillingUiState(status = Status.Loading))
    val billingUiState: StateFlow<BillingUiState> = _billingUiState.asStateFlow()

    val notConsumedPurchases: List<String>
        get() = listOf("test1", "test5")

    val consumedPurchases: List<String>
        get() = listOf("test2", "test3", "test4")


    init {
        observePurchaseUpdates()
        initializeBilling()
    }

    fun purchaseProduct(activity: Activity, productDetails: ProductDetails) {
        viewModelScope.launch {
            purchaseProductUseCase(activity, productDetails)
                .onSuccess {
                    Log.d("BillingViewModel", "Purchase flow launched successfully")
                    // 購入ダイアログが表示される
                    // 結果はobservePurchaseUpdatesで受け取る
                }
                .onFailure { error ->
                    Log.e("BillingViewModel", "Failed to launch purchase flow: ${error.message}")
                    _billingUiState.value =
                        _billingUiState.value.copy(status = Status.Error("購入のフローの起動に失敗しました"))
                }
        }
    }

    private fun observePurchaseUpdates() {
        viewModelScope.launch {
            observePurchaseUpdatesUseCase().collect { update ->
                val currentState = _billingUiState.value.status
                if (currentState !is Status.Success) return@collect

                when (update) {
                    is PurchaseUpdate.Failure -> {
                        Log.d("BillingViewModel", "Purchase failed")
                        _billingUiState.value =
                            _billingUiState.value.copy(status = Status.Error("購入に失敗しました"))
                    }

                    is PurchaseUpdate.Success -> {
                        Log.d("BillingViewModel", "Purchase success:")
                        handleSuccessfulPurchase(update.purchase)
                    }

                    PurchaseUpdate.UserCancelled -> {
                        Log.d("BillingViewModel", "Purchase cancelled")
                        _billingUiState.value =
                            _billingUiState.value.copy(status = Status.Error("購入がキャンセルされました"))
                    }
                }
            }
        }
    }

    private fun initializeBilling() {
        viewModelScope.launch {
            _billingUiState.value = BillingUiState(status = Status.Loading)

            initializeBillingUseCase()
                .onSuccess {
                    Log.d("BillingViewModel", "Billing client connected")
                    loadPurchases()
                    loadProducts()
                }
                .onFailure {
                    Log.d("BillingViewModel", "Billing client connection failed")
                    _billingUiState.value =
                        _billingUiState.value.copy(status = Status.Error("Billingクライアントの接続に失敗しました"))
                }
        }
    }

    private fun handleSuccessfulPurchase(purchase: Purchase) {
        viewModelScope.launch {
            // 購入に含まれる商品のタイプをチェック
            val hasNonConsumable = purchase.products.any { it in notConsumedPurchases }
            val hasConsumable = purchase.products.any { it in consumedPurchases }

            // 購入を承認
            when {
                // 非消費型アイテム
                hasNonConsumable && !hasConsumable -> {
                    acknowledgePurchaseUseCase(purchase)
                        .onSuccess {
                            Log.d("BillingViewModel", "Purchase acknowledged")
                            _billingUiState.value = _billingUiState.value.copy(
                                status = Status.Success,
                                purchases = _billingUiState.value.purchases + purchase,
                                pendingPurchases = _billingUiState.value.pendingPurchases.filter { current ->
                                    current.purchaseToken != purchase.purchaseToken
                                }
                            )
                        }
                        .onFailure { error ->
                            Log.e("BillingViewModel", "Acknowledge failed: ${error.message}")
                            _billingUiState.value =
                                _billingUiState.value.copy(status = Status.Error("購入の承認に失敗しました"))
                        }
                }
                // 消費型アイテム
                !hasNonConsumable && hasConsumable -> {
                    consumePurchaseUseCase(purchase)
                        .onSuccess {
                            Log.d("BillingViewModel", "Purchase consumed")
                            _billingUiState.value = _billingUiState.value.copy(
                                status = Status.Success,
                                pendingPurchases = _billingUiState.value.pendingPurchases.filter { current ->
                                    current.purchaseToken != purchase.purchaseToken
                                }
                            )
                        }
                        .onFailure {
                            Log.e("BillingViewModel", "Consume failed: ${it.message}")
                            _billingUiState.value = _billingUiState.value.copy(status = Status.Error("購入の承認に失敗しました"))
                        }
                }
                else -> {
                    Log.e("BillingViewModel", "Unknown product types: ${purchase.products}")
                    _billingUiState.value = _billingUiState.value.copy(
                        status = Status.Error("不明な商品が含まれています")
                    )
                }
            }
            loadPurchases()
        }
    }

    private fun loadPurchases() {
        viewModelScope.launch {
            getPurchasesUseCase()
                .onSuccess { purchases ->
                    Log.d("BillingViewModel", "Purchases loaded: $purchases")
                    _billingUiState.value = _billingUiState.value.copy(
                        status = Status.Success,
                        purchases = purchases.filter { it.isAcknowledged || it.purchaseState == Purchase.PurchaseState.PURCHASED },
                        pendingPurchases = purchases.filter { it.purchaseState == Purchase.PurchaseState.PENDING }
                    )
                }
                .onFailure {
                    // 購入履歴の取得失敗はエラー状態にしない（商品表示は継続）
                    Log.d("BillingViewModel", "Failed to load purchases: ${it.message}")
                }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            val productIds = notConsumedPurchases + consumedPurchases
            getProductsUseCase(productIds)
                .onSuccess {
                    Log.d("BillingViewModel", "Products loaded: ${it.products}")
                    _billingUiState.value = _billingUiState.value.copy(
                        status = Status.Success,
                        products = it.products,
                    )
                }
                .onFailure {
                    Log.d("BillingViewModel", "Failed to load products: ${it.message}")
                    _billingUiState.value =
                        _billingUiState.value.copy(status = Status.Error("商品の読み込みに失敗しました"))
                }
        }
    }

    fun retry() {
        initializeBilling()
    }

    fun isPurchased(productId: String): Boolean {
        return _billingUiState.value.purchases.any { purchase ->
            purchase.products.contains(productId)
        }
    }
}