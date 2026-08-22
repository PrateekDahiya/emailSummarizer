package com.gmailreader.security

import com.gmailreader.service.AuthService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val authService: AuthService,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(
                    // API auth endpoints
                    "/api/auth/**",
                    "/api/health",
                    "/actuator/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    // Frontend static resources
                    "/",
                    "/index.html",
                    "/_next/**",
                    "/favicon.ico",
                    "/manifest.json",
                    "/*.png",
                    "/*.jpg",
                    "/*.svg",
                    "/*.ico",
                    "/*.woff",
                    "/*.woff2"
                ).permitAll()
                // All API endpoints require authentication
                .requestMatchers("/api/**").authenticated()
                // Everything else (SPA routes) - permit for frontend routing
                .anyRequest().permitAll()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(JwtAuthenticationConverter(authService))
                }
            }
            .addFilterBefore(JwtAuthenticationFilter(authService), UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }
}