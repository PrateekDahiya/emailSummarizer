# Environment Variables Setup Guide

## Quick Start

### 1. Generate Secrets
```bash
# NextAuth secret (frontend)
openssl rand -base64 32

# JWT secret (backend) 
openssl rand -base64 32
```

### 2. Google Cloud Console Setup
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create project or select existing
3. **Enable APIs**: Gmail API, OAuth2 API
4. **OAuth Consent Screen**: 
   - User Type: External
   - Scopes: `openid`, `email`, `profile`, `https://www.googleapis.com/auth/gmail.readonly`
5. **Credentials** → Create OAuth 2.0 Client ID:
   - Application type: Web application
   - Authorized redirect URIs: `http://localhost:3000/api/auth/callback`
6. Copy **Client ID** and **Client Secret**

### 3. Frontend (.env.local)
```env
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=<generated-secret>
GOOGLE_CLIENT_ID=<from-google-console>
GOOGLE_CLIENT_SECRET=<from-google-console>
NEXT_PUBLIC_API_URL=http://localhost:8080
```

### 4. Backend (.env)
```env
DB_USERNAME=postgres
DB_PASSWORD=postgres
DB_URL=jdbc:postgresql://localhost:5432/gmail_reader

GOOGLE_CLIENT_ID=<same-as-frontend>
GOOGLE_CLIENT_SECRET=<same-as-frontend>
GOOGLE_REDIRECT_URI=http://localhost:3000/api/auth/callback

JWT_SECRET=<generated-secret>
OPENAI_API_KEY=sk-xxx  # Optional
FRONTEND_URL=http://localhost:3000
```

### 5. Start Services
```bash
# Start PostgreSQL
docker-compose up -d

# Backend
cd backend && ./mvnw spring-boot:run

# Frontend  
cd frontend && npm install && npm run dev
```

## Required Variables Checklist

| Variable | Source | Required | Description |
|----------|--------|----------|-------------|
| `NEXTAUTH_SECRET` | `openssl rand -base64 32` | ✅ | NextAuth.js encryption key |
| `NEXTAUTH_URL` | Fixed | ✅ | Frontend URL (http://localhost:3000) |
| `GOOGLE_CLIENT_ID` | Google Cloud Console | ✅ | OAuth client ID |
| `GOOGLE_CLIENT_SECRET` | Google Cloud Console | ✅ | OAuth client secret |
| `GOOGLE_REDIRECT_URI` | Fixed | ✅ | Must match Google Console: `http://localhost:3000/api/auth/callback` |
| `JWT_SECRET` | `openssl rand -base64 32` | ✅ | Backend JWT signing key (256-bit) |
| `NEXT_PUBLIC_API_URL` | Fixed | ✅ | Backend URL (http://localhost:8080) |
| `DB_USERNAME` | Fixed | ✅ | PostgreSQL username (postgres) |
| `DB_PASSWORD` | Fixed | ✅ | PostgreSQL password (postgres) |
| `DB_URL` | Fixed | ✅ | PostgreSQL JDBC URL |
| `OPENAI_API_KEY` | OpenAI Platform | ❌ | Optional - enables AI classification |
| `FRONTEND_URL` | Fixed | ✅ | For CORS (http://localhost:3000) |

## Google OAuth Scopes Required
```
openid
email
profile
https://www.googleapis.com/auth/gmail.readonly
```

## OAuth Consent Screen Notes
- App name: "Gmail Intelligence Dashboard"
- User type: External (or Internal for Workspace)
- Authorized domains: `localhost` (for dev)
- Privacy policy URL: Required for production

## Production Changes
- Change `NEXTAUTH_URL` to your domain
- Change `GOOGLE_REDIRECT_URI` to `https://yourdomain.com/api/auth/callback`
- Use strong unique secrets
- Enable HTTPS
- Set `DB_PASSWORD` to secure value
- Add `NODE_ENV=production`