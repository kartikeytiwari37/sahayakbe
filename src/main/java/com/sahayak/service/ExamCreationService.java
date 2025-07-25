package com.sahayak.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahayak.model.ExamCreationRequest;
import com.sahayak.model.ExamCreationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class ExamCreationService {

    private static final Logger logger = LoggerFactory.getLogger(ExamCreationService.class);

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.model}")
    private String geminiModel;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // REST API endpoint for Gemini (not WebSocket)
    private static final String GEMINI_REST_API_URL = "https://generativelanguage.googleapis.com/v1beta/";

    public ExamCreationService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates an exam based on the provided request parameters
     * 
     * @param request The exam creation request
     * @return The exam creation response
     */
    public ExamCreationResponse createExam(ExamCreationRequest request) {
        try {
            logger.info("Creating exam with request: {}", request);

            // Create the prompt for the LLM
            String prompt = createExamPrompt(request);
            logger.debug("Generated prompt: {}", prompt);

            // Call the Gemini API
            String llmResponse = callGeminiApi(prompt);
            logger.debug("Raw LLM response: {}", llmResponse);

            // Parse the response
            ExamCreationResponse response = parseResponse(llmResponse, request);
            response.setRawResponse(llmResponse);
            
            return response;
        } catch (Exception e) {
            logger.error("Error creating exam", e);
            return new ExamCreationResponse("error", "Failed to create exam: " + e.getMessage());
        }
    }

    /**
     * Creates a prompt for the LLM based on the request parameters
     * 
     * @param request The exam creation request
     * @return The prompt for the LLM
     */
    private String createExamPrompt(ExamCreationRequest request) {
        StringBuilder promptBuilder = new StringBuilder();

        // Generic exam creation prompt
        promptBuilder.append("Create an exam with the following specifications:\n\n");
        
        // Add request parameters to the prompt
        promptBuilder.append("Subject: ").append(request.getSubject()).append("\n");
        promptBuilder.append("Grade/Level: ").append(request.getGradeLevel()).append("\n");
        promptBuilder.append("Exam Type: ").append(request.getExamType()).append("\n");
        promptBuilder.append("Number of Questions: ").append(request.getNumberOfQuestions()).append("\n\n");
        
        // Add instructions for the response format
        promptBuilder.append("Please format your response as a JSON object with the following structure:\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"subject\": \"The subject of the exam\",\n");
        promptBuilder.append("  \"gradeLevel\": \"The grade level of the exam\",\n");
        promptBuilder.append("  \"examType\": \"The type of exam\",\n");
        promptBuilder.append("  \"questions\": [\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"questionText\": \"The text of the question\",\n");
        promptBuilder.append("      \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"],\n");
        promptBuilder.append("      \"correctAnswer\": \"The correct answer (e.g., 'Option A')\",\n");
        promptBuilder.append("      \"explanation\": \"Explanation of the correct answer\"\n");
        promptBuilder.append("    }\n");
        promptBuilder.append("  ]\n");
        promptBuilder.append("}\n\n");
        
        // Add custom prompt if provided
        if (request.getCustomPrompt() != null && !request.getCustomPrompt().trim().isEmpty()) {
            promptBuilder.append("Additional Instructions: ").append(request.getCustomPrompt()).append("\n");
        }

        return promptBuilder.toString();
    }

    /**
     * Calls the Gemini API with the provided prompt
     * 
     * @param prompt The prompt for the LLM
     * @return The raw response from the LLM
     */
    private String callGeminiApi(String prompt) {
        try {
            String url;
            
            // If the model name already contains "models/", remove it from the URL to avoid duplication
            if (geminiModel.startsWith("models/")) {
                url = GEMINI_REST_API_URL + geminiModel + ":generateContent";
            } else {
                url = GEMINI_REST_API_URL + "models/" + geminiModel + ":generateContent";
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-goog-api-key", geminiApiKey);
            
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> contents = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            
            part.put("text", prompt);
            List<Map<String, Object>> parts = Collections.singletonList(part);
            contents.put("parts", parts);
            contents.put("role", "user");
            
            requestBody.put("contents", Collections.singletonList(contents));
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            String response = restTemplate.postForObject(url, entity, String.class);
            return response;
        } catch (Exception e) {
            logger.error("Error calling Gemini API", e);
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the raw LLM response into a structured exam creation response
     * 
     * @param rawResponse The raw response from the LLM
     * @param request The original exam creation request
     * @return The structured exam creation response
     */
    private ExamCreationResponse parseResponse(String rawResponse, ExamCreationRequest request) {
        try {
            JsonNode responseJson = objectMapper.readTree(rawResponse);
            
            // Extract the text content from the response
            String textContent = "";
            if (responseJson.has("candidates") && responseJson.get("candidates").isArray() && 
                responseJson.get("candidates").size() > 0) {
                
                JsonNode candidate = responseJson.get("candidates").get(0);
                if (candidate.has("content") && candidate.get("content").has("parts") && 
                    candidate.get("content").get("parts").isArray() && 
                    candidate.get("content").get("parts").size() > 0) {
                    
                    JsonNode part = candidate.get("content").get("parts").get(0);
                    if (part.has("text")) {
                        textContent = part.get("text").asText();
                    }
                }
            }
            
            if (textContent.isEmpty()) {
                return new ExamCreationResponse("error", "Failed to extract content from LLM response");
            }
            
            // Extract JSON from the text content
            String jsonContent = extractJsonFromText(textContent);
            if (jsonContent.isEmpty()) {
                return new ExamCreationResponse("error", "Failed to extract JSON from LLM response");
            }
            
            // Parse the JSON content
            JsonNode examJson = objectMapper.readTree(jsonContent);
            
            // Create the exam data
            ExamCreationResponse.ExamData examData = new ExamCreationResponse.ExamData();
            examData.setSubject(examJson.has("subject") ? examJson.get("subject").asText() : request.getSubject());
            examData.setGradeLevel(examJson.has("gradeLevel") ? examJson.get("gradeLevel").asText() : request.getGradeLevel());
            examData.setExamType(examJson.has("examType") ? examJson.get("examType").asText() : request.getExamType());
            
            // Parse questions
            List<ExamCreationResponse.Question> questions = new ArrayList<>();
            if (examJson.has("questions") && examJson.get("questions").isArray()) {
                for (JsonNode questionNode : examJson.get("questions")) {
                    ExamCreationResponse.Question question = new ExamCreationResponse.Question();
                    
                    if (questionNode.has("questionText")) {
                        question.setQuestionText(questionNode.get("questionText").asText());
                    }
                    
                    if (questionNode.has("options") && questionNode.get("options").isArray()) {
                        List<String> options = new ArrayList<>();
                        for (JsonNode optionNode : questionNode.get("options")) {
                            options.add(optionNode.asText());
                        }
                        question.setOptions(options);
                    }
                    
                    if (questionNode.has("correctAnswer")) {
                        question.setCorrectAnswer(questionNode.get("correctAnswer").asText());
                    }
                    
                    if (questionNode.has("explanation")) {
                        question.setExplanation(questionNode.get("explanation").asText());
                    }
                    
                    questions.add(question);
                }
            }
            
            examData.setQuestions(questions);
            
            return new ExamCreationResponse("success", "Exam created successfully", examData);
        } catch (Exception e) {
            logger.error("Error parsing LLM response", e);
            return new ExamCreationResponse("error", "Failed to parse LLM response: " + e.getMessage());
        }
    }

    /**
     * Extracts JSON content from text that may contain markdown or other formatting
     * 
     * @param text The text to extract JSON from
     * @return The extracted JSON content
     */
    private String extractJsonFromText(String text) {
        // Look for JSON content between triple backticks
        int startIndex = text.indexOf("```json");
        if (startIndex != -1) {
            startIndex += 7; // Length of "```json"
            int endIndex = text.indexOf("```", startIndex);
            if (endIndex != -1) {
                return text.substring(startIndex, endIndex).trim();
            }
        }
        
        // Look for JSON content between single backticks
        startIndex = text.indexOf("`{");
        if (startIndex != -1) {
            startIndex += 1; // Length of "`"
            int endIndex = text.lastIndexOf("}`");
            if (endIndex != -1) {
                return text.substring(startIndex, endIndex + 1).trim();
            }
        }
        
        // Look for JSON content starting with { and ending with }
        startIndex = text.indexOf("{");
        if (startIndex != -1) {
            int endIndex = text.lastIndexOf("}");
            if (endIndex != -1) {
                return text.substring(startIndex, endIndex + 1).trim();
            }
        }
        
        return "";
    }
}
