package com.gmailreader.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.gmailreader.dto.ClassificationResult
import com.gmailreader.entity.Email
import com.gmailreader.entity.EmailClassification
import com.gmailreader.repository.EmailClassificationRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant

@Service
class EmailClassificationService(
    private val classificationRepository: EmailClassificationRepository,
) {
    private val logger = LoggerFactory.getLogger(EmailClassificationService::class.java)
    private val mapper = jacksonObjectMapper()
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    @Value("\${app.ai.enabled:true}")
    private var aiEnabled: Boolean = true

    @Value("\${app.ai.groq.model:llama-3.1-8b-instant}")
    private var model: String = "llama-3.1-8b-instant"

    @Value("\${app.ai.groq.base-url:https://api.groq.com/openai/v1}")
    private var baseUrl: String = "https://api.groq.com/openai/v1"

    @Value("\${app.ai.max-tokens:2000}")
    private var maxTokens: Int = 2000

    @Value("\${app.ai.temperature:0.1}")
    private var temperature: Double = 0.1

    @Value("\${GROQ_API_KEY:}")
    private var apiKey: String = ""

    private val classificationPrompt = """
        You are an expert email classifier. Analyze the email and extract structured information.
        
        Return ONLY valid JSON with this exact structure:
        {
          "category": "JOB|INTERVIEW|TRAVEL|FINANCE|PURCHASE|MEETING|DEADLINE|DOCUMENT|PERSONAL|NEWSLETTER|PROMOTION|OTHER",
          "importance_score": 0-100,
          "summary": "One sentence summary",
          "action_required": true/false,
          "action": "Specific action needed or null",
          "deadline": "ISO8601 date or null",
          "confidence": 0.0-1.0,
          "entities": {
            "company": "company name or null",
            "role": "job role or null",
            "application_status": "APPLIED|INTERVIEW|OFFER|REJECTED|WAITING or null",
            "interview_date": "ISO8601 date or null",
            "airline": "airline name or null",
            "flight_number": "flight number or null",
            "departure": "departure city or null",
            "arrival": "arrival city or null",
            "hotel": "hotel name or null",
            "booking_number": "booking number or null",
            "travel_dates": ["ISO8601 dates"],
            "event_title": "event title or null",
            "event_date": "ISO8601 date or null",
            "event_time": "time or null",
            "event_location": "location or null",
            "amount": 0.0,
            "currency": "USD",
            "transaction_type": "payment|refund|invoice|subscription|salary or null",
            "people": ["names"]
          }
        }
        
        IMPORTANCE SCORING:
        - INTERVIEW: +30
        - JOB/OFFER: +25
        - DEADLINE: +25
        - FLIGHT/TRAVEL: +20
        - PAYMENT ISSUE: +20
        - MEETING: +15
        - DOCUMENT REQUEST: +10
        - NEWSLETTER: -20
        - PROMOTIONAL: -30
        - MARKETING: -40
        Base score: 50
        
        Email to analyze:
        Subject: {subject}
        From: {sender}
        Date: {date}
        Body: {body}
        """

    fun classifyEmail(email: Email): EmailClassification {
        val startTime = System.currentTimeMillis()
        
        val classification = if (aiEnabled && apiKey.isNotBlank()) {
            try {
                classifyWithAI(email)
            } catch (e: Exception) {
                logger.warn("AI classification failed, falling back to rules: {}", e.message)
                classifyWithRules(email)
            }
        } else {
            classifyWithRules(email)
        }

        classification.processingTimeMs = (System.currentTimeMillis() - startTime).toInt()
        classification.email = email
        classification.createdAt = Instant.now()
        classification.updatedAt = Instant.now()

        return classificationRepository.save(classification)
    }

    private fun classifyWithAI(email: Email): EmailClassification {
        val prompt = classificationPrompt
            .replace("{subject}", email.subject ?: "")
            .replace("{sender}", "${email.sender} <${email.senderEmail}>")
            .replace("{date}", email.receivedAt.toString())
            .replace("{body}", truncate(email.bodyText ?: email.snippet ?: "", 3000))

        val requestBody = mapOf(
            "model" to model,
            "messages" to listOf(
                mapOf("role" to "system", "content" to "You are an email classification expert. Return only valid JSON."),
                mapOf("role" to "user", "content" to prompt)
            ),
            "max_tokens" to maxTokens,
            "temperature" to temperature,
            "response_format" to mapOf("type" to "json_object")
        )

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/chat/completions"))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody)))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        
        if (response.statusCode() != 200) {
            throw RuntimeException("Groq API error: ${response.statusCode()} - ${response.body()}")
        }

        val jsonResponse = mapper.readTree(response.body())
        val content = jsonResponse["choices"][0]["message"]["content"].asText()
        val result = mapper.readValue<ClassificationResult>(content)

        return EmailClassification(
            category = result.category,
            importanceScore = result.importanceScore,
            summary = result.summary,
            actionRequired = result.actionRequired,
            action = result.action,
            deadline = result.deadline?.let { Instant.parse(it) },
            confidence = result.confidence,
            entities = mapper.writeValueAsString(result.entities),
            modelUsed = model,
        )
    }

    private fun classifyWithRules(email: Email): EmailClassification {
        val subject = (email.subject ?: "").lowercase()
        val sender = (email.sender ?: "").lowercase()
        val body = (email.bodyText ?: email.snippet ?: "").lowercase()
        val combined = "$subject $sender $body"

        var category = "OTHER"
        var importanceScore = 50
        var actionRequired = false
        var action: String? = null
        var deadline: Instant? = null
        var confidence = 0.6
        val entities = mutableMapOf<String, Any>()

        // Job-related keywords
        if (combined.contains("interview") || combined.contains("phone screen") || combined.contains("technical interview")) {
            category = "INTERVIEW"
            importanceScore += 30
            actionRequired = true
            action = "Prepare for interview"
            entities["interview_date"] = extractDate(combined)
        } else if (combined.contains("job") || combined.contains("position") || combined.contains("application") ||
                   combined.contains("recruiter") || combined.contains("hiring") || combined.contains("career")) {
            category = "JOB"
            importanceScore += 25
            actionRequired = true
            action = "Follow up on application"
        }

        // Travel keywords
        if (combined.contains("flight") || combined.contains("airline") || combined.contains("booking") ||
            combined.contains("itinerary") || combined.contains("boarding")) {
            if (category == "OTHER") category = "TRAVEL"
            importanceScore += 20
            actionRequired = true
            action = "Check travel details"
            entities["airline"] = extractAirline(combined)
            entities["flight_number"] = extractFlightNumber(combined)
            entities["departure"] = extractLocation(combined, "from")
            entities["arrival"] = extractLocation(combined, "to")
        }

        // Hotel keywords
        if (combined.contains("hotel") || combined.contains("booking.com") || combined.contains("expedia") ||
            combined.contains("check-in") || combined.contains("reservation")) {
            if (category == "OTHER") category = "TRAVEL"
            importanceScore += 15
            entities["hotel"] = extractHotelName(combined)
        }

        // Finance keywords
        if (combined.contains("payment") || combined.contains("invoice") || combined.contains("receipt") ||
            combined.contains("billing") || combined.contains("subscription") || combined.contains("refund")) {
            if (category == "OTHER") category = "FINANCE"
            importanceScore += 15
            actionRequired = true
            action = "Review payment"
            entities["transaction_type"] = extractTransactionType(combined)
            entities["amount"] = extractAmount(combined)
        }

        // Deadline keywords
        if (combined.contains("deadline") || combined.contains("due date") || combined.contains("expires") ||
            combined.contains("last date") || combined.contains("submit by")) {
            if (category == "OTHER") category = "DEADLINE"
            importanceScore += 25
            actionRequired = true
            action = "Complete before deadline"
            deadline = extractDate(combined)?.let { Instant.parse(it) }
        }

        // Meeting keywords
        if (combined.contains("meeting") || combined.contains("call") || combined.contains("zoom") ||
            combined.contains("teams") || combined.contains("calendar invite")) {
            if (category == "OTHER") category = "MEETING"
            importanceScore += 15
            actionRequired = true
            action = "Attend meeting"
        }

        // Document keywords
        if (combined.contains("document") || combined.contains("attach") || combined.contains("resume") ||
            combined.contains("cv") || combined.contains("offer letter") || combined.contains("contract")) {
            if (category == "OTHER") category = "DOCUMENT"
            importanceScore += 10
            actionRequired = true
            action = "Review document"
        }

        // Newsletter/Promotion detection
        if (combined.contains("unsubscribe") || combined.contains("newsletter") || combined.contains("weekly digest")) {
            category = "NEWSLETTER"
            importanceScore -= 20
            confidence = 0.9
        } else if (combined.contains("promotion") || combined.contains("sale") || combined.contains("discount") ||
                   combined.contains("offer") && !combined.contains("job offer") && sender.contains("marketing")) {
            category = "PROMOTION"
            importanceScore -= 30
            confidence = 0.8
        }

        // Personal keywords
        if (combined.contains("family") || combined.contains("friend") || combined.contains("personal") ||
            combined.contains("birthday") || combined.contains("anniversary")) {
            if (category == "OTHER") category = "PERSONAL"
        }

        importanceScore = importanceScore.coerceIn(0, 100)

        return EmailClassification(
            category = category,
            importanceScore = importanceScore,
            summary = generateSummary(email, category),
            actionRequired = actionRequired,
            action = action,
            deadline = deadline,
            confidence = confidence,
            entities = mapper.writeValueAsString(entities),
            modelUsed = "rule-based",
        )
    }

    private fun generateSummary(email: Email, category: String): String {
        val sender = email.sender ?: email.senderEmail ?: "Unknown"
        val subject = email.subject ?: "No subject"
        return when (category) {
            "INTERVIEW" -> "Interview invitation from $sender"
            "JOB" -> "Job opportunity from $sender"
            "TRAVEL" -> "Travel booking: $subject"
            "FINANCE" -> "Financial: $subject"
            "DEADLINE" -> "Deadline: $subject"
            "MEETING" -> "Meeting: $subject"
            "DOCUMENT" -> "Document from $sender"
            else -> subject
        }
    }

    private fun extractDate(text: String): String? {
        val patterns = listOf(
            "\\d{4}-\\d{2}-\\d{2}",
            "\\d{1,2}\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}",
            "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2},?\\s+\\d{4}",
        )
        for (pattern in patterns) {
            val match = pattern.toRegex().find(text)
            if (match != null) {
                return try {
                    parseToISO(match.value)
                } catch (e: Exception) {
                    null
                }
            }
        }
        return null
    }

    private fun parseToISO(dateStr: String): String {
        val formatters = listOf(
            java.time.format.DateTimeFormatter.ISO_LOCAL_DATE,
            java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"),
            java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"),
            java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"),
        )
        for (formatter in formatters) {
            try {
                return java.time.LocalDate.parse(dateStr.trim(), formatter).toString()
            } catch (e: Exception) {
                // try next
            }
        }
        return dateStr
    }

    private fun extractAirline(text: String): String? {
        val airlines = listOf("indigo", "air india", "spicejet", "vistara", "go first", "akasa", "emirates", "qatar", "singapore", "british airways", "lufthansa")
        return airlines.find { text.contains(it) }?.let { it.split(" ").map { it.capitalize() }.joinToString(" ") }
    }

    private fun extractFlightNumber(text: String): String? {
        val regex = "\\b([A-Z]{2}\\d{3,4})\\b".toRegex()
        return regex.find(text)?.value
    }

    private fun extractLocation(text: String, prefix: String): String? {
        val regex = "$prefix\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)".toRegex()
        return regex.find(text)?.groupValues?.get(1)
    }

    private fun extractHotelName(text: String): String? {
        val regex = "(?:hotel|stay at)\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)".toRegex()
        return regex.find(text)?.groupValues?.get(1)
    }

    private fun extractTransactionType(text: String): String? {
        if (text.contains("refund")) return "refund"
        if (text.contains("invoice")) return "invoice"
        if (text.contains("subscription")) return "subscription"
        if (text.contains("salary")) return "salary"
        return "payment"
    }

    private fun extractAmount(text: String): Double? {
        val regex = "(?:\\$|₹|Rs\\.?|USD)\\s*([\\d,]+(?:\\.\\d{2})?)".toRegex()
        return regex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
    }

    private fun truncate(str: String, maxLen: Int): String {
        return if (str.length <= maxLen) str else str.substring(0, maxLen)
    }
}