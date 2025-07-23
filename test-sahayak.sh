#!/bin/bash

# Sahayak Backend Test Commands
SERVICE_URL="https://sahayak-backend-199913799544.us-central1.run.app"

echo "🧪 Testing Sahayak Backend API..."
echo "================================"

echo -e "\n1️⃣ Health Check:"
echo "curl $SERVICE_URL/actuator/health"
curl -s "$SERVICE_URL/actuator/health" | jq '.' || curl -s "$SERVICE_URL/actuator/health"

echo -e "\n\n2️⃣ Application Info:"
echo "curl $SERVICE_URL/actuator/info"
curl -s "$SERVICE_URL/actuator/info" | jq '.' || curl -s "$SERVICE_URL/actuator/info"

echo -e "\n\n3️⃣ Test WebSocket Connection (using wscat if available):"
echo "wscat -c wss://sahayak-backend-199913799544.us-central1.run.app/sahayak"

echo -e "\n\n4️⃣ Test with curl WebSocket upgrade request:"
echo "curl -i -N -H 'Connection: Upgrade' -H 'Upgrade: websocket' -H 'Sec-WebSocket-Version: 13' -H 'Sec-WebSocket-Key: x3JJHMbDL1EzLkh9GBhXDw==' $SERVICE_URL/sahayak"
curl -i -N -H "Connection: Upgrade" -H "Upgrade: websocket" -H "Sec-WebSocket-Version: 13" -H "Sec-WebSocket-Key: x3JJHMbDL1EzLkh9GBhXDw==" "$SERVICE_URL/sahayak"

echo -e "\n\n5️⃣ Check if any other endpoints are available:"
echo "curl $SERVICE_URL/"
curl -s "$SERVICE_URL/"

echo -e "\n\n✅ Testing complete!"
