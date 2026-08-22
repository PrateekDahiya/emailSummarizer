package com.gmailreader.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.http.HttpCredentialsAdapter
import com.gmailreader.entity.Email
import com.gmailreader.entity.GmailAccount
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import java.util.concurrent.CompletableFuture

@Service
class GmailService(
    private val emailRepository: EmailRepository,
) {
    private val logger = LoggerFactory.getLogger(GmailService::class.java)
    private val mapper = jacksonObjectMapper()
    private val httpClient = HttpClient.newBuilder().build()
    private var httpTransport: com.google.api.client.http.HttpTransport? = null

    @Value("\${google.gmail.scopes}")
    private lateinit var scopes: List<String>

    @PostConstruct
    fun init() {
        httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    }

    private fun buildCredentials(accessToken: String) = GoogleCredentials.create(
        com.google.auth.oauth2.AccessToken(accessToken, null)
    ).createScoped(scopes)

    private fun buildHttpRequest(url: String, accessToken: String): HttpRequest {
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .GET()
            .build()
    }

    fun fetchMessages(gmailAccount: GmailAccount, maxResults: Int = 50, pageToken: String? = null): ListMessagesResponse {
        val accessToken = gmailAccount.user!!.accessToken!!
        var url = "https://gmail.googleapis.com/gmail/v1/users/me/messages?maxResults=$maxResults&labelIds=INBOX"
        if (pageToken != null) {
            url += "&pageToken=$pageToken"
        }

        val request = buildHttpRequest(url, accessToken)
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        
        if (response.statusCode() != 200) {
            throw RuntimeException("Gmail API error: ${response.statusCode()} - ${response.body()}")
        }
        
        return mapper.readValue<ListMessagesResponse>(response.body())
    }

    fun fetchMessage(gmailAccount: GmailAccount, messageId: String): Message {
        val accessToken = gmailAccount.user!!.accessToken!!
        val url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/$messageId?format=full"
        
        val request = buildHttpRequest(url, accessToken)
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        
        if (response.statusCode() != 200) {
            throw RuntimeException("Gmail API error: ${response.statusCode()} - ${response.body()}")
        }
        
        return mapper.readValue<Message>(response.body())
    }

    fun fetchThread(gmailAccount: GmailAccount, threadId: String): Thread {
        val accessToken = gmailAccount.user!!.accessToken!!
        val url = "https://gmail.googleapis.com/gmail/v1/users/me/threads/$threadId?format=full"
        
        val request = buildHttpRequest(url, accessToken)
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        
        if (response.statusCode() != 200) {
            throw RuntimeException("Gmail API error: ${response.statusCode()} - ${response.body()}")
        }
        
        return mapper.readValue<Thread>(response.body())
    }

    fun getProfile(gmailAccount: GmailAccount): Profile {
        val accessToken = gmailAccount.user!!.accessToken!!
        val url = "https://gmail.googleapis.com/gmail/v1/users/me/profile"
        
        val request = buildHttpRequest(url, accessToken)
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        
        if (response.statusCode() != 200) {
            throw RuntimeException("Gmail API error: ${response.statusCode()} - ${response.body()}")
        }
        
        return mapper.readValue<Profile>(response.body())
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

// DTOs for Gmail API responses
data class ListMessagesResponse(
    val messages: List<MessageRef>? = null,
    val nextPageToken: String? = null,
    val resultSizeEstimate: Long = 0
)

data class MessageRef(
    val id: String? = null,
    val threadId: String? = null
)

data class Message(
    val id: String? = null,
    val threadId: String? = null,
    val labelIds: List<String>? = null,
    val snippet: String? = null,
    val payload: MessagePart? = null
)

data class MessagePart(
    val partId: String? = null,
    val mimeType: String? = null,
    val filename: String? = null,
    val headers: List<Header>? = null,
    val body: Body? = null,
    val parts: List<MessagePart>? = null
)

data class Header(
    val name: String? = null,
    val value: String? = null
)

data class Body(
    val size: Int? = null,
    val data: String? = null,
    val attachmentId: String? = null
)

data class Thread(
    val id: String? = null,
    val messages: List<Message>? = null
)

data class Profile(
    val emailAddress: String? = null,
    val messagesTotal: Long = 0,
    val threadsTotal: Long = 0,
    val historyId: String? = null
)