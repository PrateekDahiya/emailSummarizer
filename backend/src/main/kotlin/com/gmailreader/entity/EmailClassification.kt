package com.gmailreader.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "email_classifications", indexes = [
    Index(name = "idx_email_classifications_email_id", columnList = "email_id"),
    Index(name = "idx_email_classifications_category", columnList = "category"),
    Index(name = "idx_email_classifications_importance", columnList = "importance_score"),
])
data class EmailClassification(
    @Id
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    var id: String = java.util.UUID.randomUUID().toString(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", nullable = false, unique = true)
    var email: Email? = null,

    @Column(nullable = false, length = 50)
    var category: String = "OTHER",

    @Column(name = "importance_score", nullable = false)
    var importanceScore: Int = 0,

    @Column(columnDefinition = "TEXT")
    var summary: String? = null,

    @Column(name = "action_required", nullable = false)
    var actionRequired: Boolean = false,

    @Column(columnDefinition = "TEXT")
    var action: String? = null,

    @Column(name = "deadline")
    var deadline: Instant? = null,

    @Column(name = "confidence", precision = 3, scale = 2)
    var confidence: Double = 0.0,

    @Column(name = "entities", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    var entities: String? = null,

    @Column(name = "model_used", length = 100)
    var modelUsed: String? = null,

    @Column(name = "processing_time_ms")
    var processingTimeMs: Int? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)