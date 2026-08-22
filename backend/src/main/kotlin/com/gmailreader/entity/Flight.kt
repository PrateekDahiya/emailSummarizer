package com.gmailreader.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "flights", indexes = [
    Index(name = "idx_flights_trip_id", columnList = "trip_id"),
])
data class Flight(
    @Id
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    var id: String = java.util.UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    var trip: TravelTrip? = null,

    @Column(length = 255)
    var airline: String? = null,

    @Column(name = "flight_number", length = 50)
    var flightNumber: String? = null,

    @Column(name = "departure_airport", length = 10)
    var departureAirport: String? = null,

    @Column(name = "arrival_airport", length = 10)
    var arrivalAirport: String? = null,

    @Column(name = "departure_city", length = 255)
    var departureCity: String? = null,

    @Column(name = "arrival_city", length = 255)
    var arrivalCity: String? = null,

    @Column(name = "departure_time")
    var departureTime: Instant? = null,

    @Column(name = "arrival_time")
    var arrivalTime: Instant? = null,

    @Column(name = "booking_number", length = 100)
    var bookingNumber: String? = null,

    @Column(name = "booking_class", length = 50)
    var bookingClass: String? = null,

    @Column(name = "cost", precision = 10, scale = 2)
    var cost: BigDecimal? = null,

    @Column(length = 3)
    var currency: String = "USD",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_email_id")
    var sourceEmail: Email? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)