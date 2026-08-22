package com.gmailreader.repository

import com.gmailreader.entity.Flight
import com.gmailreader.entity.TravelTrip
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FlightRepository : JpaRepository<Flight, UUID> {
    fun findByTrip(trip: TravelTrip): List<Flight>
    fun findByTripOrderByDepartureTimeAsc(trip: TravelTrip): List<Flight>
}