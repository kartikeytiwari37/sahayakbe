package com.sahayak.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahayak.model.ExamCreationRequest;
import com.sahayak.model.ExamCreationResponse;
import com.sahayak.service.strategy.ExamTypeStrategy;
import com.sahayak.service.strategy.ExamTypeStrategyFactory;
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

    @Value("${exam.creation.gemini.api.key}")
    private String geminiApiKey;

    @Value("${exam.creation.gemini.api.model}")
    private String geminiModel;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ExamTypeStrategyFactory strategyFactory;

    // REST API endpoint for Gemini (not WebSocket)
    private static final String GEMINI_REST_API_URL = "https://generativelanguage.googleapis.com/v1beta/";

    public ExamCreationService(RestTemplate restTemplate, ObjectMapper objectMapper, ExamTypeStrategyFactory strategyFactory) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.strategyFactory = strategyFactory;
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

            // Get the appropriate strategy for the exam type
            ExamTypeStrategy strategy = strategyFactory.createStrategy(request.getExamType());

            // Create the prompt for the LLM using the strategy
            String prompt = strategy.createExamPrompt(request);
            logger.debug("Generated prompt: {}", prompt);

            // Call the Gemini API
            String llmResponse = callGeminiApi(prompt);
            logger.debug("Raw LLM response: {}", llmResponse);

            // Parse the response using the strategy
            ExamCreationResponse response = parseResponse(llmResponse, request, strategy);
            response.setRawResponse(llmResponse);
            
            return response;
        } catch (Exception e) {
            logger.error("Error creating exam", e);
            return new ExamCreationResponse("error", "Failed to create exam: " + e.getMessage());
        }
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
     * @param strategy The exam type strategy to use for parsing
     * @return The structured exam creation response
     */
    private ExamCreationResponse parseResponse(String rawResponse, ExamCreationRequest request, ExamTypeStrategy strategy) {
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
            
            // Use the strategy to parse the exam data
            ExamCreationResponse.ExamData examData = strategy.parseExamData(examJson, request);
            
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
