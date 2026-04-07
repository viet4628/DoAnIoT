#!/bin/bash

# Smart Home IoT - Deploy Script
# Build and deploy backend + start all services

set -e

echo "🚀 Starting Smart Home IoT Deployment..."

# 1. Build backend
echo "📦 Building backend..."
cd backend
./mvnw clean package -DskipTests
cd ..

# 2. Start all services with Docker Compose
echo "🐳 Starting Docker services..."
docker-compose up -d --build

# 3. Wait for services to be ready
echo "⏳ Waiting for services to start..."
sleep 10

# 4. Check service status
echo ""
echo "📊 Service Status:"
docker-compose ps

# 5. Test backend API
echo ""
echo "🧪 Testing Backend API..."
if curl -s http://localhost/api/actuator/health > /dev/null; then
    echo "✅ Backend is UP"
else
    echo "❌ Backend is DOWN"
fi

echo ""
echo "✨ Deployment Complete!"
echo ""
echo "📍 Access Points:"
echo "   • Backend API: http://localhost/api"
echo "   • MQTT Broker: localhost:1883"
echo "   • MQTTX Web: http://localhost:8888"
echo ""
echo "📱 To run Flutter app:"
echo "   cd smart_home_app && flutter run"
echo ""
echo "🛑 To stop all services:"
echo "   docker-compose down"
