package com.gmailreader.security

import com.gmailreader.service.AuthService
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.stereotype.Component

@Component
class JwtAuthenticationConverter(
    private val authService: AuthService,
) : JwtAuthenticationConverter() {

    init {
        val grantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter()
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_")
        grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities")
        setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter)
        setPrincipalClaimName("userId")
    }

    override fun convert(jwt: Jwt): Authentication {
        val userId = jwt.getClaimAsString("userId")
        if (userId != null) {
            val user = authService.getUserFromToken(jwt.tokenValue)
            if (user != null) {
                return JwtAuthenticationToken(user, jwt.tokenValue)
            }
        }
        return super.convert(jwt)
    }
}