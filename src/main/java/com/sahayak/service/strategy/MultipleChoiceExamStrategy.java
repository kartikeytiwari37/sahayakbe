package com.sahayak.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.sahayak.model.ExamCreationRequest;
import com.sahayak.model.ExamCreationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Strategy implementation for Multiple Choice exam type.
 */
public class MultipleChoiceExamStrategy implements ExamTypeStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MultipleChoiceExamStrategy.class);

    @Override
    public String createExamPrompt(ExamCreationRequest request) {
        StringBuilder promptBuilder = new StringBuilder();

        // Generic exam creation prompt
        promptBuilder.append("Create a multiple choice exam with the following specifications:\n\n");
        
        // Add request parameters to the prompt
        promptBuilder.append("Subject: ").append(request.getSubject()).append("\n");
        promptBuilder.append("Grade/Level: ").append(request.getGradeLevel()).append("\n");
        promptBuilder.append("Exam Type: MULTIPLE_CHOICE\n");
        promptBuilder.append("Number of Questions: ").append(request.getNumberOfQuestions()).append("\n\n");
        
        // Add instructions for the response format
        promptBuilder.append("Please format your response as a JSON object with the following structure:\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"subject\": \"The subject of the exam\",\n");
        promptBuilder.append("  \"gradeLevel\": \"The grade level of the exam\",\n");
        promptBuilder.append("  \"examType\": \"MULTIPLE_CHOICE\",\n");
        promptBuilder.append("  \"questions\": [\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"questionType\": \"MULTIPLE_CHOICE\",\n");
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

    @Override
    public ExamCreationResponse.ExamData parseExamData(JsonNode examJson, ExamCreationRequest request) {
        try {
            // Create the exam data
            ExamCreationResponse.ExamData examData = new ExamCreationResponse.ExamData();
            examData.setSubject(examJson.has("subject") ? examJson.get("subject").asText() : request.getSubject());
            examData.setGradeLevel(examJson.has("gradeLevel") ? examJson.get("gradeLevel").asText() : request.getGradeLevel());
            examData.setExamType("MULTIPLE_CHOICE");
            
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
                    
                    // Set the question type
                    question.setQuestionType("MULTIPLE_CHOICE");
                    
                    questions.add(question);
                }
            }
            
            examData.setQuestions(questions);
            return examData;
        } catch (Exception e) {
            logger.error("Error parsing exam data", e);
            throw new RuntimeException("Failed to parse exam data: " + e.getMessage(), e);
        }
    }
}
