package com.gmailreader.controller

import com.gmailreader.dto.JobApplicationSummary
import com.gmailreader.service.AuthService
import com.gmailreader.service.JobApplicationService
import com.gmailreader.entity.JobApplication
import com.gmailreader.entity.User
import com.gmailreader.repository.JobApplicationRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/jobs")
class JobController(
    private val authService: AuthService,
    private val jobApplicationRepository: JobApplicationRepository,
) {

    @GetMapping
    fun getJobApplications(
        @RequestHeader("Authorization") authHeader: String,
    ): ResponseEntity<List<JobApplicationSummary>> {
        val token = authHeader.removePrefix("Bearer ")
        val user = authService.getUserFromToken(token)
            ?: return ResponseEntity.status(401).build()

        val applications = jobApplicationRepository.findByUserOrderByUpdatedAtDesc(user)

        return ResponseEntity.ok(applications.map { toSummary(it) })
    }

    private fun toSummary(app: JobApplication): JobApplicationSummary {
        return JobApplicationSummary(
            id = app.id,
            company = app.company,
            role = app.role,
            status = app.status,
            appliedAt = app.appliedAt,
            interviewDate = app.interviewDate,
            recruiterName = app.recruiterName,
        )
    }
}