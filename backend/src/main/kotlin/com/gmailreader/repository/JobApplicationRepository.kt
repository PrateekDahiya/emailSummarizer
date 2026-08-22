package com.gmailreader.repository

import com.gmailreader.entity.JobApplication
import com.gmailreader.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface JobApplicationRepository : JpaRepository<JobApplication, UUID> {
    fun findByUser(user: User): List<JobApplication>
    fun findByUserAndStatus(user: User, status: String): List<JobApplication>
    fun findByUserOrderByUpdatedAtDesc(user: User): List<JobApplication>
    fun findByCompanyContainingIgnoreCase(company: String): List<JobApplication>

    @Query("SELECT j FROM JobApplication j WHERE j.user = :user AND j.company = :company AND (j.role = :role OR j.role IS NULL)")
    fun findByUserAndCompanyAndRole(@Param("user") user: User, @Param("company") company: String, @Param("role") role: String): List<JobApplication>

    @Query("SELECT DISTINCT j.company FROM JobApplication j WHERE j.user = :user ORDER BY j.company")
    fun findDistinctCompaniesByUser(@Param("user") user: User): List<String>
}