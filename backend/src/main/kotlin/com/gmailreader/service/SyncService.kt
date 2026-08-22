package com.gmailreader.service

import com.gmailreader.dto.SyncStatusResponse
import com.gmailreader.dto.SyncTriggerResponse
import com.gmailreader.entity.Email
import com.gmailreader.entity.GmailAccount
import com.gmailreader.entity.SyncLog
import com.gmailreader.repository.EmailRepository
import com.gmailreader.repository.GmailAccountRepository
import com.gmailreader.repository.SyncLogRepository
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.CompletableFuture

@Service
class SyncService(
    private val gmailAccountRepository: GmailAccountRepository,
    private val emailRepository: EmailRepository,
    private val gmailService: GmailService,
    private val classificationService: EmailClassificationService,
    private val syncLogRepository: SyncLogRepository,
    private val jobApplicationService: JobApplicationService,
    private val travelService: TravelService,
    private val eventService: EventService,
) {
    private val logger = LoggerFactory.getLogger(SyncService::class.java)
    private var isSyncing = false

    fun getSyncStatus(gmailAccount: GmailAccount): SyncStatusResponse {
        val lastLog = syncLogRepository.findTop1ByGmailAccountOrderByStartedAtDesc(gmailAccount)
        val totalEmails = emailRepository.countByGmailAccount(gmailAccount)
        val processedEmails = emailRepository.countByGmailAccountAndIsProcessed(gmailAccount, true)

        return SyncStatusResponse(
            isSyncing = isSyncing,
            lastSyncedAt = lastLog?.completedAt?.toString(),
            totalEmails = totalEmails.toInt(),
            processedEmails = processedEmails.toInt(),
            error = lastLog?.errorMessage,
        )
    }

    @Async
    @Transactional
    fun triggerSync(gmailAccount: GmailAccount): CompletableFuture<SyncTriggerResponse> {
        if (isSyncing) {
            return CompletableFuture.completedFuture(SyncTriggerResponse(false, "Sync already in progress"))
        }

        isSyncing = true
        val startTime = Instant.now()
        val syncLog = SyncLog(
            gmailAccount = gmailAccount,
            syncType = "INCREMENTAL",
            status = "IN_PROGRESS",
            startedAt = startTime,
        )
        syncLogRepository.save(syncLog)

        return CompletableFuture.supplyAsync {
            try {
                var emailsFetched = 0
                var emailsNew = 0
                var emailsUpdated = 0
                var pageToken: String? = null

                do {
                    val response = gmailService.fetchMessages(gmailAccount, 100, pageToken)
                    val messages = response.messages ?: emptyList()
                    emailsFetched += messages.size

                    for (messageRef in messages) {
                        if (emailRepository.existsByGmailMessageId(messageRef.id!!)) {
                            emailsUpdated++
                        } else {
                            val message = gmailService.fetchMessage(gmailAccount, messageRef.id!!)
                            val email = gmailService.parseMessage(gmailAccount, message)
                            emailRepository.save(email)
                            emailsNew++

                            // Classify the email
                            classificationService.classifyEmail(email)
                        }
                    }

                    pageToken = response.nextPageToken
                } while (pageToken != null && emailsFetched < 500)

                // Process job applications
                jobApplicationService.extractJobApplications(gmailAccount)

                // Process travel
                travelService.extractTravelInfo(gmailAccount)

                // Process events
                eventService.extractEvents(gmailAccount)

                val endTime = Instant.now()
                syncLog.status = "COMPLETED"
                syncLog.emailsFetched = emailsFetched
                syncLog.emailsProcessed = emailsNew + emailsUpdated
                syncLog.emailsNew = emailsNew
                syncLog.emailsUpdated = emailsUpdated
                syncLog.completedAt = endTime
                syncLog.durationMs = java.time.Duration.between(startTime, endTime).toMillis().toInt()
                syncLogRepository.save(syncLog)

                // Update last sync time
                gmailAccount.lastSyncAt = endTime
                gmailAccountRepository.save(gmailAccount)

                isSyncing = false
                SyncTriggerResponse(true, "Sync completed: $emailsNew new, $emailsUpdated updated")
            } catch (e: Exception) {
                logger.error("Sync failed", e)
                val endTime = Instant.now()
                syncLog.status = "FAILED"
                syncLog.errorMessage = e.message
                syncLog.completedAt = endTime
                syncLog.durationMs = java.time.Duration.between(startTime, endTime).toMillis().toInt()
                syncLogRepository.save(syncLog)

                isSyncing = false
                SyncTriggerResponse(false, "Sync failed: ${e.message}")
            }
        }
    }

    @Async
    @Transactional
    fun initialSync(gmailAccount: GmailAccount): CompletableFuture<SyncTriggerResponse> {
        if (isSyncing) {
            return CompletableFuture.completedFuture(SyncTriggerResponse(false, "Sync already in progress"))
        }

        isSyncing = true
        val startTime = Instant.now()
        val syncLog = SyncLog(
            gmailAccount = gmailAccount,
            syncType = "INITIAL",
            status = "IN_PROGRESS",
            startedAt = startTime,
        )
        syncLogRepository.save(syncLog)

        return CompletableFuture.supplyAsync {
            try {
                var emailsFetched = 0
                var emailsNew = 0
                var pageToken: String? = null

                do {
                    val response = gmailService.fetchMessages(gmailAccount, 100, pageToken)
                    val messages = response.messages ?: emptyList()
                    emailsFetched += messages.size

                    for (messageRef in messages) {
                        if (!emailRepository.existsByGmailMessageId(messageRef.id!!)) {
                            val message = gmailService.fetchMessage(gmailAccount, messageRef.id!!)
                            val email = gmailService.parseMessage(gmailAccount, message)
                            emailRepository.save(email)
                            emailsNew++

                            // Classify the email
                            classificationService.classifyEmail(email)
                        }
                    }

                    pageToken = response.nextPageToken
                } while (pageToken != null && emailsFetched < 1000)

                // Process all extracted info
                jobApplicationService.extractJobApplications(gmailAccount)
                travelService.extractTravelInfo(gmailAccount)
                eventService.extractEvents(gmailAccount)

                val endTime = Instant.now()
                syncLog.status = "COMPLETED"
                syncLog.emailsFetched = emailsFetched
                syncLog.emailsProcessed = emailsNew
                syncLog.emailsNew = emailsNew
                syncLog.completedAt = endTime
                syncLog.durationMs = java.time.Duration.between(startTime, endTime).toMillis().toInt()
                syncLogRepository.save(syncLog)

                gmailAccount.lastSyncAt = endTime
                gmailAccountRepository.save(gmailAccount)

                isSyncing = false
                SyncTriggerResponse(true, "Initial sync completed: $emailsNew emails processed")
            } catch (e: Exception) {
                logger.error("Initial sync failed", e)
                val endTime = Instant.now()
                syncLog.status = "FAILED"
                syncLog.errorMessage = e.message
                syncLog.completedAt = endTime
                syncLog.durationMs = java.time.Duration.between(startTime, endTime).toMillis().toInt()
                syncLogRepository.save(syncLog)

                isSyncing = false
                SyncTriggerResponse(false, "Initial sync failed: ${e.message}")
            }
        }
    }
}