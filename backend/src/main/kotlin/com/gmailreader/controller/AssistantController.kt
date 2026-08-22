package com.gmailreader.controller

import com.gmailreader.dto.AssistantRequest
import com.gmailreader.dto.AssistantResponse
import com.gmailreader.service.AuthService
import com.gmailreader.service.AssistantService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/assistant")
class AssistantController(
    private val authService: AuthService,
    private val assistantService: AssistantService,
) {

    @PostMapping("/ask")
    fun askAssistant(
        @RequestHeader("Authorization") authHeader: String,
        @RequestBody request: AssistantRequest,
    ): ResponseEntity<AssistantResponse> {
        val token = authHeader.removePrefix("Bearer ")
        val user = authService.getUserFromToken(token)
            ?: return ResponseEntity.status(401).build()

        val response = assistantService.answerQuestion(user, request.question)
        return ResponseEntity.ok(response)
    }
}