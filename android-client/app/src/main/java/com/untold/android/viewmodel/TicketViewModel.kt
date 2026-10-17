package com.untold.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.untold.android.data.api.ApiClient
import com.untold.android.data.local.TokenManager
import com.untold.android.data.model.PersonDto
import com.untold.android.data.model.TicketDto
import com.untold.android.data.model.TopUpRequest
import com.untold.android.data.model.TicketItemRequest
import com.untold.android.data.model.CheckoutSessionRequest
import com.untold.android.data.model.ConfirmPaymentRequest
import com.untold.android.ui.screens.CartItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class TicketState {
    object Loading : TicketState()
    data class Success(val tickets: List<TicketDto>) : TicketState()
    data class Error(val message: String) : TicketState()
}

sealed class ActionState {
    object Idle : ActionState()
    object Loading : ActionState()
    data class Success(val payload: String? = null) : ActionState()
    data class Error(val message: String) : ActionState()
}

class TicketViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenManager = TokenManager(application)
    private val apiService = ApiClient.getApiService(application)

    private val _ticketState = MutableStateFlow<TicketState>(TicketState.Loading)
    val ticketState: StateFlow<TicketState> = _ticketState

    private val _checkInState = MutableStateFlow<ActionState>(ActionState.Idle)
    val checkInState: StateFlow<ActionState> = _checkInState

    private val _topUpState = MutableStateFlow<ActionState>(ActionState.Idle)
    val topUpState: StateFlow<ActionState> = _topUpState

    fun loadTickets() {
        val userId = tokenManager.getUserId()
        if (userId == -1L) {
            _ticketState.value = TicketState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            _ticketState.value = TicketState.Loading
            try {
                val response = apiService.getUserTickets(userId)
                if (response.isSuccessful && response.body()?.success == true) {
                    _ticketState.value = TicketState.Success(response.body()!!.data)
                } else {
                    _ticketState.value = TicketState.Error(response.body()?.message ?: "Failed to retrieve tickets")
                }
            } catch (e: Exception) {
                _ticketState.value = TicketState.Error("Network error: ${e.localizedMessage ?: "Failed to reach server"}")
            }
        }
    }

    fun checkInTicket(ticketId: Long, person: PersonDto) {
        viewModelScope.launch {
            _checkInState.value = ActionState.Loading
            try {
                val response = apiService.checkInTicket(ticketId, person)
                if (response.isSuccessful) {
                    // If we get back the ticket successfully, trigger a reload of all tickets and mark action as Success
                    loadTickets()
                    _checkInState.value = ActionState.Success()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Bad Request or Validation Error"
                    // Parse if it has a nested message or use direct text
                    _checkInState.value = ActionState.Error("Check-in failed: $errorBody")
                }
            } catch (e: Exception) {
                _checkInState.value = ActionState.Error("Check-in failed: ${e.localizedMessage ?: "Could not connect to server"}")
            }
        }
    }

    fun initiateTopUp(ticketId: Long, amount: Double) {
        viewModelScope.launch {
            _topUpState.value = ActionState.Loading
            try {
                val response = apiService.createTopUpSession(TopUpRequest(ticketId, amount))
                if (response.isSuccessful && response.body()?.success == true) {
                    val url = response.body()!!.data.url
                    _topUpState.value = ActionState.Success(url)
                } else {
                    _topUpState.value = ActionState.Error(response.body()?.message ?: "Failed to create checkout session")
                }
            } catch (e: Exception) {
                _topUpState.value = ActionState.Error("Network error: ${e.localizedMessage ?: "Could not contact server"}")
            }
        }
    }

    fun initiateCheckout(cartItems: List<CartItem>, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val userId = tokenManager.getUserId()
        if (userId == -1L) {
            onError("User not logged in")
            return
        }

        viewModelScope.launch {
            try {
                val tickets = cartItems.flatMap { item ->
                    List(item.quantity) {
                        TicketItemRequest(
                            ticketType = mapOfferIdToBackendTicketType(item.offer.id, item.day),
                            price = item.offer.price
                        )
                    }
                }

                val response = apiService.createCheckoutSession(CheckoutSessionRequest(userId, tickets))
                if (response.isSuccessful && response.body()?.success == true) {
                    val url = response.body()!!.data.url
                    onSuccess(url)
                } else {
                    val errMsg = response.body()?.message ?: "Failed to initiate checkout session"
                    onError(errMsg)
                }
            } catch (e: Exception) {
                onError("Network error: ${e.localizedMessage ?: "Failed to contact server"}")
            }
        }
    }

    private fun mapOfferIdToBackendTicketType(offerId: String, day: Int?): String {
        return when (offerId) {
            "GA_4_DAYS_PHASE_5" -> "GA_4_DAYS_PHASE_5"
            "VIP_4_DAYS_PHASE_5" -> "VIP_MAINSTAGE_4_DAYS_PHASE_3"
            "GA_SINGLE_DAY" -> {
                when (day ?: 1) {
                    1 -> "GA_DAY_1_PHASE_2"
                    2 -> "GA_DAY_2_PHASE_2"
                    3 -> "GA_DAY_3_PHASE_2"
                    4 -> "GA_DAY_4_PHASE_2"
                    else -> "GA_DAY_1_PHASE_2"
                }
            }
            "VIP_SINGLE_DAY" -> {
                when (day ?: 1) {
                    1 -> "VIP_MAINSTAGE_DAY_1_PHASE_2"
                    2 -> "VIP_MAINSTAGE_DAY_2_PHASE_2"
                    3 -> "VIP_MAINSTAGE_DAY_3_PHASE_2"
                    4 -> "VIP_MAINSTAGE_DAY_4_PHASE_2"
                    else -> "VIP_MAINSTAGE_DAY_1_PHASE_2"
                }
            }
            else -> offerId
        }
    }

    fun confirmPayment(sessionId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = apiService.confirmPayment(ConfirmPaymentRequest(sessionId))
                if (response.isSuccessful && response.body()?.success == true) {
                    loadTickets() // Reload updated tickets & balance automatically!
                    onResult(true, null)
                } else {
                    onResult(false, response.body()?.message ?: "Failed to confirm payment")
                }
            } catch (e: Exception) {
                onResult(false, "Network error: ${e.localizedMessage ?: "Failed to connect to server"}")
            }
        }
    }

    fun resetActionStates() {
        _checkInState.value = ActionState.Idle
        _topUpState.value = ActionState.Idle
    }

    fun getUserId(): Long = tokenManager.getUserId()
}

