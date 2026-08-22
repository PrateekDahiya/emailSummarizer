package com.gmailreader.controller

import com.gmailreader.dto.TravelTripSummary
import com.gmailreader.service.AuthService
import com.gmailreader.entity.TravelTrip
import com.gmailreader.entity.User
import com.gmailreader.repository.TravelTripRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/travel")
class TravelController(
    private val authService: AuthService,
    private val travelTripRepository: TravelTripRepository,
) {

    @GetMapping("/trips")
    fun getTrips(
        @RequestHeader("Authorization") authHeader: String,
    ): ResponseEntity<List<TravelTripSummary>> {
        val token = authHeader.removePrefix("Bearer ")
        val user = authService.getUserFromToken(token)
            ?: return ResponseEntity.status(401).build()

        val trips = travelTripRepository.findByUserOrderByStartDateDesc(user)

        return ResponseEntity.ok(trips.map { toSummary(it) })
    }

    private fun toSummary(trip: TravelTrip): TravelTripSummary {
        return TravelTripSummary(
            id = trip.id,
            name = trip.name,
            destination = trip.destination,
            startDate = trip.startDate?.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant(),
            endDate = trip.endDate?.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant(),
            flights = trip.flights?.map { flight ->
                com.gmailreader.dto.FlightSummary(
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
                com.gmailreader.dto.HotelSummary(
                    id = hotel.id,
                    name = hotel.name,
                    checkInDate = hotel.checkInDate?.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant(),
                    checkOutDate = hotel.checkOutDate?.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant(),
                    bookingNumber = hotel.bookingNumber,
                    cost = hotel.cost?.toDouble(),
                )
            } ?: emptyList(),
            totalCost = trip.totalCost?.toDouble(),
        )
    }
}