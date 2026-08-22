package com.gmailreader.controller

import com.gmailreader.dto.AuthResponse
import com.gmailreader.dto.GoogleAuthUrlResponse
import com.gmailreader.dto.GoogleCallbackRequest
import com.gmailreader.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {

    @GetMapping("/google/url")
    fun getGoogleAuthUrl(): ResponseEntity<GoogleAuthUrlResponse> {
        return ResponseEntity.ok(authService.getGoogleAuthUrl())
    }

    @PostMapping("/google/callback")
    fun handleGoogleCallback(@RequestBody request: GoogleCallbackRequest): ResponseEntity<AuthResponse> {
        return ResponseEntity.ok(authService.handleGoogleCallback(request))
    }

    @GetMapping("/me")
    fun getCurrentUser(@RequestParam("token") token: String): ResponseEntity<Any> {
        val user = authService.getUserFromToken(token)
        return if (user != null) {
            ResponseEntity.ok(mapOf(
                "id" to user.id.toString(),
                "email" to user.email,
                "name" to user.name,
                "picture" to user.picture,
                "gmailConnected" to user.gmailConnected,
                "lastSyncAt" to user.lastSyncAt?.toString(),
            ))
        } else {
            ResponseEntity.status(401).body(mapOf("error" to "Invalid token"))
        }
    }

    @PostMapping("/logout")
    fun logout(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }
}