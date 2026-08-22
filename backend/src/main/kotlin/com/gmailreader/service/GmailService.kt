package com.gmailreader.service

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.ListMessagesResponse
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.MessagePart
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.http.HttpCredentialsAdapter
import com.gmailreader.entity.Email
import com.gmailreader.entity.GmailAccount
import com.gmailreader.repository.EmailRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import java.util.concurrent.TimeUnit

@Service
class GmailService(
    private val emailRepository: EmailRepository,
) {
    private val logger = LoggerFactory.getLogger(GmailService::class.java)
    private val JSON_FACTORY = GsonFactory.getDefaultInstance()
    private var httpTransport: com.google.api.client.http.HttpTransport? = null

    @Value("\${google.gmail.scopes}")
    private lateinit var scopes: List<String>

    @PostConstruct
    fun init() {
        httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    }

    private fun buildGmailService(accessToken: String): Gmail {
        val credentials = GoogleCredentials.create(
            com.google.auth.oauth2.AccessToken(accessToken, null)
        ).createScoped(scopes)

        return Gmail.Builder(httpTransport!!, JSON_FACTORY, HttpCredentialsAdapter(credentials))
            .setApplicationName("Gmail Intelligence Dashboard")
            .build()
    }

    fun fetchMessages(gmailAccount: GmailAccount, maxResults: Int = 50, pageToken: String? = null): ListMessagesResponse {
        val service = buildGmailService(gmailAccount.user!!.accessToken!!)
        return service.users().messages()
            .list("me")
            .setMaxResults(maxResults.toLong())
            .setPageToken(pageToken)
            .setLabelIds(listOf("INBOX"))
            .execute()
    }

    fun fetchMessage(gmailAccount: GmailAccount, messageId: String): Message {
        val service = buildGmailService(gmailAccount.user!!.accessToken!!)
        return service.users().messages()
            .get("me", messageId)
            .setFormat("full")
            .execute()
    }

    fun fetchThread(gmailAccount: GmailAccount, threadId: String): com.google.api.services.gmail.model.Thread {
        val service = buildGmailService(gmailAccount.user!!.accessToken!!)
        return service.users().threads()
            .get("me", threadId)
            .setFormat("full")
            .execute()
    }

    fun getProfile(gmailAccount: GmailAccount): com.google.api.services.gmail.model.Profile {
        val service = buildGmailService(gmailAccount.user!!.accessToken!!)
        return service.users().getProfile("me").execute()
    }

    fun parseMessage(gmailAccount: GmailAccount, message: Message): Email {
        val payload = message.payload
        val headers = payload.headers ?: emptyList()

        val headerMap = headers.associateBy({ it.name.lowercase() }, { it.value })

        val sender = headerMap["from"] ?: ""
        val senderEmail = extractEmail(sender)
        val subject = headerMap["subject"] ?: ""
        val dateHeader = headerMap["date"] ?: ""
        val receivedAt = parseDate(dateHeader)
        val threadId = message.threadId ?: ""

        val labels = message.labelIds?.toList() ?: emptyList()
        val snippet = message.snippet ?: ""

        val (bodyText, bodyHtml) = extractBody(payload)
        val hasAttachments = hasAttachments(payload)
        val attachmentMetadata = extractAttachmentMetadata(payload)

        val recipientEmails = headerMap["to"]?.split(",")?.map { extractEmail(it.trim()) }?.toTypedArray()
        val ccEmails = headerMap["cc"]?.split(",")?.map { extractEmail(it.trim()) }?.toTypedArray()
        val bccEmails = headerMap["bcc"]?.split(",")?.map { extractEmail(it.trim()) }?.toTypedArray()

        return Email(
            gmailAccount = gmailAccount,
            gmailMessageId = message.id!!,
            threadId = threadId,
            sender = extractName(sender),
            senderEmail = senderEmail,
            recipientEmails = recipientEmails,
            ccEmails = ccEmails,
            bccEmails = bccEmails,
            subject = subject,
            snippet = snippet,
            bodyText = bodyText,
            bodyHtml = bodyHtml,
            receivedAt = receivedAt,
            labels = labels.toTypedArray(),
            hasAttachments = hasAttachments,
            attachmentMetadata = attachmentMetadata,
            isProcessed = false,
        )
    }

    private fun extractBody(payload: MessagePart?): Pair<String?, String?> {
        if (payload == null) return null to null

        var textBody: String? = null
        var htmlBody: String? = null

        if (payload.mimeType == "text/plain" && payload.body?.data != null) {
            textBody = decodeBase64(payload.body.data)
        } else if (payload.mimeType == "text/html" && payload.body?.data != null) {
            htmlBody = decodeBase64(payload.body.data)
        } else if (payload.parts != null) {
            for (part in payload.parts!!) {
                val (text, html) = extractBody(part)
                textBody = textBody ?: text
                htmlBody = htmlBody ?: html
            }
        }

        return textBody to htmlBody
    }

    private fun hasAttachments(payload: MessagePart?): Boolean {
        if (payload == null) return false
        if (payload.filename != null && payload.filename!!.isNotBlank()) return true
        if (payload.parts != null) {
            return payload.parts!!.any { hasAttachments(it) }
        }
        return false
    }

    private fun extractAttachmentMetadata(payload: MessagePart?): String? {
        if (payload == null) return null
        val attachments = mutableListOf<Map<String, Any>>()

        fun collectAttachments(part: MessagePart) {
            if (part.filename != null && part.filename!!.isNotBlank()) {
                attachments.add(mapOf(
                    "filename" to part.filename,
                    "mimeType" to part.mimeType,
                    "size" to part.body?.size?.toLong() ?: 0L,
                    "attachmentId" to part.body?.attachmentId
                ))
            }
            part.parts?.forEach { collectAttachments(it) }
        }

        collectAttachments(payload)
        return if (attachments.isNotEmpty()) {
            com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().writeValueAsString(attachments)
        } else null
    }

    private fun decodeBase64(data: String): String {
        val decoded = Base64.getUrlDecoder().decode(data)
        return String(decoded, StandardCharsets.UTF_8)
    }

    private fun extractEmail(fromHeader: String): String {
        val regex = "<(.+?)>".toRegex()
        val match = regex.find(fromHeader)
        return match?.groupValues?.get(1) ?: fromHeader.trim()
    }

    private fun extractName(fromHeader: String): String {
        val regex = "^(.+?)<".toRegex()
        val match = regex.find(fromHeader)
        return match?.groupValues?.get(1)?.trim() ?: fromHeader.trim()
    }

    private fun parseDate(dateHeader: String): Instant {
        return try {
            val formatter = java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
            Instant.from(formatter.parse(dateHeader))
        } catch (e: Exception) {
            logger.warn("Failed to parse date: $dateHeader", e)
            Instant.now()
        }
    }
}