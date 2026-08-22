package com.gmailreader.security

import com.gmailreader.entity.User
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import java.util.Collection

class JwtAuthenticationToken(
    val user: User,
    val token: String,
) : Authentication {

    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()

    override fun getCredentials(): Any = token

    override fun getDetails(): Any = user

    override fun getPrincipal(): Any = user

    override fun isAuthenticated(): Boolean = true

    override fun setAuthenticated(isAuthenticated: Boolean) {}

    override fun getName(): String = user.email
}