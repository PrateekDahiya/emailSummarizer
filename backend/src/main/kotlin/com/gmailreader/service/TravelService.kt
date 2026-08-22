package com.gmailreader.service

import com.gmailreader.entity.Email
import com.gmailreader.entity.EmailClassification
import com.gmailreader.entity.Flight
import com.gmailreader.entity.Hotel
import com.gmailreader.entity.TravelTrip
import com.gmailreader.entity.User
import com.gmailreader.repository.EmailRepository
import com.gmailreader.repository.FlightRepository
import com.gmailreader.repository.HotelRepository
import com.gmailreader.repository.TravelTripRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
class TravelService(
    private val emailRepository: EmailRepository,
    private val travelTripRepository: TravelTripRepository,
    private val flightRepository: FlightRepository,
    private val hotelRepository: HotelRepository,
) {
    private val logger = LoggerFactory.getLogger(TravelService::class.java)

    @Transactional
    fun extractTravelInfo(gmailAccount: GmailAccount) {
        val user = gmailAccount.user!!
        val classifications = emailRepository.findByGmailAccount(gmailAccount)
            .filter { it.isProcessed }
            .mapNotNull { email ->
                email.classification?.let { classification ->
                    email to classification
                }
            }
            .filter { (_, classification) ->
                classification.category == "TRAVEL"
            }

        for ((email, classification) in classifications) {
            val entities = parseEntities(classification.entities)
            
            val airline = entities["airline"] as String?
            val flightNumber = entities["flight_number"] as String?
            val departure = entities["departure"] as String?
            val arrival = entities["arrival"] as String?
            val hotelName = entities["hotel"] as String?
            val bookingNumber = entities["booking_number"] as String?
            val travelDates = entities["travel_dates"] as List<String>?

            if (airline != null || flightNumber != null) {
                // Create or find trip
                var trip = findOrCreateTrip(user, email, entities)
                
                // Add flight
                if (flightNumber != null) {
                    val existingFlight = flightRepository.findByTrip(trip)
                        .firstOrNull { it.flightNumber == flightNumber }
                    
                    if (existingFlight == null) {
                        val flight = Flight(
                            trip = trip,
                            airline = airline,
                            flightNumber = flightNumber,
                            departureCity = departure,
                            arrivalCity = arrival,
                            bookingNumber = bookingNumber,
                            sourceEmail = email,
                        )
                        flightRepository.save(flight)
                    }
                }
            } else if (hotelName != null) {
                var trip = findOrCreateTrip(user, email, entities)
                
                val existingHotel = hotelRepository.findByTrip(trip)
                    .firstOrNull { it.name == hotelName }
                
                if (existingHotel == null) {
                    val hotel = Hotel(
                        trip = trip,
                        name = hotelName,
                        city = arrival ?: departure,
                        bookingNumber = bookingNumber,
                        sourceEmail = email,
                    )
                    hotelRepository.save(hotel)
                }
            }
        }
    }

    private fun findOrCreateTrip(user: User, email: Email, entities: Map<String, Any>): TravelTrip {
        val destination = (entities["arrival"] as String?) ?: (entities["departure"] as String?) ?: "Unknown"
        val travelDates = entities["travel_dates"] as List<String>?
        
        val startDate = travelDates?.firstOrNull()?.let { LocalDate.parse(it) } 
            ?: email.receivedAt.atZone(ZoneId.systemDefault()).toLocalDate()
        val endDate = travelDates?.lastOrNull()?.let { LocalDate.parse(it) } 
            ?: startDate.plusDays(3)

        // Try to find existing trip
        val existingTrips = travelTripRepository.findByUser(user)
        val trip = existingTrips.firstOrNull { t ->
            t.destination?.lowercase() == destination.lowercase() &&
            t.startDate != null &&
            java.time.temporal.ChronoUnit.DAYS.between(t.startDate!!, startDate).absoluteValue <= 7
        } ?: TravelTrip(
            user = user,
            name = "Trip to $destination",
            destination = destination,
            startDate = startDate,
            endDate = endDate,
            sourceEmailIds = arrayOf(email.id),
        )
        
        if (trip.id == null || trip.id == UUID.randomUUID()) {
            return travelTripRepository.save(trip)
        }
        
        // Update existing trip
        if (email.id !in (trip.sourceEmailIds ?: emptyArray())) {
            trip.sourceEmailIds = (trip.sourceEmailIds ?: emptyArray()) + email.id
        }
        trip.updatedAt = Instant.now()
        return travelTripRepository.save(trip)
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