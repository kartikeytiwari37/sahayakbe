#!/bin/bash

# Sahayak Backend Test Commands - Correct Endpoints
SERVICE_URL="https://sahayak-backend-199913799544.us-central1.run.app"

echo "🧪 Testing Sahayak Backend API with Correct Endpoints..."
echo "======================================================="

echo -e "\n1️⃣ Health Check (Actuator):"
echo "Command: curl $SERVICE_URL/actuator/health"
curl -s "$SERVICE_URL/actuator/health" | jq '.' || curl -s "$SERVICE_URL/actuator/health"

echo -e "\n\n2️⃣ API Health Check:"
echo "Command: curl $SERVICE_URL/api/sahayak/health"
curl -s "$SERVICE_URL/api/sahayak/health" | jq '.' || curl -s "$SERVICE_URL/api/sahayak/health"

echo -e "\n\n3️⃣ Get All Sessions:"
echo "Command: curl $SERVICE_URL/api/sahayak/teacher/sessions"
curl -s "$SERVICE_URL/api/sahayak/teacher/sessions" | jq '.' || curl -s "$SERVICE_URL/api/sahayak/teacher/sessions"

echo -e "\n\n4️⃣ Create a Teacher Session:"
echo "Command: curl -X POST $SERVICE_URL/api/sahayak/teacher/session"
SESSION_RESPONSE=$(curl -s -X POST "$SERVICE_URL/api/sahayak/teacher/session")
echo "$SESSION_RESPONSE" | jq '.' || echo "$SESSION_RESPONSE"

# Extract session ID if available
SESSION_ID=$(echo "$SESSION_RESPONSE" | jq -r '.sessionId' 2>/dev/null || echo "")

if [ ! -z "$SESSION_ID" ] && [ "$SESSION_ID" != "null" ]; then
    echo -e "\n\n5️⃣ Check Session Status:"
    echo "Command: curl $SERVICE_URL/api/sahayak/teacher/session/$SESSION_ID/status"
    curl -s "$SERVICE_URL/api/sahayak/teacher/session/$SESSION_ID/status" | jq '.' || curl -s "$SERVICE_URL/api/sahayak/teacher/session/$SESSION_ID/status"
    
    echo -e "\n\n6️⃣ Send Text Message to Session:"
    echo "Command: curl -X POST -H 'Content-Type: application/json' -d '{\"text\":\"Hello, Sahayak!\"}' $SERVICE_URL/api/sahayak/teacher/session/$SESSION_ID/text"
    curl -s -X POST -H "Content-Type: application/json" -d '{"text":"Hello, Sahayak!"}' "$SERVICE_URL/api/sahayak/teacher/session/$SESSION_ID/text" | jq '.' || curl -s -X POST -H "Content-Type: application/json" -d '{"text":"Hello, Sahayak!"}' "$SERVICE_URL/api/sahayak/teacher/session/$SESSION_ID/text"
fi

echo -e "\n\n7️⃣ WebSocket Endpoint Info (SockJS):"
echo "The WebSocket endpoint is available at:"
echo "- SockJS URL: $SERVICE_URL/sahayak-teacher"
echo "- WebSocket URL: wss://sahayak-backend-199913799544.us-central1.run.app/sahayak-teacher/websocket"
echo ""
echo "To test WebSocket connection, you can use:"
echo "1. A SockJS client library in your frontend"
echo "2. wscat with: wscat -c 'wss://sahayak-backend-199913799544.us-central1.run.app/sahayak-teacher/websocket'"

echo -e "\n\n8️⃣ Test SockJS Info Endpoint:"
echo "Command: curl $SERVICE_URL/sahayak-teacher/info"
curl -s "$SERVICE_URL/sahayak-teacher/info" || echo "SockJS info endpoint may not be accessible directly"

echo -e "\n\n✅ Testing complete!"
echo ""
echo "📝 Quick Reference - Main Endpoints:"
echo "- Health: GET /actuator/health"
echo "- API Health: GET /api/sahayak/health"
echo "- Create Session: POST /api/sahayak/teacher/session"
echo "- Get Sessions: GET /api/sahayak/teacher/sessions"
echo "- Session Status: GET /api/sahayak/teacher/session/{sessionId}/status"
echo "- Send Text: POST /api/sahayak/teacher/session/{sessionId}/text"
echo "- WebSocket: /sahayak-teacher (SockJS)"
