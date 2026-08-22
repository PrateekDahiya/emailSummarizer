package com.gmailreader.controller

import com.gmailreader.dto.SearchResponse
import com.gmailreader.dto.EmailSearchResult
import com.gmailreader.service.AuthService
import com.gmailreader.entity.Email
import com.gmailreader.entity.User
import com.gmailreader.repository.EmailRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/search")
class SearchController(
    private val authService: AuthService,
    private val emailRepository: EmailRepository,
) {

    @GetMapping
    fun searchEmails(
        @RequestHeader("Authorization") authHeader: String,
        @RequestParam("q") query: String,
    ): ResponseEntity<SearchResponse> {
        val token = authHeader.removePrefix("Bearer ")
        val user = authService.getUserFromToken(token)
            ?: return ResponseEntity.status(401).build()

        val gmailAccount = user.gmailAccounts?.firstOrNull()
            ?: return ResponseEntity.ok(SearchResponse(emptyList()))

        val emails = emailRepository.searchEmails(gmailAccount!!, query)

        return ResponseEntity.ok(SearchResponse(
            emails = emails.map { toSearchResult(it) }
        ))
    }

    private fun toSearchResult(email: Email): EmailSearchResult {
        return EmailSearchResult(
            id = email.id.toString(),
            sender = email.sender ?: email.senderEmail ?: "Unknown",
            subject = email.subject ?: "No subject",
            snippet = email.snippet ?: "",
            receivedAt = email.receivedAt.toString(),
            category = email.classification?.category,
            importanceScore = email.classification?.importanceScore,
        )
    }
}