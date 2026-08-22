package com.gmailreader.repository

import com.gmailreader.entity.Event
import com.gmailreader.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface EventRepository : JpaRepository<Event, UUID> {
    fun findByUser(user: User): List<Event>
    fun findByUserOrderByStartTimeAsc(user: User): List<Event>

    @Query("SELECT e FROM Event e WHERE e.user = :user AND e.startTime >= :now ORDER BY e.startTime ASC")
    fun findUpcomingEventsByUser(@Param("user") user: User, @Param("now") now: Instant): List<Event>

    @Query("SELECT e FROM Event e WHERE e.user = :user AND e.startTime BETWEEN :start AND :end ORDER BY e.startTime ASC")
    fun findEventsByUserAndDateRange(@Param("user") user: User, @Param("start") start: Instant, @Param("end") end: Instant): List<Event>
}