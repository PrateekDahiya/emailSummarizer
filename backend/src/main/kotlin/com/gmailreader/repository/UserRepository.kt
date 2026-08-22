package com.gmailreader.repository

import com.gmailreader.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByGoogleId(googleId: String): User?
    fun findByEmail(email: String): User?
    fun existsByGoogleId(googleId: String): Boolean
    fun existsByEmail(email: String): Boolean

    @Query("SELECT u FROM User u WHERE u.googleId = :googleId OR u.email = :email")
    fun findByGoogleIdOrEmail(@Param("googleId") googleId: String, @Param("email") email: String): User?
}