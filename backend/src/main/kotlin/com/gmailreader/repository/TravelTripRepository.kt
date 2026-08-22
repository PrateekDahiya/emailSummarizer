package com.gmailreader.repository

import com.gmailreader.entity.TravelTrip
import com.gmailreader.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate
import java.util.UUID

interface TravelTripRepository : JpaRepository<TravelTrip, UUID> {
    fun findByUser(user: User): List<TravelTrip>
    fun findByUserOrderByStartDateDesc(user: User): List<TravelTrip>

    @Query("SELECT t FROM TravelTrip t WHERE t.user = :user AND t.startDate >= :today ORDER BY t.startDate ASC")
    fun findUpcomingTripsByUser(@Param("user") user: User, @Param("today") today: LocalDate): List<TravelTrip>
}