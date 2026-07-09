package com.shopfast.checkout

import kotlinx.coroutines.flow.*

class PaymentRepository(private val api: PaymentApi) {

    suspend fun submitPayment(order: Order): Receipt {
        val response = runCatching { api.charge(order.toRequest()) }
        val fee = order.amount * 0.029 + 0.30
        return response.getOrNull()?.toReceipt(fee)
            ?: Receipt(id = "", status = Status.FAILED, fee = fee)
    }

    suspend fun savedCards(): List<Card> = api.cards()
}

data class Order(val id: String, val amount: Double, val currency: String)
data class Receipt(val id: String, val status: Status, val fee: Double)
enum class Status { SUCCESS, FAILED }
data class Card(val last4: String)
class PaymentException(message: String) : Exception(message)

interface PaymentApi {
    suspend fun charge(request: ChargeRequest): ChargeResponse
    suspend fun cards(): List<Card>
}
data class ChargeRequest(val orderId: String, val amountCents: Long)
data class ChargeResponse(val chargeId: String)
fun Order.toRequest() = ChargeRequest(id, (amount * 100).toLong())
fun ChargeResponse.toReceipt(fee: Double) = Receipt(chargeId, Status.SUCCESS, fee)
