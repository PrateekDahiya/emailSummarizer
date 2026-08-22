package com.gmailreader.repository

import com.gmailreader.entity.GmailAccount
import com.gmailreader.entity.SyncLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface SyncLogRepository : JpaRepository<SyncLog, UUID> {
    fun findByGmailAccount(gmailAccount: GmailAccount): List<SyncLog>
    fun findByGmailAccountOrderByStartedAtDesc(gmailAccount: GmailAccount): List<SyncLog>

    @Query("SELECT s FROM SyncLog s WHERE s.gmailAccount = :account ORDER BY s.startedAt DESC")
    fun findByGmailAccountPaged(@Param("account") account: GmailAccount, pageable: Pageable): Page<SyncLog>

    fun findTop1ByGmailAccountOrderByStartedAtDesc(gmailAccount: GmailAccount): SyncLog?
}