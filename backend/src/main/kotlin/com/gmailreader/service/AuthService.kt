package com.gmailreader.service

import com.gmailreader.dto.AuthResponse
import com.gmailreader.dto.GoogleAuthUrlResponse
import com.gmailreader.dto.GoogleCallbackRequest
import com.gmailreader.dto.UserResponse
import com.gmailreader.entity.GmailAccount
import com.gmailreader.entity.User
import com.gmailreader.repository.GmailAccountRepository
import com.gmailreader.repository.UserRepository
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.oauth2.Oauth2
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.http.HttpCredentialsAdapter
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.Key
import java.time.Instant
import java.util.Date
import java.util.concurrent.CompletableFuture

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val gmailAccountRepository: GmailAccountRepository,
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)
    private val JSON_FACTORY = GsonFactory.getDefaultInstance()
    private var httpTransport: com.google.api.client.http.HttpTransport? = null
    private var jwtKey: Key? = null

    @Value("\${google.oauth.client-id}")
    private lateinit var clientId: String

    @Value("\${google.oauth.client-secret}")
    private lateinit var clientSecret: String

    @Value("\${google.oauth.redirect-uri}")
    private lateinit var redirectUri: String

    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    @Value("\${jwt.expiration:86400000}")
    private var jwtExpiration: Long = 86400000

    @PostConstruct
    fun init() {
        httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        jwtKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))
    }

    fun getGoogleAuthUrl(): GoogleAuthUrlResponse {
        val scopes = listOf(
            "openid",
            "email",
            "profile",
            "https://www.googleapis.com/auth/gmail.readonly"
        )
        
        val authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
            "client_id=$clientId" +
            "&redirect_uri=$redirectUri" +
            "&response_type=code" +
            "&scope=${scopes.joinToString(" ")}" +
            "&access_type=offline" +
            "&prompt=consent"

        return GoogleAuthUrlResponse(authUrl)
    }

    @Transactional
    fun handleGoogleCallback(request: GoogleCallbackRequest): AuthResponse {
        val tokenResponse = exchangeCodeForTokens(request.code)
        
        val googleUser = getGoogleUserInfo(tokenResponse.access_token)
        
        var user = userRepository.findByGoogleId(googleUser.id)
        val isNewUser = user == null
        
        if (isNewUser) {
            user = userRepository.findByEmail(googleUser.email)
        }

        if (user == null) {
            user = User(
                googleId = googleUser.id,
                email = googleUser.email,
                name = googleUser.name,
                picture = googleUser.picture,
                accessToken = tokenResponse.access_token,
                refreshToken = tokenResponse.refresh_token,
                tokenExpiresAt = Instant.now().plusSeconds(tokenResponse.expires_in.toLong()),
            )
        } else {
            user.accessToken = tokenResponse.access_token
            if (tokenResponse.refresh_token != null) {
                user.refreshToken = tokenResponse.refresh_token
            }
            user.tokenExpiresAt = Instant.now().plusSeconds(tokenResponse.expires_in.toLong())
            user.name = googleUser.name
            user.picture = googleUser.picture
        }

        user = userRepository.save(user)

        if (isNewUser) {
            val gmailAccount = GmailAccount(
                user = user,
                gmailAddress = googleUser.email,
                isPrimary = true,
            )
            gmailAccountRepository.save(gmailAccount)
        } else {
            val gmailAccount = gmailAccountRepository.findByUserAndIsPrimary(user, true)
                ?: GmailAccount(user = user, gmailAddress = googleUser.email, isPrimary = true).also { gmailAccountRepository.save(it) }
            gmailAccount.gmailAddress = googleUser.email
            gmailAccountRepository.save(gmailAccount)
        }

        val accessToken = generateJwtToken(user)
        val refreshToken = generateRefreshToken(user)

        return AuthResponse(
            user = UserResponse(
                id = user.id.toString(),
                email = user.email,
                name = user.name,
                picture = user.picture,
                gmailConnected = true,
                lastSyncAt = user.lastSyncAt?.toString(),
            ),
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }

    private fun exchangeCodeForTokens(code: String): TokenResponse {
        val client = HttpClient.newHttpClient()
        val body = "code=$code&client_id=$clientId&client_secret=$clientSecret&redirect_uri=$redirectUri&grant_type=authorization_code"
        
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://oauth2.googleapis.com/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        
        if (response.statusCode() != 200) {
            throw RuntimeException("Failed to exchange code: ${response.body()}")
        }

        val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
        return mapper.readValue<TokenResponse>(response.body())
    }

    private fun getGoogleUserInfo(accessToken: String): GoogleUserInfo {
        val credentials = GoogleCredentials.create(
            com.google.auth.oauth2.AccessToken(accessToken, null)
        ).createScoped(listOf("openid", "email", "profile"))

        val oauth2 = Oauth2.Builder(httpTransport!!, JSON_FACTORY, HttpCredentialsAdapter(credentials))
            .setApplicationName("Gmail Intelligence Dashboard")
            .build()

        val userInfo = oauth2.userinfo().get().execute()
        
        return GoogleUserInfo(
            id = userInfo.id!!,
            email = userInfo.email!!,
            name = userInfo.name,
            picture = userInfo.picture,
        )
    }

    fun getUserFromToken(token: String): User? {
        try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(jwtKey)
                .build()
                .parseClaimsJws(token)
                .body

            val userId = claims.get("userId", String::class.java)
            return userRepository.findById(UUID.fromString(userId)).orElse(null)
        } catch (e: Exception) {
            logger.warn("Invalid token: {}", e.message)
            return null
        }
    }

    private fun generateJwtToken(user: User): String {
        val now = Instant.now()
        return Jwts.builder()
            .setSubject(user.id.toString())
            .claim("userId", user.id.toString())
            .claim("email", user.email)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusMillis(jwtExpiration)))
            .signWith(jwtKey!!)
            .compact()
    }

    private fun generateRefreshToken(user: User): String {
        val now = Instant.now()
        return Jwts.builder()
            .setSubject(user.id.toString())
            .claim("type", "refresh")
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusMillis(604800000))) // 7 days
            .signWith(jwtKey!!)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        try {
            Jwts.parserBuilder()
                .setSigningKey(jwtKey)
                .build()
                .parseClaimsJws(token)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}

data class TokenResponse(
    val access_token: String,
    val refresh_token: String?,
    val expires_in: Int,
    val token_type: String,
    val scope: String,
)

data class GoogleUserInfo(
    val id: String,
    val email: String,
    val name: String?,
    val picture: String?,
)