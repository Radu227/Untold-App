package com.untold.android.data.model

import com.google.gson.annotations.SerializedName

enum class DensityLevel {
    LOW, MODERATE, HIGH, CRITICAL
}

data class MapPinDto(
    val id: Long,
    val type: String, // e.g., "STAGE", "BAR", "FOOD_COURT", "CREDIT_POINT", "ENTRANCE", "TOILET", "FIRST_AID"
    val name: String,
    val description: String?,
    val xpos: Float,
    val ypos: Float
)

data class MapLayoutDto(
    val id: Long,
    val imageUrl: String?,
    val active: Boolean,
    val pins: List<MapPinDto>
)

data class ZoneDensityDto(
    val mapPinId: Long,
    val mapPinName: String,
    val mapPinType: String,
    val currentLevel: DensityLevel,
    val averageScore: Double,
    val reportCount: Int,
    val lastReportedAt: String?,
    val lastReportedBy: String?
)

data class EmergencyAlertRequest(
    val serviceType: String, // "POLICE", "AMBULANCE", "FIREFIGHTERS"
    val pinId: Long,
    val pinName: String
)

data class EmergencyAlertResponse(
    val id: Long,
    val serviceType: String,
    val pinId: Long,
    val pinName: String,
    val status: String,
    val createdAt: String
)
