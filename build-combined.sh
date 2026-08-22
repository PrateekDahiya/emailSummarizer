# Build both frontend and backend for combined deployment
# Run this script from the project root

set -e  # Exit on error

echo "🔨 Building combined application..."

# 1. Build frontend
echo "📦 Building Next.js frontend..."
cd frontend
npm ci
npm run build

# 2. Copy frontend build to Spring Boot static resources
echo "📁 Copying frontend build to backend static resources..."
rm -rf ../backend/src/main/resources/static
mkdir -p ../backend/src/main/resources/static
cp -r .next/server/app/* ../backend/src/main/resources/static/
cp -r public/* ../backend/src/main/resources/static/ 2>/dev/null || true

# 3. Create a special index.html that works with Spring Boot routing
cat > ../backend/src/main/resources/static/index.html << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>Gmail Intelligence Dashboard</title>
    <script>
        // Handle client-side routing for Spring Boot
        (function() {
            var path = window.location.pathname;
            if (path !== '/' && !path.startsWith('/api') && !path.startsWith('/_next') && !path.includes('.')) {
                // Rewrite to index.html for client-side routing
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

# 4. Build backend with frontend included
echo "☕ Building Spring Boot backend..."
cd ../backend
./mvnw clean package -DskipTests

echo "✅ Combined build complete!"
echo "📦 JAR location: backend/target/gmail-reader-backend-1.0.0-SNAPSHOT.jar"
echo ""
echo "🚀 To run: java -jar backend/target/gmail-reader-backend-1.0.0-SNAPSHOT.jar"
echo "🌐 Access: http://localhost:8080"