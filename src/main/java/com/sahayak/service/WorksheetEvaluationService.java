package com.sahayak.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahayak.model.WorksheetEvaluationRequest;
import com.sahayak.model.WorksheetEvaluationResponse;
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
        this.restTemplate = new RestTemplate();
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


        promptBuilder
                .append("\n- Instruct the AI to strictly follow the Output Format as : ")
                .append("\"evaluation\": {\n" + //
                        "        \"totalScore\": 0.0,\n" + //
                        "        \"maxPossibleScore\": 100.0,\n" + //
                        "        \"percentage\": 0.0,\n" + //
                        "        \"questionsAnalyzed\": 0,\n" + //
                        "        \"questionWiseResults\": [],\n" + //
                        "        \"overallFeedback\": \"\",\n" + //
                        "        \"strengths\": [],\n" + //
                        "        \"areasForImprovement\": [],\n" + //
                        "        \"teacherRecommendations\": \"\"\n" + //
                        "    }");


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
            // First, try to extract JSON data if present
            extractJsonFromText(evaluationText, result);
            
            // If JSON extraction didn't work, try regex-based extraction
            if (result.getTotalScore() == 0.0) {
                extractScoresFromText(evaluationText, result);
            }
            
            // Set default values for any fields that weren't extracted
            if (result.getQuestionWiseResults() == null) {
                result.setQuestionWiseResults(new ArrayList<>());
            }
            if (result.getStrengths() == null) {
                result.setStrengths(new ArrayList<>());
            }
            if (result.getAreasForImprovement() == null) {
                result.setAreasForImprovement(new ArrayList<>());
            }
            if (result.getOverallFeedback() == null || result.getOverallFeedback().isEmpty()) {
                result.setOverallFeedback(evaluationText);
            }
            if (result.getTeacherRecommendations() == null || result.getTeacherRecommendations().isEmpty()) {
                result.setTeacherRecommendations("Please review the detailed feedback above.");
            }
            
            logger.info("Evaluation response parsed successfully");
            
        } catch (Exception e) {
            logger.warn("Could not parse structured evaluation, using raw text", e);
            result.setOverallFeedback(evaluationText);
        }
        
        return result;
    }
    
    /**
     * Extract JSON data from evaluation text
     */
    private void extractJsonFromText(String text, WorksheetEvaluationResponse.EvaluationResult result) {
        try {
            // First, check if the text contains a JSON code block (```json ... ```)
            String codeBlockPattern = "```json\\s*\\{([\\s\\S]*?)\\}\\s*```";
            java.util.regex.Pattern codeBlockRegex = java.util.regex.Pattern.compile(codeBlockPattern);
            java.util.regex.Matcher codeBlockMatcher = codeBlockRegex.matcher(text);
            
            String jsonStr = null;
            
            if (codeBlockMatcher.find()) {
                // Extract the JSON from the code block
                jsonStr = "{" + codeBlockMatcher.group(1) + "}";
                logger.info("Found JSON code block in evaluation text");
            } else {
                // If no code block, try to find JSON directly in the text
                String jsonPattern = "\"evaluation\"\\s*:\\s*\\{";
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(jsonPattern);
                java.util.regex.Matcher matcher = pattern.matcher(text);
                
                if (matcher.find()) {
                    // Extract the JSON object
                    int startIndex = matcher.start();
                    
                    // Find the matching closing brace
                    int openBraces = 0;
                    int closeBraces = 0;
                    int endIndex = startIndex;
                    
                    for (int i = startIndex; i < text.length(); i++) {
                        if (text.charAt(i) == '{') {
                            openBraces++;
                        } else if (text.charAt(i) == '}') {
                            closeBraces++;
                            if (closeBraces == openBraces) {
                                endIndex = i + 1;
                                break;
                            }
                        }
                    }
                    
                    // Extract the JSON substring
                    jsonStr = "{" + text.substring(startIndex, endIndex);
                    logger.info("Found JSON directly in evaluation text");
                }
            }
            
            if (jsonStr != null) {
                // Parse the JSON
                JsonNode rootNode = objectMapper.readTree(jsonStr);
                JsonNode evaluationNode = rootNode.get("evaluation");
                
                if (evaluationNode != null) {
                    // Extract values from JSON
                    if (evaluationNode.has("totalScore")) {
                        result.setTotalScore(evaluationNode.get("totalScore").asDouble());
                    }
                    
                    if (evaluationNode.has("maxPossibleScore")) {
                        result.setMaxPossibleScore(evaluationNode.get("maxPossibleScore").asDouble());
                    }
                    
                    if (evaluationNode.has("percentage")) {
                        result.setPercentage(evaluationNode.get("percentage").asDouble());
                    }
                    
                    if (evaluationNode.has("questionsAnalyzed")) {
                        result.setQuestionsAnalyzed(evaluationNode.get("questionsAnalyzed").asInt());
                    }
                    
                    if (evaluationNode.has("questionWiseResults") && evaluationNode.get("questionWiseResults").isArray()) {
                        JsonNode questionResults = evaluationNode.get("questionWiseResults");
                        List<WorksheetEvaluationResponse.QuestionResult> questionResultsList = new ArrayList<>();
                        
                        for (JsonNode questionNode : questionResults) {
                            WorksheetEvaluationResponse.QuestionResult questionResult = new WorksheetEvaluationResponse.QuestionResult();
                            
                            if (questionNode.has("questionNumber")) {
                                questionResult.setQuestionNumber(questionNode.get("questionNumber").asInt());
                            }
                            
                            if (questionNode.has("questionText")) {
                                questionResult.setQuestionText(questionNode.get("questionText").asText());
                            }
                            
                            if (questionNode.has("studentAnswer")) {
                                questionResult.setStudentAnswer(questionNode.get("studentAnswer").asText());
                            }
                            
                            if (questionNode.has("correctAnswer")) {
                                questionResult.setCorrectAnswer(questionNode.get("correctAnswer").asText());
                            }
                            
                            // Handle different field names for scores
                            if (questionNode.has("scoreAwarded")) {
                                questionResult.setPointsAwarded(questionNode.get("scoreAwarded").asDouble());
                            } else if (questionNode.has("score")) {
                                questionResult.setPointsAwarded(questionNode.get("score").asDouble());
                            }
                            
                            if (questionNode.has("maxPoints")) {
                                questionResult.setMaxPoints(questionNode.get("maxPoints").asDouble());
                            } else if (questionNode.has("maxScore")) {
                                questionResult.setMaxPoints(questionNode.get("maxScore").asDouble());
                            }
                            
                            if (questionNode.has("feedback")) {
                                questionResult.setFeedback(questionNode.get("feedback").asText());
                            }
                            
                            questionResultsList.add(questionResult);
                        }
                        
                        result.setQuestionWiseResults(questionResultsList);
                    }
                    
                    if (evaluationNode.has("overallFeedback")) {
                        result.setOverallFeedback(evaluationNode.get("overallFeedback").asText());
                    }
                    
                    if (evaluationNode.has("strengths") && evaluationNode.get("strengths").isArray()) {
                        List<String> strengths = new ArrayList<>();
                        for (JsonNode strength : evaluationNode.get("strengths")) {
                            strengths.add(strength.asText());
                        }
                        result.setStrengths(strengths);
                    }
                    
                    if (evaluationNode.has("areasForImprovement") && evaluationNode.get("areasForImprovement").isArray()) {
                        List<String> areasForImprovement = new ArrayList<>();
                        for (JsonNode area : evaluationNode.get("areasForImprovement")) {
                            areasForImprovement.add(area.asText());
                        }
                        result.setAreasForImprovement(areasForImprovement);
                    }
                    
                    if (evaluationNode.has("teacherRecommendations")) {
                        result.setTeacherRecommendations(evaluationNode.get("teacherRecommendations").asText());
                    }
                    
                    logger.info("Successfully extracted JSON data from evaluation text");
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract JSON data from evaluation text: {}", e.getMessage());
        }
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
