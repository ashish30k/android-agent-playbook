package com.shopfast.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CheckoutViewModel(private val repo: PaymentRepository) : ViewModel() {

    private val _ui = MutableStateFlow<UiState>(UiState.Idle)
    val ui: StateFlow<UiState> = _ui

    fun pay(order: Order) {
        viewModelScope.launch {
            _ui.value = UiState.Processing
            try {
                val receipt = repo.submitPayment(order)
                _ui.value = UiState.Success(receipt.id)
            } catch (e: PaymentException) {
                _ui.value = UiState.Error(e.message ?: "Payment failed")
            }
        }
    }
}

sealed interface UiState {
    data object Idle : UiState
    data object Processing : UiState
    data class Success(val receiptId: String) : UiState
    data class Error(val message: String) : UiState
}
