# Two-Document Worksheet Evaluation API

This document describes the new two-document evaluation API that has been added to the WorksheetController. This API allows for more accurate evaluation by accepting separate question paper and answer sheet documents.

## Overview

The new API follows a two-step process:
1. **Question Paper Analysis**: Uses Gemini to parse and digitize the question paper, extracting structured information about questions, scoring rubrics, and point values.
2. **Answer Sheet Evaluation**: Uses the structured question data to evaluate the student's answer sheet with higher accuracy.

## New Endpoints

### 1. `/api/worksheet/evaluate-with-question-paper`

**Method**: POST  
**Content-Type**: multipart/form-data

**Parameters**:
- `questionPaper` (MultipartFile) - The question paper document (PDF, JPG, PNG) - **Required**
- `answerSheet` (MultipartFile) - The student's answer sheet document (PDF, JPG, PNG) - **Required**
- `metadata` (String) - JSON string containing evaluation metadata - **Required**

**Metadata JSON Structure**:
```json
{
  "studentName": "Jane Smith",
  "studentId": "STU002",
  "subject": "Physics",
  "examTitle": "Mechanics Test",
  "evaluationCriteria": "moderate",
  "additionalInstructions": "Focus on conceptual understanding",
  "teacherNotes": "Student has been struggling with force diagrams"
}
```

### 2. `/api/worksheet/evaluate-with-question-paper-form`

**Method**: POST  
**Content-Type**: multipart/form-data

**Parameters**:
- `questionPaper` (MultipartFile) - The question paper document - **Required**
- `answerSheet` (MultipartFile) - The student's answer sheet document - **Required**
- `studentName` (String) - Student's name - **Required**
- `subject` (String) - Subject of the exam - **Required**
- `examTitle` (String) - Title of the exam - **Required**
- `studentId` (String) - Student's ID - *Optional*
- `evaluationCriteria` (String) - Evaluation criteria (strict|moderate|lenient) - *Optional* (default: moderate)
- `additionalInstructions` (String) - Additional instructions - *Optional*
- `teacherNotes` (String) - Teacher notes - *Optional*

## New Model Classes

### QuestionPaperEvaluationRequest
Similar to `WorksheetEvaluationRequest` but uses `examTitle` instead of `worksheetTitle` to better reflect the two-document nature.

### QuestionPaperAnalysisResult
Represents the structured output from question paper analysis:
```json
{
  "examTitle": "string",
  "subject": "string",
  "totalMarks": 100.0,
  "totalQuestions": 10,
  "instructions": "string",
  "questions": [
    {
      "questionNumber": 1,
      "questionText": "What is the acceleration due to gravity?",
      "questionType": "SHORT_ANSWER",
      "marks": 5.0,
      "correctAnswer": "9.8 m/s²",
      "scoringRubric": "Award full marks for correct value with units",
      "keywords": ["9.8", "m/s²", "gravity"]
    }
  ],
  "status": "success"
}
```

## Process Flow

### Step 1: Question Paper Analysis
1. Validates and processes the question paper file
2. Creates a specialized prompt for Gemini to extract structured question data
3. Calls Gemini 2.5 Pro to analyze the document
4. Parses the response into a `QuestionPaperAnalysisResult` object
5. Extracts:
   - Question numbers and text
   - Question types (MCQ, SHORT_ANSWER, ESSAY, TRUE_FALSE, FILL_BLANK)
   - Point values for each question
   - Correct answers (for objective questions)
   - Scoring rubrics (for subjective questions)
   - Expected keywords for partial scoring

### Step 2: Answer Sheet Evaluation
1. Uses the structured question data from Step 1
2. Creates an evaluation prompt that includes the parsed question information
3. Calls Gemini 2.5 Pro to evaluate the answer sheet against the structured questions
4. Returns the same `WorksheetEvaluationResponse` format as the original API

## Benefits

1. **Higher Accuracy**: Separate analysis of question paper provides more precise evaluation criteria
2. **Better Scoring**: Structured question data enables more consistent scoring
3. **Detailed Rubrics**: AI can create specific scoring rubrics for each question
4. **Keyword Matching**: Enables partial credit based on expected keywords
5. **Question Type Awareness**: Different evaluation strategies for different question types

## Usage Examples

### Using cURL with Form Data
```bash
curl -X POST "http://localhost:8080/api/worksheet/evaluate-with-question-paper-form" \
  -F "questionPaper=@/path/to/question_paper.pdf" \
  -F "answerSheet=@/path/to/answer_sheet.pdf" \
  -F "studentName=John Doe" \
  -F "subject=Physics" \
  -F "examTitle=Midterm Exam" \
  -F "evaluationCriteria=moderate"
```

### Using cURL with JSON Metadata
```bash
curl -X POST "http://localhost:8080/api/worksheet/evaluate-with-question-paper" \
  -F "questionPaper=@/path/to/question_paper.pdf" \
  -F "answerSheet=@/path/to/answer_sheet.pdf" \
  -F 'metadata={"studentName":"John Doe","subject":"Physics","examTitle":"Midterm Exam","evaluationCriteria":"moderate"}'
```

## Testing

Use the provided test script to verify the API functionality:
```bash
./test-two-document-api.sh
```

## Error Handling

The API includes comprehensive error handling for:
- Invalid file formats
- Missing required fields
- Question paper analysis failures
- Answer sheet evaluation failures
- Network/API errors

## Response Format

The response format is identical to the original worksheet evaluation API, ensuring compatibility with existing client applications.

## API Version

The API version has been updated to 2.0.0 to reflect the new two-document functionality. The original single-document endpoints remain available for backward compatibility.

## Technical Implementation

### Service Layer Methods
- `analyzeQuestionPaper(MultipartFile, String)` - Analyzes question paper and returns structured data
- `evaluateAnswerSheetAgainstQuestions(MultipartFile, QuestionPaperAnalysisResult, QuestionPaperEvaluationRequest)` - Evaluates answer sheet using structured question data

### Prompting Strategy
- **Question Paper Analysis**: Uses structured JSON output requirements to ensure consistent parsing
- **Answer Sheet Evaluation**: Includes the complete question analysis in the prompt for precise evaluation

### Gemini Integration
- Uses Gemini 2.5 Pro for both question paper analysis and answer sheet evaluation
- Implements robust JSON parsing with fallback mechanisms
- Includes comprehensive error handling for API failures
