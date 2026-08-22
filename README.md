# Gmail Intelligence Dashboard

An AI-powered personal email intelligence dashboard that connects to Gmail, understands every email, extracts actionable information, and organizes it into things you actually care about.

## Features

- **Gmail Integration**: OAuth 2.0 with read-only Gmail API access
- **Email Intelligence**: AI-powered classification, summarization, and entity extraction
- **Smart Dashboard**: Organized views for Jobs, Travel, Events, and Important items
- **Job Application Tracker**: Automatic detection and tracking of job applications
- **Travel Intelligence**: Flight, hotel, and trip organization
- **AI Assistant**: Natural language queries about your emails
- **Importance Scoring**: Rule-based + AI scoring system
- **Incremental Sync**: Efficient Gmail synchronization

## Tech Stack

### Frontend
- Next.js 14 (App Router)
- TypeScript
- Tailwind CSS
- NextAuth.js for authentication
- Lucide React for icons

### Backend
- Kotlin + Spring Boot 3.2
- PostgreSQL with Flyway migrations
- Gmail API integration
- OpenAI API for AI processing
- JWT authentication

## Project Structure

```
gmailReader/
├── frontend/                 # Next.js frontend
│   ├── src/
│   │   ├── app/             # App Router pages & API routes
│   │   ├── components/      # React components
│   │   ├── hooks/           # Custom React hooks
│   │   ├── lib/             # Utilities & API client
│   │   └── types/           # TypeScript types
│   └── ...
├── backend/                  # Spring Boot backend
│   ├── src/main/kotlin/com/gmailreader/
│   │   ├── controller/      # REST controllers
│   │   ├── entity/          # JPA entities
│   │   ├── repository/      # Spring Data repositories
│   │   ├── service/         # Business logic
│   │   ├── dto/             # Data transfer objects
│   │   ├── security/        # Security config
│   │   └── config/          # Configuration
│   └── src/main/resources/
│       └── db/migration/    # Flyway SQL migrations
└── docker-compose.yml       # Development services
```

## Getting Started

### Prerequisites
- Node.js 20+
- Java 21+
- Maven 3.9+
- PostgreSQL 16+
- Google Cloud Console project with Gmail API enabled

### Environment Setup

1. **Clone the repository**
```bash
git clone https://github.com/PrateekDahiya/emailSummarizer.git
cd emailSummarizer
```

2. **Start PostgreSQL and Redis**
```bash
docker-compose up -d
```

3. **Configure Frontend**
```bash
cd frontend
cp .env.local.example .env.local
# Edit .env.local with your credentials
```

4. **Configure Backend**
```bash
cd backend
# Set environment variables or create application-local.yml
export GOOGLE_CLIENT_ID=your-client-id
export GOOGLE_CLIENT_SECRET=your-client-secret
export GOOGLE_REDIRECT_URI=http://localhost:3000/api/auth/callback
export JWT_SECRET=your-super-secret-jwt-key-min-256-bits
export OPENAI_API_KEY=your-openai-key (optional)
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
```

5. **Run Backend**
```bash
cd backend
./mvnw spring-boot:run
```

6. **Run Frontend**
```bash
cd frontend
npm install
npm run dev
```

7. **Access the application**
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui.html

## Google OAuth Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable Gmail API
4. Create OAuth 2.0 credentials
5. Add authorized redirect URI: `http://localhost:3000/api/auth/callback`
6. Configure OAuth consent screen with scopes:
   - `openid`
   - `email`
   - `profile`
   - `https://www.googleapis.com/auth/gmail.readonly`

## Database Schema

Key tables:
- `users` - User accounts linked to Google
- `gmail_accounts` - Connected Gmail accounts
- `emails` - Stored email messages
- `email_classifications` - AI classification results
- `job_applications` - Tracked job applications
- `travel_trips` - Organized travel itineraries
- `flights` / `hotels` - Travel components
- `events` - Calendar events from emails
- `sync_logs` - Synchronization history

## API Endpoints

### Authentication
- `GET /api/auth/google/url` - Get Google OAuth URL
- `POST /api/auth/google/callback` - Handle OAuth callback
- `GET /api/auth/me` - Get current user

### Dashboard
- `GET /api/dashboard` - Get dashboard data
- `GET /api/dashboard/sync/status` - Get sync status
- `POST /api/dashboard/sync/trigger` - Trigger incremental sync
- `POST /api/dashboard/sync/initial` - Trigger initial sync

### Emails
- `GET /api/emails` - List emails with pagination
- `GET /api/emails/{id}` - Get email details
- `POST /api/emails/{id}/reclassify` - Reclassify email

### Jobs
- `GET /api/jobs` - List job applications

### Travel
- `GET /api/travel/trips` - List travel trips

### Search
- `GET /api/search?q={query}` - Search emails

### Assistant
- `POST /api/assistant/ask` - Ask AI assistant

## Development

### Frontend Commands
```bash
cd frontend
npm run dev      # Development server
npm run build    # Production build
npm run start    # Production server
npm run lint     # ESLint
```

### Backend Commands
```bash
cd backend
./mvnw spring-boot:run     # Run application
./mvnw test                # Run tests
./mvnw clean package       # Build JAR
./mvnw flyway:migrate      # Run migrations
```

## Deployment

### Backend
```bash
./mvnw clean package -DskipTests
java -jar target/gmail-reader-backend-1.0.0-SNAPSHOT.jar
```

### Frontend
```bash
npm run build
npm run start
```

## Security Considerations

- Read-only Gmail permissions
- JWT tokens with short expiration
- Encrypted token storage
- Rate limiting on API endpoints
- Input validation and sanitization
- Secure headers via Spring Security

## License

MIT License - see LICENSE file for details.