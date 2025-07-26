#!/bin/bash

# Test script for Worksheet Evaluation API
# This script demonstrates how to use the new worksheet evaluation endpoints

echo "=== Worksheet Evaluation API Test Script ==="
echo ""

# Base URL for the API
BASE_URL="http://localhost:8080/api/worksheet"

echo "1. Testing Health Check Endpoint..."
echo "GET $BASE_URL/health"
curl -X GET "$BASE_URL/health" \
  -H "Content-Type: application/json" \
  -w "\nStatus: %{http_code}\n\n" || echo "Service not running"

echo "2. Testing API Info Endpoint..."
echo "GET $BASE_URL/info"
curl -X GET "$BASE_URL/info" \
  -H "Content-Type: application/json" \
  -w "\nStatus: %{http_code}\n\n" || echo "Service not running"

echo "3. Testing Example Request Format Endpoint..."
echo "GET $BASE_URL/example"
curl -X GET "$BASE_URL/example" \
  -H "Content-Type: application/json" \
  -w "\nStatus: %{http_code}\n\n" || echo "Service not running"

echo ""
echo "=== Sample Usage Examples ==="
echo ""

echo "4. Example: Worksheet Evaluation with JSON Metadata"
echo "POST $BASE_URL/evaluate"
echo "Content-Type: multipart/form-data"
echo ""
echo "Form Data:"
echo "  worksheetFile: [PDF/Image file]"
echo "  metadata: {"
echo "    \"studentName\": \"John Doe\","
echo "    \"studentId\": \"STU001\","
echo "    \"subject\": \"Mathematics\","
echo "    \"worksheetTitle\": \"Algebra Practice Sheet\","
echo "    \"evaluationCriteria\": \"moderate\","
echo "    \"additionalInstructions\": \"Consider partial marks for methodology\","
echo "    \"teacherNotes\": \"Focus on problem-solving approach\""
echo "  }"
echo ""

echo "5. Example: Worksheet Evaluation with Form Data"
echo "POST $BASE_URL/evaluate-form"
echo "Content-Type: multipart/form-data"
echo ""
echo "Form Data:"
echo "  worksheetFile: [PDF/Image file]"
echo "  studentName: John Doe"
echo "  subject: Mathematics"
echo "  worksheetTitle: Algebra Practice Sheet"
echo "  evaluationCriteria: moderate"
echo "  additionalInstructions: Consider partial marks for methodology"
echo "  teacherNotes: Focus on problem-solving approach"
echo ""

echo "=== cURL Examples for Testing ==="
echo ""

echo "# Test with a sample file (replace 'sample-worksheet.pdf' with actual file):"
echo "curl -X POST \"$BASE_URL/evaluate-form\" \\"
echo "  -F \"worksheetFile=@sample-worksheet.pdf\" \\"
echo "  -F \"studentName=John Doe\" \\"
echo "  -F \"subject=Mathematics\" \\"
echo "  -F \"worksheetTitle=Algebra Practice Sheet\" \\"
echo "  -F \"evaluationCriteria=moderate\" \\"
echo "  -F \"additionalInstructions=Consider partial marks for methodology\" \\"
echo "  -F \"teacherNotes=Focus on problem-solving approach\""
echo ""

echo "# Test with JSON metadata:"
echo "curl -X POST \"$BASE_URL/evaluate\" \\"
echo "  -F \"worksheetFile=@sample-worksheet.pdf\" \\"
echo "  -F 'metadata={\"studentName\":\"John Doe\",\"subject\":\"Mathematics\",\"worksheetTitle\":\"Algebra Practice Sheet\",\"evaluationCriteria\":\"moderate\"}'"
echo ""

echo "=== API Documentation ==="
echo ""
echo "Supported File Formats: PDF, JPG, JPEG, PNG"
echo "Maximum File Size: 10MB"
echo "Evaluation Criteria Options: strict, moderate, lenient"
echo ""
echo "Required Fields:"
echo "  - studentName"
echo "  - subject" 
echo "  - worksheetTitle"
echo ""
echo "Optional Fields:"
echo "  - studentId"
echo "  - evaluationCriteria (default: moderate)"
echo "  - additionalInstructions"
echo "  - teacherNotes"
echo ""

echo "=== Response Format ==="
echo ""
echo "Successful Response:"
echo "{"
echo "  \"status\": \"success\","
echo "  \"studentName\": \"John Doe\","
echo "  \"studentId\": \"STU001\","
echo "  \"worksheetTitle\": \"Algebra Practice Sheet\","
echo "  \"subject\": \"Mathematics\","
echo "  \"evaluation\": {"
echo "    \"totalScore\": 85.0,"
echo "    \"maxPossibleScore\": 100.0,"
echo "    \"percentage\": 85.0,"
echo "    \"questionsAnalyzed\": 10,"
echo "    \"questionWiseResults\": [...],"
echo "    \"overallFeedback\": \"...\","
echo "    \"strengths\": [...],"
echo "    \"areasForImprovement\": [...],"
echo "    \"teacherRecommendations\": \"...\""
echo "  },"
echo "  \"processingTime\": \"3.2s\","
echo "  \"timestamp\": \"2025-01-26T15:51:13Z\""
echo "}"
echo ""

echo "=== End of Test Script ==="
