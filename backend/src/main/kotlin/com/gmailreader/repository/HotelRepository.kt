package com.gmailreader.repository

import com.gmailreader.entity.Hotel
import com.gmailreader.entity.TravelTrip
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface HotelRepository : JpaRepository<Hotel, UUID> {
    fun findByTrip(trip: TravelTrip): List<Hotel>
    fun findByTripOrderByCheckInDateAsc(trip: TravelTrip): List<Hotel>
}