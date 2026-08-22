package com.gmailreader.repository

import com.gmailreader.entity.Email
import com.gmailreader.entity.EmailClassification
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EmailClassificationRepository : JpaRepository<EmailClassification, UUID> {
    fun findByEmail(email: Email): EmailClassification?
    fun findByCategory(category: String): List<EmailClassification>
    fun findByImportanceScoreGreaterThanEqual(score: Int): List<EmailClassification>
}