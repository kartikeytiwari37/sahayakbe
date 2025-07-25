package com.sahayak.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExamCreationResponse {
    private String status;
    private String message;
    private ExamData examData;
    private String rawResponse;
    private String error;

    // Default constructor
    public ExamCreationResponse() {
    }

    // Success constructor
    public ExamCreationResponse(String status, String message, ExamData examData) {
        this.status = status;
        this.message = message;
        this.examData = examData;
    }

    // Error constructor
    public ExamCreationResponse(String status, String error) {
        this.status = status;
        this.error = error;
    }

    // Getters and setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ExamData getExamData() {
        return examData;
    }

    public void setExamData(ExamData examData) {
        this.examData = examData;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    // Inner class for exam data
    public static class ExamData {
        private String subject;
        private String gradeLevel;
        private String examType;
        private List<Question> questions;

        public ExamData() {
        }

        public ExamData(String subject, String gradeLevel, String examType, List<Question> questions) {
            this.subject = subject;
            this.gradeLevel = gradeLevel;
            this.examType = examType;
            this.questions = questions;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getGradeLevel() {
            return gradeLevel;
        }

        public void setGradeLevel(String gradeLevel) {
            this.gradeLevel = gradeLevel;
        }

        public String getExamType() {
            return examType;
        }

        public void setExamType(String examType) {
            this.examType = examType;
        }

        public List<Question> getQuestions() {
            return questions;
        }

        public void setQuestions(List<Question> questions) {
            this.questions = questions;
        }
    }

    // Inner class for question
    public static class Question {
        private String questionText;
        private List<String> options;
        private String correctAnswer;
        private String explanation;
        private String questionType; // Enum format: MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER, ESSAY

        public Question() {
        }

        public Question(String questionText, List<String> options, String correctAnswer, String explanation, String questionType) {
            this.questionText = questionText;
            this.options = options;
            this.correctAnswer = correctAnswer;
            this.explanation = explanation;
            this.questionType = questionType;
        }

        public String getQuestionText() {
            return questionText;
        }

        public void setQuestionText(String questionText) {
            this.questionText = questionText;
        }

        public List<String> getOptions() {
            return options;
        }

        public void setOptions(List<String> options) {
            this.options = options;
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        public void setCorrectAnswer(String correctAnswer) {
            this.correctAnswer = correctAnswer;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }

        public String getQuestionType() {
            return questionType;
        }

        public void setQuestionType(String questionType) {
            this.questionType = questionType;
        }
    }
}
