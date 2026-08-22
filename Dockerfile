# Multi-stage build for combined Spring Boot + Next.js application

# Stage 1: Build Frontend
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# Stage 2: Build Backend with Frontend
FROM maven:3.9-eclipse-temurin-21 AS backend-builder
WORKDIR /app/backend

# Copy Maven files
COPY backend/pom.xml .
COPY backend/src/main/resources/db/migration ./src/main/resources/db/migration

# Copy frontend build to static resources
COPY --from=frontend-builder /app/frontend/.next/server/app/ ./src/main/resources/static/
COPY --from=frontend-builder /app/frontend/public/ ./src/main/resources/static/

# Create index.html for SPA routing
RUN cat > ./src/main/resources/static/index.html << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>Gmail Intelligence Dashboard</title>
    <script>
        (function() {
            var path = window.location.pathname;
            if (path !== '/' && !path.startsWith('/api') && !path.startsWith('/_next') && !path.includes('.')) {
                window.location.href = '/' + '#' + path.slice(1);
            }
        })();
    </script>
</head>
<body>
    <div id="__next"></div>
    <script src="/_next/static/chunks/main.js" defer></script>
    <script src="/_next/static/chunks/webpack.js" defer></script>
    <script src="/_next/static/chunks/framework.js" defer></script>
    <script src="/_next/static/chunks/pages/_app.js" defer></script>
    <script src="/_next/static/chunks/pages/index.js" defer></script>
</body>
</html>
EOF

# Build backend (use mvn from maven image)
COPY backend/src ./src
RUN mvn clean package -DskipTests

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install dumb-init for proper signal handling
RUN apk add --no-cache dumb-init

# Create non-root user
RUN addgroup -g 1000 -S appgroup && \
    adduser -u 1000 -S appuser -G appgroup

# Copy built JAR
COPY --from=backend-builder /app/backend/target/gmail-reader-backend-1.0.0-SNAPSHOT.jar app.jar

# Change ownership
RUN chown appuser:appgroup app.jar

USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1

# Run with dumb-init
ENTRYPOINT ["dumb-init", "--"]
CMD ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]