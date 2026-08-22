package com.gmailreader.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "hotels", indexes = [
    Index(name = "idx_hotels_trip_id", columnList = "trip_id"),
])
data class Hotel(
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    var trip: TravelTrip? = null,

    @Column(length = 255)
    var name: String? = null,

    @Column(columnDefinition = "TEXT")
    var address: String? = null,

    @Column(length = 255)
    var city: String? = null,

    @Column(length = 255)
    var country: String? = null,

    @Column(name = "check_in_date")
    var checkInDate: LocalDate? = null,

    @Column(name = "check_out_date")
    var checkOutDate: LocalDate? = null,

    @Column(name = "booking_number", length = 100)
    var bookingNumber: String? = null,

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