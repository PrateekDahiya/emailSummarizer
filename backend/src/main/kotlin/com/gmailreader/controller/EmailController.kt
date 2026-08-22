package com.gmailreader.controller

import com.gmailreader.dto.EmailListResponse
import com.gmailreader.dto.EmailResponse
import com.gmailreader.dto.EmailsRequest
import com.gmailreader.service.AuthService
import com.gmailreader.service.EmailClassificationService
import com.gmailreader.entity.Email
import com.gmailreader.entity.EmailClassification
import com.gmailreader.entity.GmailAccount
import com.gmailreader.repository.EmailRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/emails")
class EmailController(
    private val authService: AuthService,
    private val emailRepository: EmailRepository,
    private val classificationService: EmailClassificationService,
) {

    @GetMapping
    fun getEmails(
        @RequestHeader("Authorization") authHeader: String,
        @RequestParam(required = false) category: String?,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int,
    ): ResponseEntity<EmailListResponse> {
        val token = authHeader.removePrefix("Bearer ")
        val user = authService.getUserFromToken(token)
            ?: return ResponseEntity.status(401).build()

        val gmailAccount = user.gmailAccounts?.firstOrNull()
            ?: return ResponseEntity.ok(EmailListResponse(emptyList(), 0, 0, limit))

        val pageable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "receivedAt"))
        val emails = if (category != null && category != "ALL") {
            emailRepository.findByGmailAccountAndCategory(gmailAccount!!, category, pageable)
        } else {
            emailRepository.findByGmailAccount(gmailAccount!!, pageable)
        }

        val total = emailRepository.countByGmailAccount(gmailAccount!!)

        return ResponseEntity.ok(EmailListResponse(
            emails = emails.content.map { toResponse(it) },
            total = total,
            page = offset / limit,
            size = limit,
        ))
    }

    @GetMapping("/{id}")
    fun getEmail(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable id: String,
    ): ResponseEntity<EmailResponse> {
        val token = authHeader.removePrefix("Bearer ")
        val user = authService.getUserFromToken(token)
            ?: return ResponseEntity.status(401).build()

        val email = emailRepository.findById(UUID.fromString(id)).orElse(null)
        if (email == null || email.gmailAccount?.user?.id != user.id) {
            return ResponseEntity.notFound().build()
        }

        return ResponseEntity.ok(toResponse(email))
    }

    @PostMapping("/{id}/reclassify")
    fun reclassifyEmail(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable id: String,
    ): ResponseEntity<EmailResponse> {
        val token = authHeader.removePrefix("Bearer ")
        val user = authService.getUserFromToken(token)
            ?: return ResponseEntity.status(401).build()

        val email = emailRepository.findById(UUID.fromString(id)).orElse(null)
        if (email == null || email.gmailAccount?.user?.id != user.id) {
            return ResponseEntity.notFound().build()
        }

        val classification = classificationService.classifyEmail(email)
        email.isProcessed = true
        email.processedAt = Instant.now()
        emailRepository.save(email)

        return ResponseEntity.ok(toResponse(email))
    }

    private fun toResponse(email: Email): EmailResponse {
        return EmailResponse(
            id = email.id,
            gmailMessageId = email.gmailMessageId,
            threadId = email.threadId,
            sender = email.sender,
            senderEmail = email.senderEmail,
            subject = email.subject,
            snippet = email.snippet,
            bodyText = email.bodyText,
            bodyHtml = email.bodyHtml,
            receivedAt = email.receivedAt,
            labels = email.labels?.toList(),
            hasAttachments = email.hasAttachments,
            attachmentMetadata = email.attachmentMetadata,
            isProcessed = email.isProcessed,
            classification = email.classification?.let { toClassificationResponse(it) },
            createdAt = email.createdAt,
        )
    }

    private fun toClassificationResponse(c: EmailClassification): com.gmailreader.dto.EmailClassificationResponse {
        return com.gmailreader.dto.EmailClassificationResponse(
            id = c.id,
            category = c.category,
            importanceScore = c.importanceScore,
            summary = c.summary,
            actionRequired = c.actionRequired,
            action = c.action,
            deadline = c.deadline,
            confidence = c.confidence,
            entities = c.entities,
            modelUsed = c.modelUsed,
            processingTimeMs = c.processingTimeMs,
            createdAt = c.createdAt,
        )
    }
}