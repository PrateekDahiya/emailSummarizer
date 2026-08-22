package com.gmailreader.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity
@Table(name = "gmail_accounts")
data class GmailAccount(
    @Id
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    var id: String = java.util.UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null,

    @Column(name = "gmail_address", nullable = false, length = 255)
    var gmailAddress: String = "",

    @Column(name = "history_id", length = 255)
    var historyId: String? = null,

    @Column(name = "is_primary", nullable = false)
    var isPrimary: Boolean = true,

    @Column(name = "sync_enabled", nullable = false)
    var syncEnabled: Boolean = true,

    @Column(name = "last_history_id", length = 255)
    var lastHistoryId: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)