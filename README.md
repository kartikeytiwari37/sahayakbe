# Sahayak Backend Service

## Introduction

Sahayak is a comprehensive AI-powered educational service backend that provides various functionalities to support teaching and learning. The service leverages Google's Gemini AI models to offer features like AI teacher assistance, worksheet evaluation, exam creation, and more.

## Features and Capabilities

### 1. AI Teacher Assistant

- **Real-time AI Teacher**: Interactive AI teacher that can respond to student questions via WebSocket
- **Multi-modal Interaction**: Supports text, audio, and video inputs
- **Custom Prompts**: Ability to create custom teacher personas with specific instructions
- **Prompt Creator**: Special mode to help teachers create effective prompts for AI teaching assistants
- **Session Management**: Create, manage, and close teacher sessions

### 2. Worksheet Evaluation

- **Single Document Evaluation**: Evaluate student worksheets and provide detailed feedback
- **Two-Document Evaluation**: Enhanced evaluation using separate question paper and answer sheet
- **Structured Analysis**: Detailed question-by-question analysis with scoring
- **Feedback Generation**: Comprehensive feedback including strengths, areas for improvement, and teacher recommendations
- **Multiple File Formats**: Support for PDF, JPG, and PNG formats
- **Customizable Evaluation Criteria**: Options for strict, moderate, or lenient evaluation

### 3. Exam Creation

- **Multiple Exam Types**: Support for multiple-choice, true/false, essay, short answer, and mixed exam types
- **Customizable Parameters**: Control subject, grade level, number of questions, and more
- **PDF-based Exam Creation**: Generate exams based on content from PDF documents
- **Structured Output**: Well-formatted exam data with questions, options, correct answers, and explanations
- **Strategy Pattern**: Extensible architecture for different exam types

### 4. Video Generation

- **Educational Video Creation**: Generate educational videos based on teaching context
- **Video Prompt Generation**: Create structured prompts for video generation
- **Video Status Tracking**: Monitor video generation progress
- **Video Download**: Download generated videos

### 5. Future Planning

- **Career Roadmaps**: Generate future career plans for students
- **Udaan Prompt Creator**: Special mode for creating motivational career roadmaps

### 6. Document Processing

- **PDF Summarization**: Summarize PDF content using Gemini API

## API Endpoints

### Health Check Endpoints

- `GET /api/sahayak/health`: Health check for Sahayak service
- `GET /api/worksheet/health`: Health check for worksheet evaluation service
- `GET /api/exam/health`: Health check for exam creation service

### Teacher Session Endpoints

- `POST /api/sahayak/teacher/session`: Create a new teacher session
- `POST /api/sahayak/teacher/session/custom`: Create a custom teacher session
- `POST /api/sahayak/teacher/prompt-creator`: Create a prompt creator session
- `GET /api/sahayak/teacher/session/{sessionId}/status`: Get session status
- `DELETE /api/sahayak/teacher/session/{sessionId}`: Close a teacher session
- `GET /api/sahayak/teacher/sessions`: Get all active sessions
- `POST /api/sahayak/teacher/session/{sessionId}/text`: Send text message to teacher

### Worksheet Evaluation Endpoints

- `POST /api/worksheet/evaluate`: Evaluate a worksheet
- `POST /api/worksheet/evaluate-form`: Evaluate a worksheet with form data
- `GET /api/worksheet/info`: Get API information
- `GET /api/worksheet/example`: Get example request format

### Two-Document Evaluation Endpoints

- `POST /api/worksheet/evaluate-with-question-paper`: Evaluate with separate question paper and answer sheet
- `POST /api/worksheet/evaluate-with-question-paper-form`: Evaluate with separate question paper and answer sheet using form data

### Exam Creation Endpoints

- `POST /api/exam/create`: Create an exam
- `POST /api/exam/createWithPdf`: Create an exam based on PDF content
- `POST /api/exam/summarize-pdf`: Summarize PDF content

### Video Generation Endpoints

- `POST /api/sahayak/video/generate-prompt`: Generate video prompt
- `POST /api/sahayak/video/generate`: Generate video
- `GET /api/sahayak/video/status`: Check video generation status
- `POST /api/sahayak/video/download`: Download generated video

### Future Planning Endpoints

- `POST /api/sahayak/future-plan/generate`: Generate future plan

### WebSocket Endpoints

- `/sahayak-teacher`: WebSocket endpoint for real-time teacher interaction

## Architecture and Components

### Core Components

1. **Controllers**:
   - `SahayakController`: Manages teacher sessions and video generation
   - `WorksheetController`: Handles worksheet evaluation
   - `ExamController`: Manages exam creation

2. **Services**:
   - `SahayakTeacherService`: Manages teacher sessions and interactions
   - `WorksheetEvaluationService`: Evaluates worksheets and question papers
   - `ExamCreationService`: Creates exams using different strategies
   - `GeminiLiveWebSocketClient`: Handles WebSocket communication with Gemini API

3. **Models**:
   - Request/response models for different functionalities
   - Data models for structured information

4. **Strategies**:
   - Exam type strategies for different exam formats
   - Strategy factory for creating appropriate strategies

### WebSocket Architecture

- Real-time bidirectional communication with clients
- Support for text, audio, and video data
- Session management and cleanup

### Two-Document Evaluation Process

1. Question paper analysis to extract structured data
2. Answer sheet evaluation against structured question data
3. Comprehensive feedback generation

## Gemini AI Integration

Sahayak leverages Google's Gemini AI models extensively throughout the service to provide intelligent educational features:

### Gemini Models Used

#### Gemini 2.5 Flash
Primary model used for most processing tasks:
- **Worksheet Evaluation**:
  - Generating evaluation prompts for worksheets
  - Analyzing question papers to extract structured data
  - Processing student answer sheets against question data
- **Exam Creation**:
  - Generating exam questions based on subject and grade level
  - Creating structured exam data with questions, options, and answers
- **Initial Content Analysis**:
  - Extracting key information from uploaded documents
  - Structuring content for further processing

#### Gemini 2.5 Pro
Used for specific tasks requiring deeper understanding:
- **PDF Summarization**:
  - Extracting and summarizing content from PDF documents
  - Processing PDF content for exam question generation
  - Handling multimodal inputs (text + PDF)

#### Gemini 2.0 Flash
Used for real-time interactions and streaming responses:
- **Live Teacher Sessions**:
  - Real-time conversation via WebSocket connections
  - Processing text inputs from students
  - Generating conversational responses
- **Audio/Video Processing**:
  - Processing audio inputs in teacher sessions
  - Analyzing video/screen sharing data
  - Multimodal understanding during live sessions
- **Prompt Creation**:
  - Generating specialized teaching prompts
  - Creating future planning prompts for Udaan

### Specific Use Cases by Feature

#### Worksheet Evaluation
- **Gemini 2.5 Flash**: Analyzes question papers to extract structured data about questions, point values, and scoring rubrics
- **Gemini 2.5 Flash**: Evaluates student answer sheets against structured question data
- **Gemini 2.5 Flash**: Generates detailed feedback with strengths and areas for improvement

#### Exam Creation
- **Gemini 2.5 Flash**: Creates exam questions based on subject, grade level, and exam type
- **Gemini 2.5 Flash**: Generates multiple-choice, true/false, essay, and short answer questions
- **Gemini 2.5 Pro**: Processes PDF content to create context-relevant exam questions

#### Teacher Assistant
- **Gemini 2.0 Flash**: Powers real-time teacher sessions via WebSocket
- **Gemini 2.0 Flash**: Processes text, audio, and video inputs from students
- **Gemini 2.0 Flash**: Generates educational responses with appropriate voice modality

#### Video Generation
- **Gemini 2.5 Flash**: Creates structured video prompts based on educational context
- **Gemini 2.0 Flash**: Processes teaching context to generate appropriate video content

#### Future Planning
- **Gemini 2.0 Flash**: Creates career roadmaps and future plans for students
- **Gemini 2.0 Flash**: Powers the Udaan prompt creator for motivational content

### Key Gemini AI Features Utilized

1. **Multimodal Understanding**: Processing text, images (PDFs, worksheets), and audio inputs
2. **Structured Output Generation**: Creating well-formatted JSON responses for exam questions and evaluations
3. **Context-Aware Responses**: Maintaining conversation context in teacher sessions
4. **Educational Content Generation**: Creating educational content tailored to specific subjects and grade levels
5. **Real-time Interaction**: Providing immediate responses through WebSocket connections
6. **Content Summarization**: Extracting and summarizing key information from documents

### Integration Methods

- **REST API**: For non-streaming requests like exam creation and worksheet evaluation
- **WebSocket API**: For real-time, streaming interactions with the AI teacher
- **Prompt Engineering**: Specialized prompts for different educational contexts

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           CLIENT APPLICATIONS                            │
│                                                                         │
│  ┌───────────────┐    ┌───────────────┐    ┌───────────────────────┐   │
│  │ Web Interface │    │ Mobile App    │    │ Learning Management   │   │
│  │               │    │               │    │ System                │   │
│  └───────┬───────┘    └───────┬───────┘    └───────────┬───────────┘   │
└──────────┼─────────────────────┼───────────────────────┼───────────────┘
           │                     │                       │
           │                     │                       │
           ▼                     ▼                       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                          SAHAYAK BACKEND SERVICE                          │
│                                                                          │
│  ┌────────────────┐   ┌────────────────┐   ┌────────────────────────┐   │
│  │                │   │                │   │                        │   │
│  │  REST API      │   │  WebSocket     │   │  Async Processing      │   │
│  │  Endpoints     │   │  Endpoints     │   │  Services              │   │
│  │                │   │                │   │                        │   │
│  └────────┬───────┘   └────────┬───────┘   └────────────┬───────────┘   │
│           │                    │                        │                │
│           │                    │                        │                │
│  ┌────────▼───────┐   ┌────────▼───────┐   ┌────────────▼───────────┐   │
│  │                │   │                │   │                        │   │
│  │  Controllers   │   │  WebSocket     │   │  Service Layer         │   │
│  │                │   │  Handlers      │   │                        │   │
│  └────────┬───────┘   └────────┬───────┘   └────────────┬───────────┘   │
│           │                    │                        │                │
│           │                    │                        │                │
│           └──────────┬─────────┴────────────────────────┘                │
│                      │                                                   │
│                      ▼                                                   │
│  ┌─────────────────────────────────────┐                                │
│  │                                     │                                │
│  │  Strategy Pattern Implementation    │                                │
│  │  (Exam Types, Evaluation Methods)   │                                │
│  │                                     │                                │
│  └─────────────────────────────────────┘                                │
│                                                                          │
└───────────────────────────────┬──────────────────────────────────────────┘
                                │
                                │
                                ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                         EXTERNAL SERVICES                                  │
│                                                                           │
│  ┌────────────────┐   ┌────────────────┐   ┌────────────────────────┐    │
│  │                │   │                │   │                        │    │
│  │  Google        │   │  Google        │   │  Google Cloud Run      │    │
│  │  Gemini API    │   │  Veo API       │   │  (Deployment)          │    │
│  │                │   │                │   │                        │    │
│  └────────────────┘   └────────────────┘   └────────────────────────┘    │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
```

## Technologies Used

- **Spring Boot**: Java-based framework for building the backend
- **WebSocket**: For real-time communication
- **Google Gemini API**: AI models for various functionalities
- **Docker**: For containerization
- **Google Cloud Run**: For deployment

## Setup and Installation

### Prerequisites

- Java 11 or higher
- Maven
- Google Gemini API key
- Google Cloud account (for deployment)

### Local Setup

1. Clone the repository
2. Configure environment variables:
   - `GEMINI_API_KEY`: Your Google Gemini API key
   - `PORT`: Server port (default: 8080)

3. Build the project:
   ```bash
   mvn clean package
   ```

4. Run the application:
   ```bash
   java -jar target/sahayak-backend.jar
   ```

## Deployment

The service can be deployed to Google Cloud Run using the provided deployment script or manual deployment steps. See [DEPLOYMENT.md](DEPLOYMENT.md) for detailed instructions.

## Usage Examples

### Creating a Teacher Session

```bash
curl -X POST "http://localhost:8080/api/sahayak/teacher/session"
```

### Evaluating a Worksheet

```bash
curl -X POST "http://localhost:8080/api/worksheet/evaluate-form" \
  -F "worksheetFile=@/path/to/worksheet.pdf" \
  -F "studentName=John Doe" \
  -F "subject=Mathematics" \
  -F "worksheetTitle=Algebra Practice"
```

### Creating an Exam

```bash
curl -X POST "http://localhost:8080/api/exam/create" \
  -H "Content-Type: application/json" \
  -d '{
    "subject": "Science",
    "gradeLevel": "8",
    "examType": "MULTIPLE_CHOICE",
    "numberOfQuestions": 10
  }'
```

### Two-Document Evaluation

```bash
curl -X POST "http://localhost:8080/api/worksheet/evaluate-with-question-paper-form" \
  -F "questionPaper=@/path/to/question_paper.pdf" \
  -F "answerSheet=@/path/to/answer_sheet.pdf" \
  -F "studentName=John Doe" \
  -F "subject=Physics" \
  -F "examTitle=Midterm Exam"
```

## Configuration

The application can be configured using environment variables or the `application.properties` file. Key configuration options include:

- **Server Configuration**: Port, timeouts, etc.
- **Gemini API Configuration**: API key, URL, model
- **WebSocket Configuration**: Heartbeat time, disconnect delay
- **Upload Configuration**: Max file size, request size

## Security Considerations

- API keys are stored as environment variables
- CORS is configured for cross-origin requests
- File validation for uploaded documents
- Error handling to prevent information leakage

## Performance Optimization

- Asynchronous processing for long-running tasks
- Configurable timeouts for API calls
- Memory and CPU limits for deployment
- Connection pooling for external services

## Limitations and Known Issues

- Large files may take longer to process
- Gemini API rate limits may apply
- WebSocket connections require stable internet connection

## Future Enhancements

- Additional exam types and evaluation strategies
- Enhanced video generation capabilities
- Integration with learning management systems
- Support for more languages and subjects
- Advanced analytics for student performance

## License

This project is licensed under the MIT License - see the LICENSE file for details.
