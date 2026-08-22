package com.gmailreader.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.UUID

data class DashboardResponse(
    val needsAttention: List<AttentionItem>,
    val upcoming: List<UpcomingItem>,
    val recentImportant: List<RecentEmail>,
    val jobApplications: List<JobApplicationSummary>,
    val upcomingTrips: List<TravelTripSummary>,
)

data class AttentionItem(
    val id: UUID,
    val type: String,
    val title: String,
    val description: String,
    val date: Instant,
    val time: String?,
    val priority: String,
    val sourceEmailId: UUID,
    val actionUrl: String? = null,
)

data class UpcomingItem(
    val id: UUID,
    val type: String,
    val title: String,
    val date: Instant,
    val time: String?,
    val location: String?,
    val sourceEmailId: UUID,
)

data class RecentEmail(
    val id: UUID,
    val sender: String,
    val subject: String,
    val category: String,
    val importanceScore: Int,
    val receivedAt: Instant,
    val summary: String,
    val actionRequired: Boolean,
)

data class JobApplicationSummary(
    val id: UUID,
    val company: String,
    val role: String?,
    val status: String,
    val appliedAt: Instant?,
    val interviewDate: Instant?,
    val recruiterName: String?,
)

data class TravelTripSummary(
    val id: UUID,
    val name: String?,
    val destination: String?,
    val startDate: Instant?,
    val endDate: Instant?,
    val flights: List<FlightSummary>,
    val hotels: List<HotelSummary>,
    val totalCost: Double?,
)

data class FlightSummary(
    val id: UUID,
    val airline: String?,
    val flightNumber: String?,
    val departure: String?,
    val arrival: String?,
    val departureTime: Instant?,
    val arrivalTime: Instant?,
    val bookingNumber: String?,
)

data class HotelSummary(
    val id: UUID,
    val name: String?,
    val checkInDate: Instant?,
    val checkOutDate: Instant?,
    val bookingNumber: String?,
    val cost: Double?,
)