package com.untold.android.data.model

import com.google.gson.annotations.SerializedName

// Generic API Response Wrapper
data class ApiResponse<T>(
    val type: String,
    val message: String?,
    val data: T
) {
    val success: Boolean
        get() = type == "SUCCESS"
}

// Auth Request & Response
data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val userType: String = "ATTENDEE"
)

data class UserDto(
    val id: Long,
    val username: String,
    val email: String,
    val roles: List<String>
)

data class AuthResponseDto(
    val token: String,
    val user: UserDto
)

// Person Details
data class PersonDto(
    val id: Long? = null,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String, // YYYY-MM-DD
    val gender: String,
    val country: String,
    val city: String,
    val documentType: String,
    val documentId: String,
    val phoneNumber: String
)

// Cashless RFID Bracelet
data class BraceletDto(
    val id: Long,
    val rfidTag: String,
    val balance: Double
)

// Ticket Details
data class TicketDto(
    val id: Long,
    val ticketType: String,
    val displayName: String,
    val price: Double,
    val purchaseDate: String,
    val checkInCompleted: Boolean,
    val attendee: PersonDto?,
    val bracelet: BraceletDto?,
    val rfidTag: String?
)

// Checkout / Top Up Request
data class TopUpRequest(
    val ticketId: Long,
    val amount: Double
)

// Top Up Response Session
data class SessionUrlDto(
    val url: String
)

// Checkout / Purchase Request & Response
data class TicketItemRequest(
    val ticketType: String,
    val price: Double
)

data class CheckoutSessionRequest(
    val userId: Long,
    val tickets: List<TicketItemRequest>
)

data class CheckoutResponse(
    val url: String
)

// Payment confirmation request
data class ConfirmPaymentRequest(
    val sessionId: String
)

// Announcement details
data class Announcement(
    val id: Long,
    val title: String,
    val content: String,
    val targetAudience: String,
    val createdAt: String
)

