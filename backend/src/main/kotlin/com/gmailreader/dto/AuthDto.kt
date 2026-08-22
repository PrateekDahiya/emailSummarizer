package com.gmailreader.dto

data class GoogleAuthUrlResponse(
    val authUrl: String
)

data class GoogleCallbackRequest(
    val code: String
)

data class AuthResponse(
    val user: UserResponse,
    val accessToken: String,
    val refreshToken: String,
)

data class UserResponse(
    val id: String,
    val email: String,
    val name: String?,
    val picture: String?,
    val gmailConnected: Boolean,
    val lastSyncAt: String?,
)

data class SyncStatusResponse(
    val isSyncing: Boolean,
    val lastSyncedAt: String?,
    val totalEmails: Int,
    val processedEmails: Int,
    val error: String?,
)

data class SyncTriggerResponse(
    val success: Boolean,
    val message: String?,
)

data class SearchRequest(
    val query: String,
)

data class SearchResponse(
    val emails: List<EmailSearchResult>,
)

data class EmailSearchResult(
    val id: String,
    val sender: String,
    val subject: String,
    val snippet: String,
    val receivedAt: String,
    val category: String?,
    val importanceScore: Int?,
)

data class AssistantRequest(
    val question: String,
)

data class AssistantResponse(
    val answer: String,
    val sources: List<String>,
)