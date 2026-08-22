package com.gmailreader.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "sync_logs", indexes = [
    Index(name = "idx_sync_logs_gmail_account_id", columnList = "gmail_account_id"),
    Index(name = "idx_sync_logs_started_at", columnList = "started_at"),
])
data class SyncLog(
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gmail_account_id", nullable = false)
    var gmailAccount: GmailAccount? = null,

    @Column(name = "sync_type", nullable = false, length = 50)
    var syncType: String = "",

    @Column(nullable = false, length = 50)
    var status: String = "",

    @Column(name = "emails_fetched", nullable = false)
    var emailsFetched: Int = 0,

    @Column(name = "emails_processed", nullable = false)
    var emailsProcessed: Int = 0,

    @Column(name = "emails_new", nullable = false)
    var emailsNew: Int = 0,

    @Column(name = "emails_updated", nullable = false)
    var emailsUpdated: Int = 0,

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,

    @Column(name = "started_at", nullable = false)
    var startedAt: Instant = Instant.now(),

    @Column(name = "completed_at")
    var completedAt: Instant? = null,

    @Column(name = "duration_ms")
    var durationMs: Int? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)