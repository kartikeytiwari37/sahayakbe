#!/bin/bash

# Test script for the new two-document evaluation API
# This script tests the new /evaluate-with-question-paper endpoint

echo "Testing Two-Document Worksheet Evaluation API"
echo "============================================="

# Base URL for the API
BASE_URL="http://localhost:8080/api/worksheet"

# Test 1: Check API info to see new endpoints
echo "Test 1: Checking API info for new endpoints..."
curl -X GET "$BASE_URL/info" \
  -H "Content-Type: application/json" \
  | jq '.'

echo -e "\n\n"

# Test 2: Check example endpoint for new API format
echo "Test 2: Checking example endpoint for new API format..."
curl -X GET "$BASE_URL/example" \
  -H "Content-Type: application/json" \
  | jq '.'

echo -e "\n\n"

# Test 3: Test the new two-document endpoint with form data (easier to test)
echo "Test 3: Testing two-document evaluation with form data..."
echo "Note: This test requires actual PDF/image files to work properly"
echo "Creating sample test files..."

# Create a simple test text file (in real usage, these would be PDF/image files)
echo "Sample Question Paper Content" > /tmp/question_paper.txt
echo "Sample Answer Sheet Content" > /tmp/answer_sheet.txt

# Test the form-based endpoint
curl -X POST "$BASE_URL/evaluate-with-question-paper-form" \
  -F "questionPaper=@/tmp/question_paper.txt" \
  -F "answerSheet=@/tmp/answer_sheet.txt" \
  -F "studentName=Test Student" \
  -F "subject=Mathematics" \
  -F "examTitle=Sample Test" \
  -F "evaluationCriteria=moderate" \
  -F "additionalInstructions=This is a test" \
  -F "teacherNotes=Sample notes" \
  | jq '.'

echo -e "\n\n"

# Test 4: Test health endpoint
echo "Test 4: Testing health endpoint..."
curl -X GET "$BASE_URL/health" \
  -H "Content-Type: application/json" \
  | jq '.'

echo -e "\n\n"

# Clean up test files
rm -f /tmp/question_paper.txt /tmp/answer_sheet.txt

echo "Testing completed!"
echo ""
echo "Note: For full testing with actual documents, use PDF or image files"
echo "Example usage with real files:"
echo "curl -X POST \"$BASE_URL/evaluate-with-question-paper-form\" \\"
echo "  -F \"questionPaper=@/path/to/question_paper.pdf\" \\"
echo "  -F \"answerSheet=@/path/to/answer_sheet.pdf\" \\"
echo "  -F \"studentName=John Doe\" \\"
echo "  -F \"subject=Physics\" \\"
echo "  -F \"examTitle=Midterm Exam\" \\"
echo "  -F \"evaluationCriteria=moderate\""
