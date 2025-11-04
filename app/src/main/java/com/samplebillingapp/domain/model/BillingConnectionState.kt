package com.samplebillingapp.domain.model

sealed class BillingConnectionState {
    object Connecting : BillingConnectionState()
    object Connected : BillingConnectionState()
    object Disconnected : BillingConnectionState()
    data class Error(val message: String) : BillingConnectionState()
}