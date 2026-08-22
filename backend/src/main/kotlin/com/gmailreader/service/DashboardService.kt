package com.gmailreader.service

import com.gmailreader.dto.DashboardResponse
import com.gmailreader.dto.AttentionItem
import com.gmailreader.dto.UpcomingItem
import com.gmailreader.dto.RecentEmail
import com.gmailreader.dto.JobApplicationSummary
import com.gmailreader.dto.TravelTripSummary
import com.gmailreader.dto.FlightSummary
import com.gmailreader.dto.HotelSummary
import com.gmailreader.entity.Email
import com.gmailreader.entity.EmailClassification
import com.gmailreader.entity.Event
import com.gmailreader.entity.Flight
import com.gmailreader.entity.GmailAccount
import com.gmailreader.entity.Hotel
import com.gmailreader.entity.JobApplication
import com.gmailreader.entity.TravelTrip
import com.gmailreader.entity.User
import com.gmailreader.repository.EmailRepository
import com.gmailreader.repository.EventRepository
import com.gmailreader.repository.JobApplicationRepository
import com.gmailreader.repository.TravelTripRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
class DashboardService(
    private val emailRepository: EmailRepository,
    private val eventRepository: EventRepository,
    private val jobApplicationRepository: JobApplicationRepository,
    private val travelTripRepository: TravelTripRepository,
) {

    @Transactional(readOnly = true)
    fun getDashboard(user: User): DashboardResponse {
        val gmailAccount = user.gmailAccounts?.firstOrNull() ?: return emptyDashboard()

        val needsAttention = buildNeedsAttention(gmailAccount)
        val upcoming = buildUpcoming(gmailAccount, user)
        val recentImportant = buildRecentImportant(gmailAccount)
        val jobApplications = buildJobApplications(user)
        val upcomingTrips = buildUpcomingTrips(user)

        return DashboardResponse(
            needsAttention = needsAttention,
            upcoming = upcoming,
            recentImportant = recentImportant,
            jobApplications = jobApplications,
            upcomingTrips = upcomingTrips,
        )
    }

    private fun emptyDashboard(): DashboardResponse {
        return DashboardResponse(
            needsAttention = emptyList(),
            upcoming = emptyList(),
            recentImportant = emptyList(),
            jobApplications = emptyList(),
            upcomingTrips = emptyList(),
        )
    }

    private fun buildNeedsAttention(gmailAccount: GmailAccount): List<AttentionItem> {
        val classifications = emailRepository.findByGmailAccount(gmailAccount)
            .filter { it.isProcessed }
            .mapNotNull { it.classification }
            .filter { it.importanceScore >= 70 && it.actionRequired }
            .sortedByDescending { it.importanceScore }
            .take(5)

        return classifications.mapIndexed { index, classification ->
            val email = classification.email!!
            AttentionItem(
                id = UUID.randomUUID(),
                type = mapCategoryToAttentionType(classification.category),
                title = classification.summary ?: email.subject ?: "Important email",
                description = classification.action ?: "Action required",
                date = classification.deadline ?: email.receivedAt.plusDays(1),
                time = classification.deadline?.let { it.atZone(ZoneId.systemDefault()).toLocalTime().toString() },
                priority = when {
                    classification.importanceScore >= 90 -> "high"
                    classification.importanceScore >= 70 -> "medium"
                    else -> "low"
                },
                sourceEmailId = email.id,
            )
        }
    }

    private fun buildUpcoming(gmailAccount: GmailAccount, user: User): List<UpcomingItem> {
        val items = mutableListOf<UpcomingItem>()
        val now = Instant.now()

        // Events
        val events = eventRepository.findUpcomingEventsByUser(user, now).take(5)
        items.addAll(events.map { event ->
            UpcomingItem(
                id = event.id,
                type = mapEventTypeToUpcomingType(event.type),
                title = event.title,
                date = event.startTime,
                time = event.startTime.atZone(ZoneId.systemDefault()).toLocalTime().toString(),
                location = event.location,
                sourceEmailId = event.sourceEmail?.id ?: UUID.randomUUID(),
            )
        })

        // Job interviews
        val interviews = jobApplicationRepository.findByUser(user)
            .filter { it.status == "INTERVIEW" && it.interviewDate != null && it.interviewDate!! > now }
            .sortedBy { it.interviewDate }
            .take(3)
        
        items.addAll(interviews.map { app ->
            UpcomingItem(
                id = UUID.randomUUID(),
                type = "interview",
                title = "Interview at ${app.company}",
                date = app.interviewDate!!,
                time = app.interviewDate!!.atZone(ZoneId.systemDefault()).toLocalTime().toString(),
                location = null,
                sourceEmailId = app.sourceEmailIds?.firstOrNull() ?: UUID.randomUUID(),
            )
        })

        // Flights from trips
        val trips = travelTripRepository.findUpcomingTripsByUser(user, LocalDate.now()).take(2)
        items.addAll(trips.flatMap { trip ->
            trip.flights?.take(1).map { flight ->
                UpcomingItem(
                    id = flight.id,
                    type = "flight",
                    title = "${flight.airline} ${flight.flightNumber}",
                    date = flight.departureTime!!,
                    time = flight.departureTime!!.atZone(ZoneId.systemDefault()).toLocalTime().toString(),
                    location = "${flight.departureCity} → ${flight.arrivalCity}",
                    sourceEmailId = flight.sourceEmail?.id ?: UUID.randomUUID(),
                )
            }
        })

        return items.sortedBy { it.date }.take(10)
    }

    private fun buildRecentImportant(gmailAccount: GmailAccount): List<RecentEmail> {
        val classifications = emailRepository.findByGmailAccount(gmailAccount)
            .filter { it.isProcessed }
            .mapNotNull { it.classification }
            .filter { it.importanceScore >= 50 }
            .sortedByDescending { it.importanceScore }
            .take(10)

        return classifications.map { classification ->
            val email = classification.email!!
            RecentEmail(
                id = email.id,
                sender = email.sender ?: email.senderEmail ?: "Unknown",
                subject = email.subject ?: "No subject",
                category = classification.category,
                importanceScore = classification.importanceScore,
                receivedAt = email.receivedAt,
                summary = classification.summary ?: email.snippet ?: "",
                actionRequired = classification.actionRequired,
            )
        }
    }

    private fun buildJobApplications(user: User): List<JobApplicationSummary> {
        return jobApplicationRepository.findByUserOrderByUpdatedAtDesc(user)
            .map { app ->
                JobApplicationSummary(
                    id = app.id,
                    company = app.company,
                    role = app.role,
                    status = app.status,
                    appliedAt = app.appliedAt,
                    interviewDate = app.interviewDate,
                    recruiterName = app.recruiterName,
                )
            }
    }

    private fun buildUpcomingTrips(user: User): List<TravelTripSummary> {
        return travelTripRepository.findUpcomingTripsByUser(user, LocalDate.now())
            .map { trip ->
                TravelTripSummary(
                    id = trip.id,
                    name = trip.name,
                    destination = trip.destination,
                    startDate = trip.startDate?.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    endDate = trip.endDate?.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    flights = trip.flights?.map { flight ->
                        FlightSummary(
                            id = flight.id,
                            airline = flight.airline,
                            flightNumber = flight.flightNumber,
                            departure = flight.departureCity,
                            arrival = flight.arrivalCity,
                            departureTime = flight.departureTime,
                            arrivalTime = flight.arrivalTime,
                            bookingNumber = flight.bookingNumber,
                        )
                    } ?: emptyList(),
                    hotels = trip.hotels?.map { hotel ->
                        HotelSummary(
                            id = hotel.id,
                            name = hotel.name,
                            checkInDate = hotel.checkInDate?.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                            checkOutDate = hotel.checkOutDate?.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                            bookingNumber = hotel.bookingNumber,
                            cost = hotel.cost?.toDouble(),
                        )
                    } ?: emptyList(),
                    totalCost = trip.totalCost?.toDouble(),
                )
            }
    }

    private fun mapCategoryToAttentionType(category: String): String {
        return when (category) {
            "INTERVIEW" -> "interview"
            "DEADLINE" -> "deadline"
            "DOCUMENT" -> "document"
            "MEETING" -> "meeting"
            "FINANCE" -> "payment"
            "TRAVEL" -> "travel"
            else -> "deadline"
        }
    }

    private fun mapEventTypeToUpcomingType(type: String): String {
        return when (type) {
            "INTERVIEW" -> "interview"
            "MEETING" -> "meeting"
            "DEADLINE" -> "deadline"
            "EVENT" -> "event"
            else -> "event"
        }
    }
}