package com.untold.android.data.api

import com.untold.android.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- AUTHENTICATION ---
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AuthResponseDto>>

    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<AuthResponseDto>>

    // --- TICKETS ---
    @GET("api/tickets/user/{userId}")
    suspend fun getUserTickets(
        @Path("userId") userId: Long
    ): Response<ApiResponse<List<TicketDto>>>

    @POST("api/tickets/{ticketId}/check-in")
    suspend fun checkInTicket(
        @Path("ticketId") ticketId: Long,
        @Body details: PersonDto
    ): Response<TicketDto> // Note: Backend check-in endpoint returns the TicketDto directly (not wrapped in ApiResponse) or wrapper. Let's make it robust.

    // --- CASHLESS WALLET ---
    @POST("api/payment/create-topup-session")
    suspend fun createTopUpSession(
        @Body request: TopUpRequest
    ): Response<ApiResponse<SessionUrlDto>>

    @POST("api/payment/create-checkout-session")
    suspend fun createCheckoutSession(
        @Body request: CheckoutSessionRequest
    ): Response<ApiResponse<CheckoutResponse>>

    @POST("api/payment/confirm")
    suspend fun confirmPayment(
        @Body request: ConfirmPaymentRequest
    ): Response<ApiResponse<List<TicketDto>>>

    // --- VENUE MAP & CROWD DENSITY ---
    @GET("api/map/active")
    suspend fun getActiveMapLayout(): Response<ApiResponse<MapLayoutDto>>

    @GET("api/crowd-reports/aggregation")
    suspend fun getCrowdDensityAggregation(
        @Query("windowMinutes") windowMinutes: Int = 60
    ): Response<ApiResponse<List<ZoneDensityDto>>>

    @POST("api/alerts")
    suspend fun sendEmergencyAlert(
        @Body request: EmergencyAlertRequest
    ): Response<ApiResponse<EmergencyAlertResponse>>
}

