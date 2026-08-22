<# 
.SYNOPSIS
    Build both frontend and backend for combined deployment
.DESCRIPTION
    This script builds the Next.js frontend and copies it to Spring Boot static resources,
    then builds the combined JAR file.
.NOTES
    Run this script from the project root directory in PowerShell
#>

param(
    [switch]$SkipTests = $true
)

Write-Host "🔨 Building combined application..." -ForegroundColor Green

# 1. Build frontend
Write-Host "📦 Building Next.js frontend..." -ForegroundColor Cyan
Set-Location frontend
if (-not (Test-Path "node_modules")) {
    Write-Host "📥 Installing npm dependencies..." -ForegroundColor Yellow
    npm ci
}
npm run build

# 2. Copy frontend build to Spring Boot static resources
Write-Host "📁 Copying frontend build to backend static resources..." -ForegroundColor Cyan
$staticPath = "../backend/src/main/resources/static"
if (Test-Path $staticPath) {
    Remove-Item -Recurse -Force $staticPath
}
New-Item -ItemType Directory -Path $staticPath | Out-Null

# Copy Next.js build output
Copy-Item -Path ".next/server/app/*" -Destination "$staticPath/" -Recurse -Force
if (Test-Path "public") {
    Copy-Item -Path "public/*" -Destination "$staticPath/" -Recurse -Force -ErrorAction SilentlyContinue
}

# 3. Create index.html for SPA routing
Write-Host "📝 Creating SPA index.html..." -ForegroundColor Cyan
$indexHtml = @"
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
"@
Set-Content -Path "$staticPath/index.html" -Value $indexHtml -Encoding UTF8

# 4. Build backend with frontend included
Write-Host "☕ Building Spring Boot backend..." -ForegroundColor Cyan
Set-Location ../backend
$mvnArgs = @("clean", "package")
if ($SkipTests) { $mvnArgs += "-DskipTests" }
./mvnw @mvnArgs

Write-Host "✅ Combined build complete!" -ForegroundColor Green
Write-Host "📦 JAR location: backend/target/gmail-reader-backend-1.0.0-SNAPSHOT.jar"
Write-Host ""
Write-Host "🚀 To run: java -jar backend/target/gmail-reader-backend-1.0.0-SNAPSHOT.jar"
Write-Host "🌐 Access: http://localhost:8080"