package com.gmailreader.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "job_applications", indexes = [
    Index(name = "idx_job_applications_user_id", columnList = "user_id"),
    Index(name = "idx_job_applications_status", columnList = "status"),
    Index(name = "idx_job_applications_company", columnList = "company"),
])
data class JobApplication(
    @Id
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    var id: String = java.util.UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null,

    @Column(nullable = false, length = 255)
    var company: String = "",

    @Column(length = 255)
    var role: String? = null,

    @Column(nullable = false, length = 50)
    var status: String = "APPLIED",

    @Column(name = "applied_at")
    var appliedAt: Instant? = null,

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    var updatedAt: Instant = Instant.now(),

    @Column(name = "interview_date")
    var interviewDate: Instant? = null,

    @Column(name = "recruiter_name", length = 255)
    var recruiterName: String? = null,

    @Column(name = "recruiter_email", length = 255)
    var recruiterEmail: String? = null,

    @Column(name = "source_email_ids", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    var sourceEmailIds: String? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)