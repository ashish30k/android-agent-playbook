package com.shopfast.checkout

import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun CheckoutScreen(viewModel: CheckoutViewModel, order: Order) {
    val state by viewModel.ui.collectAsState()
    when (val s = state) {
        is UiState.Success -> Text("Payment successful! Ref: " + s.receiptId)
        is UiState.Error -> Text(s.message)
        else -> {}
    }
    IconButton(onClick = { viewModel.pay(order) }) {
        Icon(PayIcon, contentDescription = null)
    }
}
