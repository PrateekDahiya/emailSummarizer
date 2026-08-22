package com.gmailreader.service

import com.gmailreader.entity.Email
import com.gmailreader.entity.EmailClassification
import com.gmailreader.entity.Event
import com.gmailreader.entity.TravelTrip
import com.gmailreader.entity.User
import com.gmailreader.repository.EmailRepository
import com.gmailreader.repository.EventRepository
import com.gmailreader.repository.TravelTripRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
class EventService(
    private val emailRepository: EmailRepository,
    private val eventRepository: EventRepository,
    private val travelTripRepository: TravelTripRepository,
) {
    private val logger = LoggerFactory.getLogger(EventService::class.java)

    @Transactional
    fun extractEvents(gmailAccount: GmailAccount) {
        val user = gmailAccount.user!!
        val classifications = emailRepository.findByGmailAccount(gmailAccount)
            .filter { it.isProcessed }
            .mapNotNull { email ->
                email.classification?.let { classification ->
                    email to classification
                }
            }
            .filter { (_, classification) ->
                classification.category in setOf("MEETING", "INTERVIEW", "DEADLINE", "EVENT")
            }

        for ((email, classification) in classifications) {
            val entities = parseEntities(classification.entities)
            
            val eventTitle = entities["event_title"] as String? ?: email.subject
            val eventDateStr = entities["event_date"] as String?
            val eventTime = entities["event_time"] as String?
            val eventLocation = entities["event_location"] as String?
            val eventType = when (classification.category) {
                "INTERVIEW" -> "INTERVIEW"
                "MEETING" -> "MEETING"
                "DEADLINE" -> "DEADLINE"
                else -> "EVENT"
            }

            val startTime = parseEventDateTime(eventDateStr, eventTime, email.receivedAt)
            if (startTime == null) continue

            // Check if event already exists
            val existingEvent = eventRepository.findByUser(user)
                .firstOrNull { e ->
                    e.title == eventTitle &&
                    e.startTime != null &&
                    java.time.Duration.between(e.startTime!!, startTime).abs().toMinutes() < 30
                }

            if (existingEvent != null) continue

            // Find associated trip
            val trip = findAssociatedTrip(user, eventDateStr)

            val event = Event(
                user = user,
                trip = trip,
                type = eventType,
                title = eventTitle!!,
                description = classification.summary,
                location = eventLocation,
                startTime = startTime,
                endTime = startTime.plusHours(1),
                sourceEmail = email,
            )
            eventRepository.save(event)
        }
    }

    private fun parseEventDateTime(dateStr: String?, timeStr: String?, fallback: Instant): Instant? {
        val date = dateStr?.let { LocalDate.parse(it) } 
            ?: fallback.atZone(ZoneId.systemDefault()).toLocalDate()
        
        val time = timeStr?.let { 
            try {
                java.time.LocalTime.parse(it)
            } catch (e: Exception) {
                java.time.LocalTime.of(9, 0)
            }
        } ?: java.time.LocalTime.of(9, 0)

        return date.atTime(time).atZone(ZoneId.systemDefault()).toInstant()
    }

    private fun findAssociatedTrip(user: User, dateStr: String?): TravelTrip? {
        val date = dateStr?.let { LocalDate.parse(it) } ?: return null
        return travelTripRepository.findByUser(user)
            .firstOrNull { trip ->
                trip.startDate != null && trip.endDate != null &&
                !date.isBefore(trip.startDate!!) && !date.isAfter(trip.endDate!!)
            }
    }

    private fun parseEntities(entitiesJson: String?): Map<String, Any> {
        if (entitiesJson.isNullOrBlank()) return emptyMap()
        return try {
            com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().readValue<Map<String, Any>>(entitiesJson)
        } catch (e: Exception) {
            emptyMap()
        }
    }
}