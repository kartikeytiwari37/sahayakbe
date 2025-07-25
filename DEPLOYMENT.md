# Sahayak Backend - Google Cloud Run Deployment Guide

This guide will help you deploy your Sahayak AI Teacher Backend to Google Cloud Run.

## Prerequisites

Before deploying, ensure you have:

1. **Google Cloud Account**: Active Google Cloud account with billing enabled
2. **Google Cloud CLI**: Installed and configured
3. **Docker**: Installed on your local machine
4. **Gemini API Key**: Valid Google Gemini API key

## Setup Instructions

### 1. Install Required Tools

#### Google Cloud CLI
```bash
# For macOS
brew install --cask google-cloud-sdk

# For Ubuntu/Debian
curl https://sdk.cloud.google.com | bash
exec -l $SHELL
```

#### Docker
- Download and install from [Docker's official website](https://docs.docker.com/get-docker/)

### 2. Configure Google Cloud

```bash
# Authenticate with Google Cloud
gcloud auth login

# Set your project (replace YOUR_PROJECT_ID with your actual project ID)
gcloud config set project YOUR_PROJECT_ID

# Enable required APIs
gcloud services enable run.googleapis.com
gcloud services enable containerregistry.googleapis.com
```

### 3. Configure Docker for Google Cloud

```bash
# Configure Docker to use gcloud as a credential helper
gcloud auth configure-docker
```

## Deployment Options

### Option 1: Using the Automated Script (Recommended)

1. **Edit the deployment script**:
   ```bash
   nano deploy.sh
   ```
   
2. **Update the PROJECT_ID**:
   ```bash
   PROJECT_ID="your-google-cloud-project-id"
   ```

3. **Set your Gemini API key**:
   ```bash
   export GEMINI_API_KEY="your-gemini-api-key-here"
   ```

4. **Run the deployment script**:
   ```bash
   ./deploy.sh
   ```

### Option 2: Manual Deployment

1. **Build the Docker image**:
   ```bash
   docker build -t gcr.io/YOUR_PROJECT_ID/sahayak-backend .
   ```

2. **Push to Google Container Registry**:
   ```bash
   docker push gcr.io/YOUR_PROJECT_ID/sahayak-backend
   ```

3. **Deploy to Cloud Run**:
   ```bash
   gcloud run deploy sahayak-backend \
     --image gcr.io/YOUR_PROJECT_ID/sahayak-backend \
     --platform managed \
     --region us-central1 \
     --allow-unauthenticated \
     --memory 1Gi \
     --cpu 1 \
     --max-instances 10 \
     --min-instances 0 \
     --port 8080 \
     --set-env-vars "GEMINI_API_KEY=your-gemini-api-key-here" \
     --timeout 300
   ```

### Option 3: Using YAML Configuration

1. **Update the service configuration**:
   ```bash
   # Edit cloudrun-service.yaml
   # Replace PROJECT_ID and YOUR_GEMINI_API_KEY with actual values
   ```

2. **Deploy using gcloud**:
   ```bash
   gcloud run services replace cloudrun-service.yaml --region us-central1
   ```

## Environment Variables

The application uses the following environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | Server port | 8080 |
| `GEMINI_API_KEY` | Google Gemini API key | Required |

## Health Checks

The application includes health check endpoints:

- **Health Check**: `GET /actuator/health`
- **Info**: `GET /actuator/info`

## WebSocket Configuration

Your WebSocket endpoints will be available at:
- **Main WebSocket**: `wss://your-service-url/sahayak`

## Monitoring and Logs

### View Logs
```bash
# View recent logs
gcloud logs read --project=YOUR_PROJECT_ID --limit=50

# Follow logs in real-time
gcloud logs tail --follow --project=YOUR_PROJECT_ID
```

### Monitor Service
```bash
# Get service details
gcloud run services describe sahayak-backend --region=us-central1

# List all services
gcloud run services list
```

## Scaling Configuration

The service is configured with:
- **Memory**: 1GB
- **CPU**: 1 vCPU
- **Min Instances**: 0 (scales to zero when not in use)
- **Max Instances**: 10
- **Concurrency**: 80 requests per instance
- **Timeout**: 300 seconds

## Security Considerations

1. **API Keys**: Store sensitive keys as environment variables
2. **CORS**: Configure appropriate CORS settings for production
3. **Authentication**: Consider adding authentication for production use
4. **HTTPS**: Cloud Run automatically provides HTTPS

## Troubleshooting

### Common Issues

1. **Build Failures**:
   - Ensure Docker is running
   - Check Dockerfile syntax
   - Verify Maven dependencies

2. **Deployment Failures**:
   - Verify project ID is correct
   - Check API permissions
   - Ensure billing is enabled

3. **Runtime Issues**:
   - Check environment variables
   - Verify Gemini API key
   - Review application logs

### Debug Commands

```bash
# Check service status
gcloud run services describe sahayak-backend --region=us-central1

# View recent logs
gcloud logs read --project=YOUR_PROJECT_ID --service=sahayak-backend

# Test health endpoint
curl https://your-service-url/actuator/health
```

## Cost Optimization

- **Auto-scaling**: Service scales to zero when not in use
- **Resource Limits**: Configured with appropriate CPU/memory limits
- **Request Timeout**: Set to 300 seconds to prevent hanging requests

## Next Steps

After successful deployment:

1. Test your WebSocket connections
2. Configure your frontend to use the new service URL
3. Set up monitoring and alerting
4. Consider setting up CI/CD pipeline for automated deployments

## Support

For issues related to:
- **Google Cloud Run**: [Cloud Run Documentation](https://cloud.google.com/run/docs)
- **Spring Boot**: [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- **WebSocket**: [Spring WebSocket Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket)

---

**Note**: Replace all placeholder values (YOUR_PROJECT_ID, your-gemini-api-key-here, etc.) with your actual values before deployment.
