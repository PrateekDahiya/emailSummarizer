package com.gmailreader.repository

import com.gmailreader.entity.GmailAccount
import com.gmailreader.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GmailAccountRepository : JpaRepository<GmailAccount, UUID> {
    fun findByUser(user: User): List<GmailAccount>
    fun findByUserAndIsPrimary(user: User, isPrimary: Boolean): GmailAccount?
    fun findByGmailAddress(gmailAddress: String): GmailAccount?
}