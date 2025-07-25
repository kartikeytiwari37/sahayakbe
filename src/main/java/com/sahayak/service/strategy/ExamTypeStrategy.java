package com.sahayak.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.sahayak.model.ExamCreationRequest;
import com.sahayak.model.ExamCreationResponse;

/**
 * Strategy interface for different exam types.
 * Each exam type will have its own implementation of this interface.
 */
public interface ExamTypeStrategy {
    
    /**
     * Creates a prompt for the LLM based on the request parameters
     * 
     * @param request The exam creation request
     * @return The prompt for the LLM
     */
    String createExamPrompt(ExamCreationRequest request);
    
    /**
     * Parses the JSON content from the LLM response into a structured exam data
     * 
     * @param examJson The JSON content from the LLM response
     * @param request The original exam creation request
     * @return The structured exam data
     */
    ExamCreationResponse.ExamData parseExamData(JsonNode examJson, ExamCreationRequest request);
}
