package com.gmailreader.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ClassificationResult(
    val category: String,
    val importance_score: Int,
    val summary: String,
    val action_required: Boolean,
    val action: String?,
    val deadline: String?,
    val confidence: Double,
    val entities: Entities,
)

data class Entities(
    val company: String? = null,
    val role: String? = null,
    val application_status: String? = null,
    val interview_date: String? = null,
    val airline: String? = null,
    val flight_number: String? = null,
    val departure: String? = null,
    val arrival: String? = null,
    val hotel: String? = null,
    val booking_number: String? = null,
    val travel_dates: List<String> = emptyList(),
    val event_title: String? = null,
    val event_date: String? = null,
    val event_time: String? = null,
    val event_location: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val transaction_type: String? = null,
    val people: List<String> = emptyList(),
)