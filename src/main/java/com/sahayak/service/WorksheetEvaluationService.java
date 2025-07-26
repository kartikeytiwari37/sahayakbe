package com.sahayak.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahayak.model.WorksheetEvaluationRequest;
import com.sahayak.model.WorksheetEvaluationResponse;
import com.sahayak.model.QuestionPaperEvaluationRequest;
import com.sahayak.model.QuestionPaperAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class WorksheetEvaluationService {
    
    private static final Logger logger = LoggerFactory.getLogger(WorksheetEvaluationService.class);
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;
    
    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String geminiFlashModel;
    
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    
    // Supported file types
    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
        "application/pdf",
        "image/jpeg",
        "image/jpg", 
        "image/png"
    );
    
    // Maximum file size (10MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    
    public WorksheetEvaluationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = createPermissiveRestTemplate();
    }
    
    /**
     * Create a RestTemplate with permissive SSL configuration for development
     */
    private RestTemplate createPermissiveRestTemplate() {
        try {
            // Create a trust manager that accepts all certificates (for development only)
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] {
                new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
            };
            
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            // Create SSL socket factory
            javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            
            // Create hostname verifier that accepts all hostnames
            javax.net.ssl.HostnameVerifier hostnameVerifier = (hostname, session) -> true;
            
            // Configure HTTP client factory with SSL settings
            org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
            
            // For HTTPS connections, we need to use HttpsURLConnection
            factory.setConnectTimeout(30000);
            factory.setReadTimeout(60000);
            
            RestTemplate restTemplate = new RestTemplate(factory);
            
            // Set default SSL context for HttpsURLConnection
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
            
            logger.info("Created RestTemplate with permissive SSL configuration for development");
            return restTemplate;
            
        } catch (Exception e) {
            logger.warn("Failed to create permissive RestTemplate, using default: {}", e.getMessage());
            return new RestTemplate();
        }
    }
    
    /**
     * Main method to evaluate a worksheet
     */
    public CompletableFuture<WorksheetEvaluationResponse> evaluateWorksheet(
            MultipartFile worksheetFile, 
            WorksheetEvaluationRequest request) {
        
        long startTime = System.currentTimeMillis();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting worksheet evaluation for student: {}, subject: {}", 
                           request.getStudentName(), request.getSubject());
                
                // Step 1: Validate and process file
                String base64Document = validateAndProcessFile(worksheetFile);
                
                // Step 2: Generate evaluation prompt using Gemini Flash
                String evaluationPrompt = generateEvaluationPrompt(request);
                
                // Step 3: Evaluate worksheet using Gemini 2.5 Pro
                WorksheetEvaluationResponse.EvaluationResult evaluationResult = 
                    evaluateWorksheetWithGemini(base64Document, evaluationPrompt, worksheetFile.getContentType());
                
                // Step 4: Create response
                WorksheetEvaluationResponse response = new WorksheetEvaluationResponse(
                    request.getStudentName(),
                    request.getStudentId(),
                    request.getWorksheetTitle(),
                    request.getSubject(),
                    evaluationResult
                );
                
                long endTime = System.currentTimeMillis();
                response.setProcessingTime(String.format("%.1fs", (endTime - startTime) / 1000.0));
                
                logger.info("Worksheet evaluation completed successfully for student: {} , response : {}",
                           request.getStudentName(), response);
                
                return response;
                
            } catch (Exception e) {
                logger.error("Error evaluating worksheet for student: {}", request.getStudentName(), e);
                return new WorksheetEvaluationResponse("error", "Failed to evaluate worksheet: " + e.getMessage());
            }
        });
    }
    
    /**
     * Step 1: Validate file and convert to base64
     */
    private String validateAndProcessFile(MultipartFile file) throws Exception {
        // Validate file is not empty
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        
        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                String.format("File size exceeds maximum limit of %d MB", MAX_FILE_SIZE / (1024 * 1024)));
        }
        
        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                "Unsupported file type. Supported formats: PDF, JPG, JPEG, PNG");
        }
        
        // Convert to base64
        byte[] fileBytes = file.getBytes();
        String base64Document = Base64.getEncoder().encodeToString(fileBytes);
        
        logger.info("File processed successfully. Type: {}, Size: {} bytes", contentType, fileBytes.length);
        
        return base64Document;
    }
    
    /**
     * Step 2: Generate evaluation prompt using Gemini Flash
     */
    private String generateEvaluationPrompt(WorksheetEvaluationRequest request) throws Exception {
        logger.info("Generating evaluation prompt using Gemini Flash");
        
        String promptGenerationRequest = buildPromptGenerationRequest(request);
        
        // Call Gemini Flash to generate the evaluation prompt
        Map<String, Object> requestBody = createGeminiRequestBody(promptGenerationRequest, null, null);
        String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + geminiFlashModel + ":generateContent";
        String urlWithApiKey = geminiUrl + "?key=" + geminiApiKey;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(urlWithApiKey, HttpMethod.POST, entity, Map.class);
        
        String generatedPrompt = extractTextFromGeminiResponse(response.getBody());
        
        logger.info("Evaluation prompt generated successfully");
        return generatedPrompt;
    }
    
    /**
     * Build the request for prompt generation
     */
    private String buildPromptGenerationRequest(WorksheetEvaluationRequest request) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("Create a comprehensive evaluation prompt for analyzing a student's worksheet. ");
        promptBuilder.append("The prompt should instruct an AI to evaluate the worksheet document and provide structured feedback. ");
        promptBuilder.append("\n\nContext:\n");
        promptBuilder.append("- Student: ").append(request.getStudentName());
        if (request.getStudentId() != null) {
            promptBuilder.append(" (ID: ").append(request.getStudentId()).append(")");
        }
        promptBuilder.append("\n- Subject: ").append(request.getSubject());
        promptBuilder.append("\n- Worksheet: ").append(request.getWorksheetTitle());
        promptBuilder.append("\n- Evaluation Criteria: ").append(request.getEvaluationCriteria());
        
        if (request.getAdditionalInstructions() != null && !request.getAdditionalInstructions().trim().isEmpty()) {
            promptBuilder.append("\n- Additional Instructions: ").append(request.getAdditionalInstructions());
        }
        
        if (request.getTeacherNotes() != null && !request.getTeacherNotes().trim().isEmpty()) {
            promptBuilder.append("\n- Teacher Notes: ").append(request.getTeacherNotes());
        }
        
        promptBuilder.append("\n\nThe generated prompt should instruct the AI to:\n");
        promptBuilder.append("1. Analyze the worksheet document to identify all questions and their point values\n");
        promptBuilder.append("2. Identify the student's answers for each question\n");
        promptBuilder.append("3. Determine the correct answers or scoring rubric from the worksheet\n");
        promptBuilder.append("4. Evaluate each answer based on the specified criteria (").append(request.getEvaluationCriteria()).append(")\n");
        promptBuilder.append("5. Provide detailed feedback for each question\n");
        promptBuilder.append("6. Calculate total score and percentage\n");
        promptBuilder.append("7. Identify strengths and areas for improvement\n");
        promptBuilder.append("8. Provide teacher recommendations\n");
        promptBuilder.append("\nReturn only the evaluation prompt, ready to be used with a document.");
        
        return promptBuilder.toString();
    }
    
    /**
     * Step 3: Evaluate worksheet using Gemini 2.5 Pro
     */
    private WorksheetEvaluationResponse.EvaluationResult evaluateWorksheetWithGemini(
            String base64Document, String evaluationPrompt, String mimeType) throws Exception {
        
        logger.info("Evaluating worksheet using Gemini 2.5 Pro");
        
        // Create request body with document and prompt
        Map<String, Object> requestBody = createGeminiRequestBody(evaluationPrompt, base64Document, mimeType);
        
        // Call Gemini 2.5 Pro
        String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent";
        String urlWithApiKey = geminiUrl + "?key=" + geminiApiKey;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(urlWithApiKey, HttpMethod.POST, entity, Map.class);
        
        String evaluationText = extractTextFromGeminiResponse(response.getBody());
        
        // Parse the evaluation response into structured format
        WorksheetEvaluationResponse.EvaluationResult result = parseEvaluationResponse(evaluationText);
        
        logger.info("Worksheet evaluation completed using Gemini 2.5 Pro");
        return result;
    }
    
    /**
     * Create Gemini API request body
     */
    private Map<String, Object> createGeminiRequestBody(String textPrompt, String base64Document, String mimeType) {
        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();
        
        // Add text prompt part
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", textPrompt);
        parts.add(textPart);
        
        // Add document part if provided
        if (base64Document != null && mimeType != null) {
            Map<String, Object> documentPart = new HashMap<>();
            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mime_type", mimeType);
            inlineData.put("data", base64Document);
            documentPart.put("inline_data", inlineData);
            parts.add(documentPart);
        }
        
        content.put("parts", parts);
        contents.add(content);
        requestBody.put("contents", contents);
        
        return requestBody;
    }
    
    /**
     * Extract text from Gemini API response
     */
    @SuppressWarnings("unchecked")
    private String extractTextFromGeminiResponse(Map<String, Object> response) throws Exception {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> candidate = candidates.get(0);
                Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
            throw new Exception("No valid response received from Gemini API");
        } catch (Exception e) {
            logger.error("Error extracting text from Gemini response", e);
            throw new Exception("Failed to parse Gemini API response: " + e.getMessage());
        }
    }
    
    /**
     * Parse evaluation response into structured format
     * This is a simplified parser - in production, you might want more sophisticated parsing
     */
    private WorksheetEvaluationResponse.EvaluationResult parseEvaluationResponse(String evaluationText) {
        WorksheetEvaluationResponse.EvaluationResult result = new WorksheetEvaluationResponse.EvaluationResult();
        
        try {
            // Try to extract structured information from the text
            // This is a basic implementation - you might want to enhance this based on actual Gemini responses
            
            // Set default values
            result.setTotalScore(0.0);
            result.setMaxPossibleScore(100.0);
            result.setPercentage(0.0);
            result.setQuestionsAnalyzed(0);
            result.setQuestionWiseResults(new ArrayList<>());
            result.setOverallFeedback(evaluationText);
            result.setStrengths(new ArrayList<>());
            result.setAreasForImprovement(new ArrayList<>());
            result.setTeacherRecommendations("Please review the detailed feedback above.");
            
            // Try to extract numerical scores if present in the text
            extractScoresFromText(evaluationText, result);
            
            logger.info("Evaluation response parsed successfully");
            
        } catch (Exception e) {
            logger.warn("Could not parse structured evaluation, using raw text", e);
            result.setOverallFeedback(evaluationText);
        }
        
        return result;
    }
    
    /**
     * Extract scores from evaluation text using regex patterns
     */
    private void extractScoresFromText(String text, WorksheetEvaluationResponse.EvaluationResult result) {
        try {
            // Look for patterns like "Score: 85/100" or "Total: 85 out of 100"
            java.util.regex.Pattern scorePattern = java.util.regex.Pattern.compile(
                "(?i)(?:score|total|marks?)\\s*:?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:out\\s*of|/|\\s+)\\s*(\\d+(?:\\.\\d+)?)"
            );
            
            java.util.regex.Matcher matcher = scorePattern.matcher(text);
            if (matcher.find()) {
                double score = Double.parseDouble(matcher.group(1));
                double maxScore = Double.parseDouble(matcher.group(2));
                
                result.setTotalScore(score);
                result.setMaxPossibleScore(maxScore);
                result.setPercentage(maxScore > 0 ? (score / maxScore) * 100 : 0);
                
                logger.info("Extracted scores: {}/{} ({}%)", score, maxScore, result.getPercentage());
            }
            
            // Look for number of questions
            java.util.regex.Pattern questionPattern = java.util.regex.Pattern.compile(
                "(?i)(\\d+)\\s+questions?"
            );
            
            matcher = questionPattern.matcher(text);
            if (matcher.find()) {
                int questionCount = Integer.parseInt(matcher.group(1));
                result.setQuestionsAnalyzed(questionCount);
            }
            
        } catch (Exception e) {
            logger.warn("Could not extract numerical scores from text", e);
        }
    }
    
    /**
     * NEW: Two-step evaluation process - Step 1: Analyze question paper
     */
    public CompletableFuture<QuestionPaperAnalysisResult> analyzeQuestionPaper(
            MultipartFile questionPaper, String subject) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting question paper analysis for subject: {}", subject);
                
                // Step 1: Validate and process question paper file
                String base64Document = validateAndProcessFile(questionPaper);
                
                // Step 2: Create specialized prompt for question paper analysis
                String analysisPrompt = buildQuestionPaperAnalysisPrompt(subject);
                
                // Step 3: Analyze question paper using Gemini
                QuestionPaperAnalysisResult result = analyzeQuestionPaperWithGemini(
                    base64Document, analysisPrompt, questionPaper.getContentType());
                
                logger.info("Question paper analysis completed successfully");
                return result;
                
            } catch (Exception e) {
                logger.error("Error analyzing question paper", e);
                return new QuestionPaperAnalysisResult("error", "Failed to analyze question paper: " + e.getMessage());
            }
        });
    }
    
    /**
     * NEW: Two-step evaluation process - Step 2: Evaluate answer sheet against parsed questions
     */
    public CompletableFuture<WorksheetEvaluationResponse> evaluateAnswerSheetAgainstQuestions(
            MultipartFile answerSheet, 
            QuestionPaperAnalysisResult questionAnalysis,
            QuestionPaperEvaluationRequest request) {
        
        long startTime = System.currentTimeMillis();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting answer sheet evaluation for student: {}", request.getStudentName());
                
                // Step 1: Validate and process answer sheet file
                String base64Document = validateAndProcessFile(answerSheet);
                
                // Step 2: Create evaluation prompt using structured question data
                String evaluationPrompt = buildAnswerSheetEvaluationPrompt(questionAnalysis, request);
                
                // Step 3: Evaluate answer sheet using Gemini 2.5 Pro
                WorksheetEvaluationResponse.EvaluationResult evaluationResult = 
                    evaluateAnswerSheetWithGemini(base64Document, evaluationPrompt, answerSheet.getContentType());
                
                // Step 4: Create response
                WorksheetEvaluationResponse response = new WorksheetEvaluationResponse(
                    request.getStudentName(),
                    request.getStudentId(),
                    request.getExamTitle(),
                    request.getSubject(),
                    evaluationResult
                );
                
                long endTime = System.currentTimeMillis();
                response.setProcessingTime(String.format("%.1fs", (endTime - startTime) / 1000.0));
                
                logger.info("Answer sheet evaluation completed successfully for student: {}", request.getStudentName());
                return response;
                
            } catch (Exception e) {
                logger.error("Error evaluating answer sheet for student: {}", request.getStudentName(), e);
                return new WorksheetEvaluationResponse("error", "Failed to evaluate answer sheet: " + e.getMessage());
            }
        });
    }
    
    /**
     * Build specialized prompt for question paper analysis
     */
    private String buildQuestionPaperAnalysisPrompt(String subject) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("You are an expert educational content analyzer. Analyze the provided question paper document and extract structured information about all questions.\n\n");
        promptBuilder.append("Subject: ").append(subject).append("\n\n");
        
        promptBuilder.append("IMPORTANT: You must return your response in valid JSON format with the following exact structure:\n\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"examTitle\": \"string\",\n");
        promptBuilder.append("  \"subject\": \"string\",\n");
        promptBuilder.append("  \"totalMarks\": number,\n");
        promptBuilder.append("  \"totalQuestions\": number,\n");
        promptBuilder.append("  \"instructions\": \"string\",\n");
        promptBuilder.append("  \"questions\": [\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"questionNumber\": number,\n");
        promptBuilder.append("      \"questionText\": \"string\",\n");
        promptBuilder.append("      \"questionType\": \"MCQ|SHORT_ANSWER|ESSAY|TRUE_FALSE|FILL_BLANK\",\n");
        promptBuilder.append("      \"marks\": number,\n");
        promptBuilder.append("      \"options\": [\"string\"] (only for MCQ),\n");
        promptBuilder.append("      \"correctAnswer\": \"string\" (for definitive answers),\n");
        promptBuilder.append("      \"scoringRubric\": \"string\" (for subjective questions),\n");
        promptBuilder.append("      \"keywords\": [\"string\"] (expected keywords for partial scoring),\n");
        promptBuilder.append("      \"subQuestions\": [] (for questions with parts a, b, c etc.)\n");
        promptBuilder.append("    }\n");
        promptBuilder.append("  ]\n");
        promptBuilder.append("}\n\n");
        
        promptBuilder.append("Instructions:\n");
        promptBuilder.append("1. Carefully read the entire question paper\n");
        promptBuilder.append("2. Extract the exam title and total marks\n");
        promptBuilder.append("3. Identify each question with its number, text, and point value\n");
        promptBuilder.append("4. Determine the question type (MCQ, short answer, essay, etc.)\n");
        promptBuilder.append("5. For MCQ questions, extract all options\n");
        promptBuilder.append("6. For questions with definitive answers, provide the correct answer\n");
        promptBuilder.append("7. For subjective questions, create a scoring rubric based on the question requirements\n");
        promptBuilder.append("8. Identify key terms/concepts that should be present in good answers\n");
        promptBuilder.append("9. Handle sub-questions (parts a, b, c) as separate entries in subQuestions array\n\n");
        
        promptBuilder.append("Return ONLY the JSON response, no additional text or formatting.");
        
        return promptBuilder.toString();
    }
    
    /**
     * Build evaluation prompt using structured question data
     */
    private String buildAnswerSheetEvaluationPrompt(QuestionPaperAnalysisResult questionAnalysis, 
                                                   QuestionPaperEvaluationRequest request) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("You are an expert teacher evaluating a student's answer sheet. ");
        promptBuilder.append("Use the provided structured question data to evaluate each answer precisely.\n\n");
        
        promptBuilder.append("STUDENT INFORMATION:\n");
        promptBuilder.append("- Name: ").append(request.getStudentName()).append("\n");
        if (request.getStudentId() != null) {
            promptBuilder.append("- ID: ").append(request.getStudentId()).append("\n");
        }
        promptBuilder.append("- Subject: ").append(request.getSubject()).append("\n");
        promptBuilder.append("- Exam: ").append(request.getExamTitle()).append("\n");
        promptBuilder.append("- Evaluation Criteria: ").append(request.getEvaluationCriteria()).append("\n");
        
        if (request.getAdditionalInstructions() != null && !request.getAdditionalInstructions().trim().isEmpty()) {
            promptBuilder.append("- Additional Instructions: ").append(request.getAdditionalInstructions()).append("\n");
        }
        
        if (request.getTeacherNotes() != null && !request.getTeacherNotes().trim().isEmpty()) {
            promptBuilder.append("- Teacher Notes: ").append(request.getTeacherNotes()).append("\n");
        }
        
        promptBuilder.append("\nQUESTION PAPER ANALYSIS:\n");
        try {
            String questionDataJson = objectMapper.writeValueAsString(questionAnalysis);
            promptBuilder.append(questionDataJson);
        } catch (Exception e) {
            logger.warn("Could not serialize question analysis to JSON", e);
            promptBuilder.append("Total Questions: ").append(questionAnalysis.getTotalQuestions()).append("\n");
            promptBuilder.append("Total Marks: ").append(questionAnalysis.getTotalMarks()).append("\n");
        }
        
        promptBuilder.append("\n\nEVALUATION INSTRUCTIONS:\n");
        promptBuilder.append("1. Analyze the answer sheet document to identify the student's responses\n");
        promptBuilder.append("2. Match each student answer to the corresponding question from the question paper analysis\n");
        promptBuilder.append("3. Evaluate each answer based on the provided correct answers, scoring rubrics, and keywords\n");
        promptBuilder.append("4. Apply the evaluation criteria: ").append(request.getEvaluationCriteria()).append("\n");
        promptBuilder.append("   - strict: Require exact answers and complete explanations\n");
        promptBuilder.append("   - moderate: Allow reasonable variations and partial credit\n");
        promptBuilder.append("   - lenient: Give benefit of doubt and generous partial credit\n");
        promptBuilder.append("5. Calculate scores for each question and provide specific feedback\n");
        promptBuilder.append("6. Identify overall strengths and areas for improvement\n");
        promptBuilder.append("7. Provide constructive teacher recommendations\n\n");
        
        promptBuilder.append("IMPORTANT: Return your evaluation in a structured format that can be parsed. ");
        promptBuilder.append("Include total score, percentage, question-wise breakdown with feedback, ");
        promptBuilder.append("overall assessment, strengths, areas for improvement, and teacher recommendations.");
        
        return promptBuilder.toString();
    }
    
    /**
     * Analyze question paper using Gemini
     */
    private QuestionPaperAnalysisResult analyzeQuestionPaperWithGemini(
            String base64Document, String analysisPrompt, String mimeType) throws Exception {
        
        logger.info("Analyzing question paper using Gemini");
        
        // Create request body with document and prompt
        Map<String, Object> requestBody = createGeminiRequestBody(analysisPrompt, base64Document, mimeType);
        
        // Call Gemini 2.5 Pro for analysis
        String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent";
        String urlWithApiKey = geminiUrl + "?key=" + geminiApiKey;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(urlWithApiKey, HttpMethod.POST, entity, Map.class);
        
        String analysisText = extractTextFromGeminiResponse(response.getBody());
        
        // Parse the analysis response into structured format
        QuestionPaperAnalysisResult result = parseQuestionPaperAnalysis(analysisText);
        
        logger.info("Question paper analysis completed using Gemini");
        return result;
    }
    
    /**
     * Evaluate answer sheet using Gemini 2.5 Pro with structured question data
     */
    private WorksheetEvaluationResponse.EvaluationResult evaluateAnswerSheetWithGemini(
            String base64Document, String evaluationPrompt, String mimeType) throws Exception {
        
        logger.info("Evaluating answer sheet using Gemini 2.5 Pro");
        
        // Create request body with document and prompt
        Map<String, Object> requestBody = createGeminiRequestBody(evaluationPrompt, base64Document, mimeType);
        
        // Call Gemini 2.5 Pro
        String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent";
        String urlWithApiKey = geminiUrl + "?key=" + geminiApiKey;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(urlWithApiKey, HttpMethod.POST, entity, Map.class);
        
        String evaluationText = extractTextFromGeminiResponse(response.getBody());
        
        // Parse the evaluation response into structured format
        WorksheetEvaluationResponse.EvaluationResult result = parseEvaluationResponse(evaluationText);
        
        logger.info("Answer sheet evaluation completed using Gemini 2.5 Pro");
        return result;
    }
    
    /**
     * Parse question paper analysis response from Gemini
     */
    private QuestionPaperAnalysisResult parseQuestionPaperAnalysis(String analysisText) {
        try {
            // Try to parse as JSON first
            String cleanedJson = analysisText.trim();
            if (cleanedJson.startsWith("```json")) {
                cleanedJson = cleanedJson.substring(7);
            }
            if (cleanedJson.endsWith("```")) {
                cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
            }
            cleanedJson = cleanedJson.trim();
            
            QuestionPaperAnalysisResult result = objectMapper.readValue(cleanedJson, QuestionPaperAnalysisResult.class);
            result.setStatus("success");
            
            logger.info("Successfully parsed question paper analysis JSON");
            return result;
            
        } catch (Exception e) {
            logger.warn("Could not parse question paper analysis as JSON, creating fallback result", e);
            
            // Create a fallback result
            QuestionPaperAnalysisResult result = new QuestionPaperAnalysisResult();
            result.setStatus("partial");
            result.setError("Could not parse structured data, but analysis was performed: " + e.getMessage());
            result.setExamTitle("Extracted from document");
            result.setSubject("Unknown");
            result.setTotalMarks(100.0);
            result.setTotalQuestions(0);
            result.setQuestions(new ArrayList<>());
            
            return result;
        }
    }
    
    /**
     * Health check method
     */
    public Map<String, String> getHealthStatus() {
        Map<String, String> status = new HashMap<>();
        status.put("service", "Worksheet Evaluation Service");
        status.put("status", "UP");
        status.put("timestamp", String.valueOf(System.currentTimeMillis()));
        status.put("geminiModel", geminiFlashModel);
        return status;
    }
}
