package com.gmailreader.controller

import com.gmailreader.dto.DashboardResponse
import com.gmailreader.dto.SyncStatusResponse
import com.gmailreader.dto.SyncTriggerResponse
import com.gmailreader.service.AuthService
import com.gmailreader.service.DashboardService
import com.gmailreader.service.SyncService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/dashboard")
class DashboardController(
    private val authService: AuthService,
    private val dashboardService: DashboardService,
    private val syncService: SyncService,
) {

    @GetMapping
    fun getDashboard(@RequestHeader("Authorization") authHeader: String): ResponseEntity<DashboardResponse> {
        val token = authHeader.removePrefix("Bearer ")
        val user = authService.getUserFromToken(token)
            ?: return ResponseEntity.status(401).build()

        val dashboard = dashboardService.getDashboard(user)
        return ResponseEntity.ok(dashboard)
    }

    @GetMapping("/sync/status")
    fun getSyncStatus(@RequestHeader("Authorization") authHeader: String): ResponseEntity<SyncStatusResponse> {
        val token = authHeader.removePrefix("Bearer ")
        val user = authService.getUserFromToken(token)
            ?: return ResponseEntity.status(401).build()

        val gmailAccount = user.gmailAccounts?.firstOrNull()
            ?: return ResponseEntity.ok(SyncStatusResponse(false, null, 0, 0, "No Gmail account connected"))

        val status = syncService.getSyncStatus(gmailAccount!!)
        return ResponseEntity.ok(status)
    }

    @PostMapping("/sync/trigger")
    fun triggerSync(@RequestHeader("Authorization") authHeader: String): ResponseEntity<SyncTriggerResponse> {
        val token = authHeader.removePrefix("Bearer ")
        val user = authService.getUserFromToken(token)
            ?: return ResponseEntity.status(401).build()

        val gmailAccount = user.gmailAccounts?.firstOrNull()
            ?: return ResponseEntity.badRequest().body(SyncTriggerResponse(false, "No Gmail account connected"))

        val result = syncService.triggerSync(gmailAccount!!).join()
        return ResponseEntity.ok(result)
    }

    @PostMapping("/sync/initial")
    fun initialSync(@RequestHeader("Authorization") authHeader: String): ResponseEntity<SyncTriggerResponse> {
        val token = authHeader.removePrefix("Bearer ")
        val user = authService.getUserFromToken(token)
            ?: return ResponseEntity.status(401).build()

        val gmailAccount = user.gmailAccounts?.firstOrNull()
            ?: return ResponseEntity.badRequest().body(SyncTriggerResponse(false, "No Gmail account connected"))

        val result = syncService.initialSync(gmailAccount!!).join()
        return ResponseEntity.ok(result)
    }
}