package com.sahayak.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.sahayak.model.ExamCreationRequest;
import com.sahayak.model.ExamCreationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strategy implementation for Mixed exam type.
 * This strategy creates exams with a mix of Multiple Choice, True/False, and Short Answer questions
 * based on specified proportions.
 */
public class MixedExamStrategy implements ExamTypeStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MixedExamStrategy.class);
    
    // Default proportions if not specified in the prompt
    private static final Map<String, Integer> DEFAULT_PROPORTIONS = new HashMap<>();
    static {
        DEFAULT_PROPORTIONS.put("MULTIPLE_CHOICE", 50);
        DEFAULT_PROPORTIONS.put("SHORT_ANSWER", 50);
    }

    /**
     * Parses the custom prompt to extract question type proportions.
     * Expected format: "...X% Multiple Choice, Y% True/False, Z% Short Answer..."
     * 
     * @param customPrompt The custom prompt to parse
     * @return A map of question types to their proportions (percentages)
     */
    private Map<String, Integer> parseProportions(String customPrompt) {
        if (customPrompt == null || customPrompt.trim().isEmpty()) {
            return DEFAULT_PROPORTIONS;
        }
        
        Map<String, Integer> proportions = new HashMap<>();
        
        // Define patterns to match different formats of proportion specifications
        Pattern multipleChoicePattern = Pattern.compile("(\\d+)\\s*%\\s*(multiple\\s*choice|mc)", Pattern.CASE_INSENSITIVE);
        Pattern trueFalsePattern = Pattern.compile("(\\d+)\\s*%\\s*(true\\s*false|tf)", Pattern.CASE_INSENSITIVE);
        Pattern shortAnswerPattern = Pattern.compile("(\\d+)\\s*%\\s*(short\\s*answer|sa)", Pattern.CASE_INSENSITIVE);
        Pattern essayPattern = Pattern.compile("(\\d+)\\s*%\\s*(essay)", Pattern.CASE_INSENSITIVE);
        
        // Extract proportions for each question type
        Matcher mcMatcher = multipleChoicePattern.matcher(customPrompt);
        if (mcMatcher.find()) {
            proportions.put("MULTIPLE_CHOICE", Integer.parseInt(mcMatcher.group(1)));
        }
        
        Matcher tfMatcher = trueFalsePattern.matcher(customPrompt);
        if (tfMatcher.find()) {
            proportions.put("TRUE_FALSE", Integer.parseInt(tfMatcher.group(1)));
        }
        
        Matcher saMatcher = shortAnswerPattern.matcher(customPrompt);
        if (saMatcher.find()) {
            proportions.put("SHORT_ANSWER", Integer.parseInt(saMatcher.group(1)));
        }
        
        Matcher essayMatcher = essayPattern.matcher(customPrompt);
        if (essayMatcher.find()) {
            proportions.put("ESSAY", Integer.parseInt(essayMatcher.group(1)));
        }
        
        // If no proportions were found, use defaults
        if (proportions.isEmpty()) {
            return DEFAULT_PROPORTIONS;
        }
        
        // Validate that proportions sum to 100%
        int sum = proportions.values().stream().mapToInt(Integer::intValue).sum();
        if (sum != 100) {
            logger.warn("Proportions do not sum to 100%. Using default proportions.");
            return DEFAULT_PROPORTIONS;
        }
        
        return proportions;
    }
    
    /**
     * Calculates the number of questions for each type based on proportions.
     * 
     * @param totalQuestions Total number of questions
     * @param proportions Map of question types to their proportions
     * @return Map of question types to the number of questions
     */
    private Map<String, Integer> calculateQuestionCounts(int totalQuestions, Map<String, Integer> proportions) {
        Map<String, Integer> questionCounts = new HashMap<>();
        int remainingQuestions = totalQuestions;
        
        // Calculate question counts for each type except the last one
        for (Map.Entry<String, Integer> entry : proportions.entrySet()) {
            if (remainingQuestions <= 0) {
                break;
            }
            
            String questionType = entry.getKey();
            int proportion = entry.getValue();
            
            // For the last type, assign all remaining questions
            if (questionCounts.size() == proportions.size() - 1) {
                questionCounts.put(questionType, remainingQuestions);
                break;
            }
            
            // Calculate number of questions for this type
            int count = (int) Math.round((proportion / 100.0) * totalQuestions);
            
            // Ensure we don't exceed the total
            count = Math.min(count, remainingQuestions);
            
            questionCounts.put(questionType, count);
            remainingQuestions -= count;
        }
        
        return questionCounts;
    }

    @Override
    public String createExamPrompt(ExamCreationRequest request) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Create a mixed format exam with the following specifications:\n\n");
        
        // Add request parameters to the prompt
        promptBuilder.append("Subject: ").append(request.getSubject()).append("\n");
        promptBuilder.append("Grade/Level: ").append(request.getGradeLevel()).append("\n");
        promptBuilder.append("Exam Type: MIXED\n");
        promptBuilder.append("Number of Questions: ").append(request.getNumberOfQuestions()).append("\n\n");
        
        // Parse proportions from custom prompt if provided
        Map<String, Integer> proportions = parseProportions(request.getCustomPrompt());
        Map<String, Integer> questionCounts = calculateQuestionCounts(request.getNumberOfQuestions(), proportions);
        
        // Add information about the question distribution
        promptBuilder.append("Question Distribution:\n");
        for (Map.Entry<String, Integer> entry : questionCounts.entrySet()) {
            promptBuilder.append("- ").append(entry.getKey()).append(": ").append(entry.getValue())
                    .append(" questions (").append(proportions.get(entry.getKey())).append("%)\n");
        }
        promptBuilder.append("\n");
        
        // Add instructions for the response format
        promptBuilder.append("Please format your response as a JSON object with the following structure:\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"subject\": \"The subject of the exam\",\n");
        promptBuilder.append("  \"gradeLevel\": \"The grade level of the exam\",\n");
        promptBuilder.append("  \"examType\": \"MIXED\",\n");
        promptBuilder.append("  \"questions\": [\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"questionType\": \"MULTIPLE_CHOICE or TRUE_FALSE or ESSAY or SHORT_ANSWER\",\n");
        promptBuilder.append("      \"questionText\": \"The text of the question\",\n");
        promptBuilder.append("      \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"],\n");
        promptBuilder.append("      \"correctAnswer\": \"The correct answer (e.g., 'Option A')\",\n");
        promptBuilder.append("      \"explanation\": \"Explanation of the correct answer\"\n");
        promptBuilder.append("    }\n");
        promptBuilder.append("  ]\n");
        promptBuilder.append("}\n\n");
        
        // Add specific instructions for each question type and grouping
        promptBuilder.append("IMPORTANT GUIDELINES FOR DIFFERENT QUESTION TYPES:\n\n");
        promptBuilder.append("IMPORTANT: Group all questions of the same type together in the response. For example, all MULTIPLE_CHOICE questions should be listed first, followed by all TRUE_FALSE questions, then all SHORT_ANSWER questions, and finally all ESSAY questions.\n\n");
        
        // Define the order of question types
        promptBuilder.append("Please follow this order for grouping questions:\n");
        promptBuilder.append("1. First, list all MULTIPLE_CHOICE questions\n");
        promptBuilder.append("2. Then, list all TRUE_FALSE questions\n");
        promptBuilder.append("3. Then, list all SHORT_ANSWER questions\n");
        promptBuilder.append("4. Finally, list all ESSAY questions\n\n");
        
        if (questionCounts.containsKey("MULTIPLE_CHOICE")) {
            promptBuilder.append("For MULTIPLE_CHOICE questions:\n");
            promptBuilder.append("- Include 4 options (A, B, C, D)\n");
            promptBuilder.append("- Clearly indicate the correct answer\n");
            promptBuilder.append("- Provide an explanation for why the answer is correct\n");
            promptBuilder.append("- Group all MULTIPLE_CHOICE questions together\n\n");
        }
        
        if (questionCounts.containsKey("TRUE_FALSE")) {
            promptBuilder.append("For TRUE_FALSE questions:\n");
            promptBuilder.append("- The options should be exactly [\"True\", \"False\"]\n");
            promptBuilder.append("- The correctAnswer should be either \"True\" or \"False\"\n");
            promptBuilder.append("- Provide an explanation for why the statement is true or false\n");
            promptBuilder.append("- Group all TRUE_FALSE questions together\n\n");
        }
        
        if (questionCounts.containsKey("SHORT_ANSWER")) {
            promptBuilder.append("For SHORT_ANSWER questions:\n");
            promptBuilder.append("- Create questions that require brief, concise answers (2-3 lines)\n");
            promptBuilder.append("- The options field should be an empty array []\n");
            promptBuilder.append("- The correctAnswer should contain the expected answer or key points\n");
            promptBuilder.append("- Include a grading rubric in the explanation field\n");
            promptBuilder.append("- Group all SHORT_ANSWER questions together\n\n");
        }
        
        if (questionCounts.containsKey("ESSAY")) {
            promptBuilder.append("For ESSAY questions:\n");
            promptBuilder.append("- Create open-ended questions that require detailed responses\n");
            promptBuilder.append("- The options field should be an empty array []\n");
            promptBuilder.append("- The correctAnswer can be empty or contain key points to include\n");
            promptBuilder.append("- Provide a detailed rubric in the explanation field\n");
            promptBuilder.append("- Group all ESSAY questions together\n\n");
        }
        
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
            examData.setExamType("MIXED");
            
            // Parse questions
            List<ExamCreationResponse.Question> questions = new ArrayList<>();
            if (examJson.has("questions") && examJson.get("questions").isArray()) {
                for (JsonNode questionNode : examJson.get("questions")) {
                    ExamCreationResponse.Question question = new ExamCreationResponse.Question();
                    
                    if (questionNode.has("questionText")) {
                        question.setQuestionText(questionNode.get("questionText").asText());
                    }
                    
                    // Handle different question types
                    String questionType = questionNode.has("questionType") ? 
                            questionNode.get("questionType").asText() : "MULTIPLE_CHOICE";
                    
                    // Set the question type
                    question.setQuestionType(questionType.toUpperCase());
                    
                    // Handle options based on question type
                    if ("TRUE_FALSE".equalsIgnoreCase(questionType)) {
                        question.setOptions(Arrays.asList("True", "False"));
                    } else if ("MULTIPLE_CHOICE".equalsIgnoreCase(questionType)) {
                        if (questionNode.has("options") && questionNode.get("options").isArray()) {
                            List<String> options = new ArrayList<>();
                            for (JsonNode optionNode : questionNode.get("options")) {
                                options.add(optionNode.asText());
                            }
                            question.setOptions(options);
                        } else {
                            // Default options if not provided
                            question.setOptions(Arrays.asList("Option A", "Option B", "Option C", "Option D"));
                        }
                    } else {
                        // For SHORT_ANSWER and ESSAY, set empty options
                        question.setOptions(new ArrayList<>());
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
            return examData;
        } catch (Exception e) {
            logger.error("Error parsing mixed exam data", e);
            throw new RuntimeException("Failed to parse mixed exam data: " + e.getMessage(), e);
        }
    }
}
