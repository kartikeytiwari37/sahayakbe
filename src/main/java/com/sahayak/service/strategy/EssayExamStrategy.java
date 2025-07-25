package com.sahayak.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.sahayak.model.ExamCreationRequest;
import com.sahayak.model.ExamCreationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Strategy implementation for Essay exam type.
 * This is a placeholder implementation that will be completed in the future.
 */
public class EssayExamStrategy implements ExamTypeStrategy {

    private static final Logger logger = LoggerFactory.getLogger(EssayExamStrategy.class);

    @Override
    public String createExamPrompt(ExamCreationRequest request) {
        // This is a placeholder implementation
        logger.warn("Essay exam type is not yet fully implemented");
        
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Create an essay exam with the following specifications:\n\n");
        
        // Add request parameters to the prompt
        promptBuilder.append("Subject: ").append(request.getSubject()).append("\n");
        promptBuilder.append("Grade/Level: ").append(request.getGradeLevel()).append("\n");
        promptBuilder.append("Exam Type: ESSAY\n");
        promptBuilder.append("Number of Questions: ").append(request.getNumberOfQuestions()).append("\n\n");
        
        // For now, we'll use a similar format to multiple choice but note it's for essays
        promptBuilder.append("Please format your response as a JSON object with the following structure:\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"subject\": \"The subject of the exam\",\n");
        promptBuilder.append("  \"gradeLevel\": \"The grade level of the exam\",\n");
        promptBuilder.append("  \"examType\": \"ESSAY\",\n");
        promptBuilder.append("  \"questions\": [\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"questionText\": \"The essay prompt or question\",\n");
        promptBuilder.append("      \"explanation\": \"Guidance or rubric for the essay\"\n");
        promptBuilder.append("    }\n");
        promptBuilder.append("  ]\n");
        promptBuilder.append("}\n\n");
        
        // Add custom prompt if provided
        if (request.getCustomPrompt() != null && !request.getCustomPrompt().trim().isEmpty()) {
            promptBuilder.append("Additional Instructions: ").append(request.getCustomPrompt()).append("\n");
        }

        return promptBuilder.toString();
    }

    @Override
    public ExamCreationResponse.ExamData parseExamData(JsonNode examJson, ExamCreationRequest request) {
        // This is a placeholder implementation
        logger.warn("Essay exam type parsing is not yet fully implemented");
        
        try {
            // Create the exam data
            ExamCreationResponse.ExamData examData = new ExamCreationResponse.ExamData();
            examData.setSubject(examJson.has("subject") ? examJson.get("subject").asText() : request.getSubject());
            examData.setGradeLevel(examJson.has("gradeLevel") ? examJson.get("gradeLevel").asText() : request.getGradeLevel());
            examData.setExamType("ESSAY");
            
            // Parse questions
            List<ExamCreationResponse.Question> questions = new ArrayList<>();
            if (examJson.has("questions") && examJson.get("questions").isArray()) {
                for (JsonNode questionNode : examJson.get("questions")) {
                    ExamCreationResponse.Question question = new ExamCreationResponse.Question();
                    
                    if (questionNode.has("questionText")) {
                        question.setQuestionText(questionNode.get("questionText").asText());
                    }
                    
                    // Essay questions don't have options or correct answers in the same way
                    // For now, we'll leave these fields empty or with placeholder values
                    
                    if (questionNode.has("explanation")) {
                        question.setExplanation(questionNode.get("explanation").asText());
                    }
                    
                    questions.add(question);
                }
            }
            
            examData.setQuestions(questions);
            return examData;
        } catch (Exception e) {
            logger.error("Error parsing essay exam data", e);
            throw new RuntimeException("Failed to parse essay exam data: " + e.getMessage(), e);
        }
    }
}
