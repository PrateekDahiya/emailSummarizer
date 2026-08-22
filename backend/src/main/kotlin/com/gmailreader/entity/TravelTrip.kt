package com.gmailreader.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "travel_trips", indexes = [
    Index(name = "idx_travel_trips_user_id", columnList = "user_id"),
    Index(name = "idx_travel_trips_dates", columnList = "start_date,end_date"),
])
data class TravelTrip(
    @Id
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    var id: String = java.util.UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null,

    @Column(length = 255)
    var name: String? = null,

    @Column(length = 255)
    var destination: String? = null,

    @Column(name = "start_date")
    var startDate: LocalDate? = null,

    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @Column(name = "total_cost", precision = 12, scale = 2)
    var totalCost: BigDecimal? = null,

    @Column(length = 3)
    var currency: String = "USD",

    @Column(length = 50)
    var status: String = "PLANNED",

    @Column(name = "source_email_ids", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    var sourceEmailIds: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @OneToMany(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var flights: MutableList<Flight> = mutableListOf()

    @OneToMany(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var hotels: MutableList<Hotel> = mutableListOf()
}