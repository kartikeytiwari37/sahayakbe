package com.sahayak.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.sahayak.model.ExamCreationRequest;
import com.sahayak.model.ExamCreationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Strategy implementation for Short Answer exam type.
 * This strategy creates exams with questions that require brief, concise answers (typically 2-3 lines).
 */
public class ShortAnswerExamStrategy implements ExamTypeStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ShortAnswerExamStrategy.class);

    @Override
    public String createExamPrompt(ExamCreationRequest request) {
        logger.info("Creating Short Answer exam prompt for subject: {}, grade level: {}", 
                request.getSubject(), request.getGradeLevel());
        
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Create a short answer exam with the following specifications:\n\n");
        
        // Add request parameters to the prompt
        promptBuilder.append("Subject: ").append(request.getSubject()).append("\n");
        promptBuilder.append("Grade/Level: ").append(request.getGradeLevel()).append("\n");
        promptBuilder.append("Exam Type: SHORT_ANSWER\n");
        promptBuilder.append("Number of Questions: ").append(request.getNumberOfQuestions()).append("\n\n");
        
        // Specific instructions for short answer questions
        promptBuilder.append("IMPORTANT GUIDELINES FOR SHORT ANSWER QUESTIONS:\n");
        promptBuilder.append("- Create questions that require brief, concise answers (typically 2-3 lines of text)\n");
        promptBuilder.append("- Questions should test understanding of key concepts, definitions, or applications\n");
        promptBuilder.append("- Avoid questions that can be answered with just a single word\n");
        promptBuilder.append("- Also avoid questions that would require lengthy explanations (use Essay type for those)\n");
        promptBuilder.append("- For each question, provide a model answer that captures the key points expected\n");
        promptBuilder.append("- Include a clear grading rubric or explanation for each question\n\n");
        
        promptBuilder.append("Please format your response as a JSON object with the following structure:\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"subject\": \"The subject of the exam\",\n");
        promptBuilder.append("  \"gradeLevel\": \"The grade level of the exam\",\n");
        promptBuilder.append("  \"examType\": \"SHORT_ANSWER\",\n");
        promptBuilder.append("  \"questions\": [\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"questionType\": \"SHORT_ANSWER\",\n");
        promptBuilder.append("      \"questionText\": \"The short answer question\",\n");
        promptBuilder.append("      \"correctAnswer\": \"The expected answer (2-3 lines) or key points that should be included\",\n");
        promptBuilder.append("      \"explanation\": \"Explanation of the answer and/or grading rubric (what constitutes a complete answer)\"\n");
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
        logger.info("Parsing Short Answer exam data for subject: {}", 
                examJson.has("subject") ? examJson.get("subject").asText() : request.getSubject());
        
        try {
            // Create the exam data
            ExamCreationResponse.ExamData examData = new ExamCreationResponse.ExamData();
            examData.setSubject(examJson.has("subject") ? examJson.get("subject").asText() : request.getSubject());
            examData.setGradeLevel(examJson.has("gradeLevel") ? examJson.get("gradeLevel").asText() : request.getGradeLevel());
            examData.setExamType("SHORT_ANSWER");
            
            // Parse questions
            List<ExamCreationResponse.Question> questions = new ArrayList<>();
            if (examJson.has("questions") && examJson.get("questions").isArray()) {
                for (JsonNode questionNode : examJson.get("questions")) {
                    ExamCreationResponse.Question question = new ExamCreationResponse.Question();
                    
                    if (questionNode.has("questionText")) {
                        question.setQuestionText(questionNode.get("questionText").asText());
                    } else {
                        logger.warn("Question missing questionText field");
                        question.setQuestionText("Missing question text");
                    }
                    
                    // Short answer questions don't have options like multiple choice
                    // But we'll set an empty list to maintain consistency
                    question.setOptions(new ArrayList<>());
                    
                    if (questionNode.has("correctAnswer")) {
                        question.setCorrectAnswer(questionNode.get("correctAnswer").asText());
                    } else {
                        logger.warn("Short answer question missing correctAnswer field");
                        question.setCorrectAnswer("No model answer provided");
                    }
                    
                    if (questionNode.has("explanation")) {
                        question.setExplanation(questionNode.get("explanation").asText());
                    } else {
                        logger.warn("Short answer question missing explanation field");
                        question.setExplanation("No explanation or grading rubric provided");
                    }
                    
                    // Set the question type
                    question.setQuestionType("SHORT_ANSWER");
                    
                    questions.add(question);
                }
            } else {
                logger.warn("No questions found in the exam data or questions is not an array");
            }
            
            examData.setQuestions(questions);
            return examData;
        } catch (Exception e) {
            logger.error("Error parsing short answer exam data", e);
            throw new RuntimeException("Failed to parse short answer exam data: " + e.getMessage(), e);
        }
    }
}
