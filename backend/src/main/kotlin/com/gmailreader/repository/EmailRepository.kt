package com.gmailreader.repository

import com.gmailreader.entity.Email
import com.gmailreader.entity.GmailAccount
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface EmailRepository : JpaRepository<Email, UUID> {
    fun findByGmailAccount(gmailAccount: GmailAccount): List<Email>
    fun findByGmailAccountOrderByReceivedAtDesc(gmailAccount: GmailAccount): List<Email>
    fun findByGmailAccountAndIsProcessed(gmailAccount: GmailAccount, isProcessed: Boolean): List<Email>
    fun findByGmailMessageId(gmailMessageId: String): Email?
    fun findByThreadId(threadId: String): List<Email>
    fun existsByGmailMessageId(gmailMessageId: String): Boolean

    @Query("SELECT e FROM Email e WHERE e.gmailAccount = :account AND e.receivedAt >= :since ORDER BY e.receivedAt DESC")
    fun findByGmailAccountAndReceivedAfter(@Param("account") account: GmailAccount, @Param("since") since: Instant): List<Email>

    fun findTop50ByGmailAccountOrderByReceivedAtDesc(gmailAccount: GmailAccount): List<Email>

    @Query("SELECT e FROM Email e WHERE e.gmailAccount = :account AND (LOWER(e.subject) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(e.sender) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(e.snippet) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(e.bodyText) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY e.receivedAt DESC")
    fun searchEmails(@Param("account") account: GmailAccount, @Param("query") query: String): List<Email>

    @Query("SELECT e FROM Email e JOIN e.classification c WHERE e.gmailAccount = :account AND c.category = :category ORDER BY e.receivedAt DESC")
    fun findByGmailAccountAndCategory(@Param("account") account: GmailAccount, @Param("category") category: String, pageable: Pageable): Page<Email>
}