package com.gmailreader.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.core.type.TypeReference
import com.gmailreader.entity.Email
import com.gmailreader.entity.EmailClassification
import com.gmailreader.entity.JobApplication
import com.gmailreader.entity.User
import com.gmailreader.repository.EmailRepository
import com.gmailreader.repository.JobApplicationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class JobApplicationService(
    private val emailRepository: EmailRepository,
    private val jobApplicationRepository: JobApplicationRepository,
) {
    private val logger = LoggerFactory.getLogger(JobApplicationService::class.java)
    private val mapper = jacksonObjectMapper()
    private val typeRef = object : TypeReference<Map<String, Any>>() {}

    @Transactional
    fun extractJobApplications(gmailAccount: GmailAccount) {
        val user = gmailAccount.user!!
        val classifications = emailRepository.findByGmailAccount(gmailAccount)
            .filter { it.isProcessed }
            .mapNotNull { email ->
                email.classification?.let { classification ->
                    email to classification
                }
            }
            .filter { (_, classification) ->
                classification.category in setOf("JOB", "INTERVIEW")
            }

        for ((email, classification) in classifications) {
            val entities = parseEntities(classification.entities)
            val company = entities["company"] as String? ?: extractCompanyFromEmail(email)
            val role = entities["role"] as String? ?: extractRoleFromEmail(email)
            val status = determineStatus(classification, entities)
            val interviewDate = entities["interview_date"] as String??.let { Instant.parse(it) }
            val recruiterName = entities["recruiter_name"] as String? ?: extractRecruiterName(email)

            if (company.isNullOrBlank()) continue

            var application = jobApplicationRepository.findByUserAndCompanyAndRole(user, company, role ?: "")
                .firstOrNull()

            if (application == null) {
                application = JobApplication(
                    user = user,
                    company = company,
                    role = role,
                    status = status,
                    appliedAt = email.receivedAt,
                    interviewDate = interviewDate,
                    recruiterName = recruiterName,
                    recruiterEmail = email.senderEmail,
                    sourceEmailIds = arrayOf(email.id),
                )
            } else {
                application.status = status
                application.updatedAt = Instant.now()
                application.interviewDate = interviewDate ?: application.interviewDate
                application.recruiterName = recruiterName ?: application.recruiterName
                if (email.id !in (application.sourceEmailIds ?: emptyArray())) {
                    application.sourceEmailIds = (application.sourceEmailIds ?: emptyArray()) + email.id
                }
            }

            jobApplicationRepository.save(application)
        }
    }

    private fun determineStatus(classification: EmailClassification, entities: Map<String, Any>): String {
        val appStatus = entities["application_status"] as String?
        return when {
            appStatus != null -> appStatus
            classification.category == "INTERVIEW" -> "INTERVIEW"
            classification.actionRequired && classification.action?.contains("follow", true) == true -> "APPLIED"
            else -> "APPLIED"
        }
    }

    private fun parseEntities(entitiesJson: String?): Map<String, Any> {
        if (entitiesJson.isNullOrBlank()) return emptyMap()
        return try {
            mapper.readValue(entitiesJson, typeRef)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun extractCompanyFromEmail(email: Email): String? {
        val sender = email.senderEmail?.lowercase() ?: ""
        val domain = sender.substringAfterLast("@")
        val knownDomains = setOf("gmail.com", "yahoo.com", "outlook.com", "hotmail.com", "icloud.com")
        return if (domain !in knownDomains) {
            domain.substringBefore(".").capitalize()
        } else {
            email.sender?.split(" ").firstOrNull()?.capitalize()
        }
    }

    private fun extractRoleFromEmail(email: Email): String? {
        val subject = (email.subject ?: "").lowercase()
        val keywords = listOf("software engineer", "developer", "sde", "backend", "frontend", "full stack", "devops", "data scientist", "ml engineer", "intern")
        return keywords.find { subject.contains(it) }?.uppercase()
    }

    private fun extractRecruiterName(email: Email): String? {
        return email.sender?.let { name ->
            if (name.contains("<")) name.substringBefore("<").trim() else name
        }
    }
}