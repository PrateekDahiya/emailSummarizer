package com.gmailreader.service

import com.gmailreader.dto.AssistantResponse
import com.gmailreader.entity.Email
import com.gmailreader.entity.Event
import com.gmailreader.entity.JobApplication
import com.gmailreader.entity.TravelTrip
import com.gmailreader.entity.User
import com.gmailreader.repository.EmailRepository
import com.gmailreader.repository.EventRepository
import com.gmailreader.repository.JobApplicationRepository
import com.gmailreader.repository.TravelTripRepository
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
class AssistantService(
    private val emailRepository: EmailRepository,
    private val eventRepository: EventRepository,
    private val jobApplicationRepository: JobApplicationRepository,
    private val travelTripRepository: TravelTripRepository,
) {
    private val logger = LoggerFactory.getLogger(AssistantService::class.java)
    private val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    @Value("\${OPENAI_API_KEY:}")
    private var apiKey: String = ""

    @Value("\${app.ai.model:gpt-4o-mini}")
    private var model: String = "gpt-4o-mini"

    fun answerQuestion(user: User, question: String): AssistantResponse {
        if (apiKey.isBlank()) {
            return answerWithLocalData(user, question)
        }

        try {
            return answerWithAI(user, question)
        } catch (e: Exception) {
            logger.warn("AI assistant failed, falling back to local data: {}", e.message)
            return answerWithLocalData(user, question)
        }
    }

    private fun answerWithAI(user: User, question: String): AssistantResponse {
        val context = buildContext(user)
        
        val prompt = """
            You are a helpful email intelligence assistant. Answer the user's question based ONLY on the provided context.
            If the information is not in the context, say "I don't have that information in your emails."
            
            Context:
            $context
            
            User question: $question
            
            Answer concisely and reference specific emails/events when possible.
            """

        val requestBody = mapOf(
            "model" to model,
            "messages" to listOf(
                mapOf("role" to "system", "content" to "You are an email intelligence assistant."),
                mapOf("role" to "user", "content" to prompt)
            ),
            "max_tokens" to 1000,
            "temperature" to 0.1,
        )

        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/chat/completions"))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(requestBody)))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        
        if (response.statusCode() != 200) {
            throw RuntimeException("OpenAI API error: ${response.statusCode()}")
        }

        val jsonResponse = mapper.readTree(response.body())
        val answer = jsonResponse["choices"][0]["message"]["content"].asText()
        
        val sources = extractSources(answer, context)

        return AssistantResponse(answer = answer, sources = sources)
    }

    private fun answerWithLocalData(user: User, question: String): AssistantResponse {
        val lowerQuestion = question.lowercase()
        val gmailAccount = user.gmailAccounts?.firstOrNull()
        
        return when {
            lowerQuestion.contains("interview") -> {
                val interviews = jobApplicationRepository.findByUser(user)
                    .filter { it.status == "INTERVIEW" && it.interviewDate != null }
                    .sortedBy { it.interviewDate }
                
                if (interviews.isEmpty()) {
                    AssistantResponse("You don't have any upcoming interviews scheduled.", emptyList())
                } else {
                    val next = interviews.first()
                    AssistantResponse(
                        "Your next interview is with ${next.company} for ${next.role ?: "a position"} on ${next.interviewDate!!.atZone(java.time.ZoneId.systemDefault()).toLocalDate()} at ${next.interviewDate!!.atZone(java.time.ZoneId.systemDefault()).toLocalTime()}.",
                        listOf("Job application: ${next.company}")
                    )
                }
            }
            lowerQuestion.contains("applied") || lowerQuestion.contains("application") -> {
                val apps = jobApplicationRepository.findByUser(user)
                    .filter { it.status in listOf("APPLIED", "WAITING") }
                
                if (apps.isEmpty()) {
                    AssistantResponse("You don't have any pending job applications.", emptyList())
                } else {
                    AssistantResponse(
                        "You have ${apps.size} pending application${if (apps.size > 1) "s" else ""}: ${apps.map { "${it.company} (${it.role})" }.joinToString(", ")}.",
                        apps.map { "Job application: ${it.company}" }
                    )
                }
            }
            lowerQuestion.contains("trip") || lowerQuestion.contains("travel") || lowerQuestion.contains("flight") -> {
                val trips = travelTripRepository.findUpcomingTripsByUser(user, java.time.LocalDate.now())
                
                if (trips.isEmpty()) {
                    AssistantResponse("You don't have any upcoming trips.", emptyList())
                } else {
                    val next = trips.first()
                    AssistantResponse(
                        "Your next trip is to ${next.destination} from ${next.startDate} to ${next.endDate}.",
                        listOf("Trip: ${next.name}")
                    )
                }
            }
            lowerQuestion.contains("today") || lowerQuestion.contains("tomorrow") || lowerQuestion.contains("this week") -> {
                val now = Instant.now()
                val events = eventRepository.findUpcomingEventsByUser(user, now).take(5)
                
                if (events.isEmpty()) {
                    AssistantResponse("You don't have any events scheduled for today.", emptyList())
                } else {
                    val eventList = events.map { "- ${it.title} at ${it.startTime.atZone(java.time.ZoneId.systemDefault()).toLocalTime()}" }.joinToString("\n")
                    AssistantResponse("Here's what you have coming up:\n$eventList", events.map { "Event: ${it.title}" })
                }
            }
            lowerQuestion.contains("recruiter") || lowerQuestion.contains("reply") -> {
                val recentApps = jobApplicationRepository.findByUserOrderByUpdatedAtDesc(user).take(5)
                val replies = recentApps.filter { it.updatedAt != null && it.updatedAt!! > Instant.now().minusSeconds(86400) }
                
                if (replies.isEmpty()) {
                    AssistantResponse("No recruiter replies in the last 24 hours.", emptyList())
                } else {
                    AssistantResponse(
                        "Recent recruiter activity: ${replies.map { "${it.company} - ${it.status}" }.joinToString(", ")}.",
                        replies.map { "Job application: ${it.company}" }
                    )
                }
            }
            else -> {
                AssistantResponse(
                    "I can help you with questions about your interviews, job applications, upcoming trips, and schedule. Try asking: 'When is my next interview?', 'What job applications are pending?', 'What trips do I have coming up?', or 'What do I have today?'",
                    emptyList()
                )
            }
        }
    }

    private fun buildContext(user: User): String {
        val gmailAccount = user.gmailAccounts?.firstOrNull() ?: return "No Gmail account connected."
        
        val recentEmails = emailRepository.findTop50ByGmailAccountOrderByReceivedAtDesc(gmailAccount!!)
            .filter { it.isProcessed }
            .take(20)
        
        val jobs = jobApplicationRepository.findByUserOrderByUpdatedAtDesc(user).take(10)
        val trips = travelTripRepository.findUpcomingTripsByUser(user, java.time.LocalDate.now()).take(5)
        val events = eventRepository.findUpcomingEventsByUser(user, Instant.now()).take(10)

        val emailContext = recentEmails.map { email ->
            val c = email.classification
            "- ${email.sender}: ${email.subject} [${c?.category ?: "UNCLASSIFIED"}] ${c?.summary ?: ""}"
        }.joinToString("\n")

        val jobContext = jobs.map { "- ${it.company} - ${it.role ?: "N/A"} - ${it.status} ${it.interviewDate?.let { " (Interview: $it)" } ?: "" }" }.joinToString("\n")
        val tripContext = trips.map { "- ${it.name}: ${it.destination} (${it.startDate} to ${it.endDate})" }.joinToString("\n")
        val eventContext = events.map { "- ${it.title} at ${it.startTime} (${it.type})" }.joinToString("\n")

        return """
            Recent Emails:
            $emailContext

            Job Applications:
            $jobContext

            Upcoming Trips:
            $tripContext

            Upcoming Events:
            $eventContext
        """.trimIndent()
    }

    private fun extractSources(answer: String, context: String): List<String> {
        val sources = mutableListOf<String>()
        // Simple extraction - in production, you'd want more sophisticated parsing
        context.lines().forEach { line ->
            if (line.isNotBlank() && answer.lowercase().contains(line.substring(0, min(20, line.length)).lowercase())) {
                sources.add(line)
            }
        }
        return sources.distinct().take(5)
    }
}