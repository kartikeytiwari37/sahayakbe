#!/bin/bash

# Simple cURL example for testing the two-document evaluation API
# This demonstrates the /api/worksheet/evaluate-with-question-paper-form endpoint

echo "Testing Two-Document Evaluation API with increased timeouts"
echo "=========================================================="

# Base URL for the API
BASE_URL="http://localhost:8080/api/worksheet"

# Example cURL command for the form-based endpoint
echo "Example cURL command:"
echo ""
echo "curl -X POST \"$BASE_URL/evaluate-with-question-paper-form\" \\"
echo "  -F \"questionPaper=@/path/to/your/question_paper.pdf\" \\"
echo "  -F \"answerSheet=@/path/to/your/answer_sheet.pdf\" \\"
echo "  -F \"studentName=John Doe\" \\"
echo "  -F \"subject=Mathematics\" \\"
echo "  -F \"examTitle=Algebra Test\" \\"
echo "  -F \"evaluationCriteria=moderate\" \\"
echo "  -F \"additionalInstructions=Focus on methodology\" \\"
echo "  -F \"teacherNotes=Student needs help with word problems\""
echo ""

# Example with JSON metadata
echo "Alternative with JSON metadata:"
echo ""
echo "curl -X POST \"$BASE_URL/evaluate-with-question-paper\" \\"
echo "  -F \"questionPaper=@/path/to/your/question_paper.pdf\" \\"
echo "  -F \"answerSheet=@/path/to/your/answer_sheet.pdf\" \\"
echo "  -F 'metadata={\"studentName\":\"John Doe\",\"subject\":\"Mathematics\",\"examTitle\":\"Algebra Test\",\"evaluationCriteria\":\"moderate\"}'"
echo ""

echo "Note: Replace the file paths with actual paths to your PDF/image files"
echo "Timeout Configuration:"
echo "- Connect Timeout: 60 seconds"
echo "- Read Timeout: 5 minutes (300 seconds)"
echo ""
echo "This should resolve the 'Read timed out' error for document processing."
