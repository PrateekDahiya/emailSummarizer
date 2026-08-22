package com.gmailreader.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.UUID

data class EmailListResponse(
    val emails: List<EmailResponse>,
    val total: Long,
    val page: Int,
    val size: Int,
)

data class EmailResponse(
    val id: UUID,
    val gmailMessageId: String,
    val threadId: String,
    val sender: String?,
    val senderEmail: String?,
    val subject: String?,
    val snippet: String?,
    val bodyText: String?,
    val bodyHtml: String?,
    val receivedAt: Instant,
    val labels: List<String>?,
    val hasAttachments: Boolean,
    val attachmentMetadata: String?,
    val isProcessed: Boolean,
    val classification: EmailClassificationResponse?,
    val createdAt: Instant,
)

data class EmailClassificationResponse(
    val id: UUID,
    val category: String,
    val importanceScore: Int,
    val summary: String?,
    val actionRequired: Boolean,
    val action: String?,
    val deadline: Instant?,
    val confidence: Double,
    val entities: String?,
    val modelUsed: String?,
    val processingTimeMs: Int?,
    val createdAt: Instant,
)

data class EmailsRequest(
    val category: String? = null,
    val limit: Int = 20,
    val offset: Int = 0,
)