#!/bin/bash

# Sahayak Backend - Google Cloud Run Deployment Script

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
PROJECT_ID=""
REGION="us-central1"
SERVICE_NAME="sahayak-backend"
IMAGE_NAME="gcr.io/${PROJECT_ID}/${SERVICE_NAME}"

echo -e "${GREEN}🚀 Sahayak Backend - Google Cloud Run Deployment${NC}"
echo "=================================================="

# Check if PROJECT_ID is set
if [ -z "$PROJECT_ID" ]; then
    echo -e "${RED}❌ Error: PROJECT_ID is not set${NC}"
    echo "Please edit this script and set your Google Cloud Project ID"
    exit 1
fi

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}❌ Error: gcloud CLI is not installed${NC}"
    echo "Please install Google Cloud CLI: https://cloud.google.com/sdk/docs/install"
    exit 1
fi

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Error: Docker is not installed${NC}"
    echo "Please install Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

echo -e "${YELLOW}📋 Pre-deployment checklist:${NC}"
echo "1. Make sure you have set your GEMINI_API_KEY and EXAM_CREATION_GEMINI_API_KEY"
echo "2. Update PROJECT_ID in this script"
echo "3. Ensure you're authenticated with gcloud"
echo ""

read -p "Continue with deployment? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Deployment cancelled."
    exit 1
fi

echo -e "${YELLOW}🔧 Setting up Google Cloud configuration...${NC}"
gcloud config set project $PROJECT_ID

echo -e "${YELLOW}🐳 Building Docker image...${NC}"
docker build -t $IMAGE_NAME .

echo -e "${YELLOW}📤 Pushing image to Google Container Registry...${NC}"
docker push $IMAGE_NAME

echo -e "${YELLOW}🚀 Deploying to Cloud Run...${NC}"
gcloud run deploy $SERVICE_NAME \
    --image $IMAGE_NAME \
    --platform managed \
    --region $REGION \
    --allow-unauthenticated \
    --memory 1Gi \
    --cpu 1 \
    --max-instances 10 \
    --min-instances 0 \
    --port 8080 \
    --set-env-vars "GEMINI_API_KEY=${GEMINI_API_KEY},EXAM_CREATION_GEMINI_API_KEY=${EXAM_CREATION_GEMINI_API_KEY}" \
    --timeout 300

echo -e "${GREEN}✅ Deployment completed successfully!${NC}"
echo ""
echo -e "${YELLOW}📝 Next steps:${NC}"
echo "1. Your service URL will be displayed above"
echo "2. Test your WebSocket endpoints"
echo "3. Monitor logs with: gcloud logs tail --follow --project=$PROJECT_ID"
echo ""
echo -e "${GREEN}🎉 Sahayak Backend is now running on Google Cloud Run!${NC}"
