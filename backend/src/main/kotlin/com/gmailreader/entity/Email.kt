package com.gmailreader.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "emails", indexes = [
    Index(name = "idx_emails_gmail_account_id", columnList = "gmail_account_id"),
    Index(name = "idx_emails_thread_id", columnList = "thread_id"),
    Index(name = "idx_emails_received_at", columnList = "received_at"),
    Index(name = "idx_emails_sender_email", columnList = "sender_email"),
    Index(name = "idx_emails_is_processed", columnList = "is_processed"),
])
data class Email(
    @Id
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    var id: String = java.util.UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gmail_account_id", nullable = false)
    var gmailAccount: GmailAccount? = null,

    @Column(name = "gmail_message_id", nullable = false, unique = true, length = 255)
    var gmailMessageId: String = "",

    @Column(name = "thread_id", nullable = false, length = 255)
    var threadId: String = "",

    @Column(length = 500)
    var sender: String? = null,

    @Column(name = "sender_email", length = 500)
    var senderEmail: String? = null,

    @Column(name = "recipient_emails", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    var recipientEmails: String? = null,

    @Column(name = "cc_emails", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    var ccEmails: String? = null,

    @Column(name = "bcc_emails", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    var bccEmails: String? = null,

    @Column(length = 1000)
    var subject: String? = null,

    @Column(columnDefinition = "TEXT")
    var snippet: String? = null,

    @Column(name = "body_text", columnDefinition = "LONGTEXT")
    var bodyText: String? = null,

    @Column(name = "body_html", columnDefinition = "LONGTEXT")
    var bodyHtml: String? = null,

    @Column(name = "received_at", nullable = false)
    var receivedAt: Instant = Instant.now(),

    @Column(name = "labels", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    var labels: String? = null,

    @Column(name = "has_attachments", nullable = false)
    var hasAttachments: Boolean = false,

    @Column(name = "attachment_metadata", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    var attachmentMetadata: String? = null,

    @Column(name = "is_processed", nullable = false)
    var isProcessed: Boolean = false,

    @Column(name = "processed_at")
    var processedAt: Instant? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @OneToOne(mappedBy = "email", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var classification: EmailClassification? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_email_id")
    var flightSourceEmail: Flight? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_email_id")
    var hotelSourceEmail: Hotel? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_email_id")
    var eventSourceEmail: Event? = null
}