package com.sahayak.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class QuestionPaperAnalysisResult {
    
    @JsonProperty("examTitle")
    private String examTitle;
    
    @JsonProperty("subject")
    private String subject;
    
    @JsonProperty("totalMarks")
    private double totalMarks;
    
    @JsonProperty("totalQuestions")
    private int totalQuestions;
    
    @JsonProperty("instructions")
    private String instructions;
    
    @JsonProperty("questions")
    private List<Question> questions;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("error")
    private String error;
    
    // Default constructor
    public QuestionPaperAnalysisResult() {}
    
    // Constructor for success
    public QuestionPaperAnalysisResult(String examTitle, String subject, double totalMarks, 
                                     int totalQuestions, List<Question> questions) {
        this.examTitle = examTitle;
        this.subject = subject;
        this.totalMarks = totalMarks;
        this.totalQuestions = totalQuestions;
        this.questions = questions;
        this.status = "success";
    }
    
    // Constructor for error
    public QuestionPaperAnalysisResult(String status, String error) {
        this.status = status;
        this.error = error;
    }
    
    // Getters and Setters
    public String getExamTitle() {
        return examTitle;
    }
    
    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public double getTotalMarks() {
        return totalMarks;
    }
    
    public void setTotalMarks(double totalMarks) {
        this.totalMarks = totalMarks;
    }
    
    public int getTotalQuestions() {
        return totalQuestions;
    }
    
    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }
    
    public String getInstructions() {
        return instructions;
    }
    
    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
    
    public List<Question> getQuestions() {
        return questions;
    }
    
    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    @Override
    public String toString() {
        return "QuestionPaperAnalysisResult{" +
                "examTitle='" + examTitle + '\'' +
                ", subject='" + subject + '\'' +
                ", totalMarks=" + totalMarks +
                ", totalQuestions=" + totalQuestions +
                ", instructions='" + instructions + '\'' +
                ", questions=" + questions +
                ", status='" + status + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
    
    // Inner class for individual questions
    public static class Question {
        
        @JsonProperty("questionNumber")
        private int questionNumber;
        
        @JsonProperty("questionText")
        private String questionText;
        
        @JsonProperty("questionType")
        private String questionType; // MCQ, SHORT_ANSWER, ESSAY, TRUE_FALSE, FILL_BLANK
        
        @JsonProperty("marks")
        private double marks;
        
        @JsonProperty("options")
        private List<String> options; // For MCQ questions
        
        @JsonProperty("correctAnswer")
        private String correctAnswer; // For questions with definitive answers
        
        @JsonProperty("scoringRubric")
        private String scoringRubric; // For essay/subjective questions
        
        @JsonProperty("keywords")
        private List<String> keywords; // Expected keywords for partial scoring
        
        @JsonProperty("subQuestions")
        private List<Question> subQuestions; // For questions with parts (a), (b), etc.
        
        // Default constructor
        public Question() {}
        
        // Constructor with basic fields
        public Question(int questionNumber, String questionText, String questionType, double marks) {
            this.questionNumber = questionNumber;
            this.questionText = questionText;
            this.questionType = questionType;
            this.marks = marks;
        }
        
        // Getters and Setters
        public int getQuestionNumber() {
            return questionNumber;
        }
        
        public void setQuestionNumber(int questionNumber) {
            this.questionNumber = questionNumber;
        }
        
        public String getQuestionText() {
            return questionText;
        }
        
        public void setQuestionText(String questionText) {
            this.questionText = questionText;
        }
        
        public String getQuestionType() {
            return questionType;
        }
        
        public void setQuestionType(String questionType) {
            this.questionType = questionType;
        }
        
        public double getMarks() {
            return marks;
        }
        
        public void setMarks(double marks) {
            this.marks = marks;
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
        
        public String getScoringRubric() {
            return scoringRubric;
        }
        
        public void setScoringRubric(String scoringRubric) {
            this.scoringRubric = scoringRubric;
        }
        
        public List<String> getKeywords() {
            return keywords;
        }
        
        public void setKeywords(List<String> keywords) {
            this.keywords = keywords;
        }
        
        public List<Question> getSubQuestions() {
            return subQuestions;
        }
        
        public void setSubQuestions(List<Question> subQuestions) {
            this.subQuestions = subQuestions;
        }
        
        @Override
        public String toString() {
            return "Question{" +
                    "questionNumber=" + questionNumber +
                    ", questionText='" + questionText + '\'' +
                    ", questionType='" + questionType + '\'' +
                    ", marks=" + marks +
                    ", options=" + options +
                    ", correctAnswer='" + correctAnswer + '\'' +
                    ", scoringRubric='" + scoringRubric + '\'' +
                    ", keywords=" + keywords +
                    ", subQuestions=" + subQuestions +
                    '}';
        }
    }
}
