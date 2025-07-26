package com.sahayak.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public class WorksheetEvaluationResponse {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("studentName")
    private String studentName;
    
    @JsonProperty("studentId")
    private String studentId;
    
    @JsonProperty("worksheetTitle")
    private String worksheetTitle;
    
    @JsonProperty("subject")
    private String subject;
    
    @JsonProperty("evaluation")
    private EvaluationResult evaluation;
    
    @JsonProperty("processingTime")
    private String processingTime;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("error")
    private String error;
    
    // Default constructor
    public WorksheetEvaluationResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    // Constructor for success response
    public WorksheetEvaluationResponse(String studentName, String studentId, String worksheetTitle, 
                                     String subject, EvaluationResult evaluation) {
        this();
        this.status = "success";
        this.studentName = studentName;
        this.studentId = studentId;
        this.worksheetTitle = worksheetTitle;
        this.subject = subject;
        this.evaluation = evaluation;
    }
    
    // Constructor for error response
    public WorksheetEvaluationResponse(String status, String error) {
        this();
        this.status = status;
        this.error = error;
    }
    
    // Getters and Setters
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getStudentName() {
        return studentName;
    }
    
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
    
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public String getWorksheetTitle() {
        return worksheetTitle;
    }
    
    public void setWorksheetTitle(String worksheetTitle) {
        this.worksheetTitle = worksheetTitle;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public EvaluationResult getEvaluation() {
        return evaluation;
    }
    
    public void setEvaluation(EvaluationResult evaluation) {
        this.evaluation = evaluation;
    }
    
    public String getProcessingTime() {
        return processingTime;
    }
    
    public void setProcessingTime(String processingTime) {
        this.processingTime = processingTime;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    // Inner class for evaluation result
    public static class EvaluationResult {
        
        @JsonProperty("totalScore")
        private double totalScore;
        
        @JsonProperty("maxPossibleScore")
        private double maxPossibleScore;
        
        @JsonProperty("percentage")
        private double percentage;
        
        @JsonProperty("questionsAnalyzed")
        private int questionsAnalyzed;
        
        @JsonProperty("questionWiseResults")
        private List<QuestionResult> questionWiseResults;
        
        @JsonProperty("overallFeedback")
        private String overallFeedback;
        
        @JsonProperty("strengths")
        private List<String> strengths;
        
        @JsonProperty("areasForImprovement")
        private List<String> areasForImprovement;
        
        @JsonProperty("teacherRecommendations")
        private String teacherRecommendations;
        
        // Default constructor
        public EvaluationResult() {}
        
        // Getters and Setters
        public double getTotalScore() {
            return totalScore;
        }
        
        public void setTotalScore(double totalScore) {
            this.totalScore = totalScore;
        }
        
        public double getMaxPossibleScore() {
            return maxPossibleScore;
        }
        
        public void setMaxPossibleScore(double maxPossibleScore) {
            this.maxPossibleScore = maxPossibleScore;
        }
        
        public double getPercentage() {
            return percentage;
        }
        
        public void setPercentage(double percentage) {
            this.percentage = percentage;
        }
        
        public int getQuestionsAnalyzed() {
            return questionsAnalyzed;
        }
        
        public void setQuestionsAnalyzed(int questionsAnalyzed) {
            this.questionsAnalyzed = questionsAnalyzed;
        }
        
        public List<QuestionResult> getQuestionWiseResults() {
            return questionWiseResults;
        }
        
        public void setQuestionWiseResults(List<QuestionResult> questionWiseResults) {
            this.questionWiseResults = questionWiseResults;
        }
        
        public String getOverallFeedback() {
            return overallFeedback;
        }
        
        public void setOverallFeedback(String overallFeedback) {
            this.overallFeedback = overallFeedback;
        }
        
        public List<String> getStrengths() {
            return strengths;
        }
        
        public void setStrengths(List<String> strengths) {
            this.strengths = strengths;
        }
        
        public List<String> getAreasForImprovement() {
            return areasForImprovement;
        }
        
        public void setAreasForImprovement(List<String> areasForImprovement) {
            this.areasForImprovement = areasForImprovement;
        }
        
        public String getTeacherRecommendations() {
            return teacherRecommendations;
        }
        
        public void setTeacherRecommendations(String teacherRecommendations) {
            this.teacherRecommendations = teacherRecommendations;
        }
    }
    
    // Inner class for individual question results
    public static class QuestionResult {
        
        @JsonProperty("questionNumber")
        private int questionNumber;
        
        @JsonProperty("questionText")
        private String questionText;
        
        @JsonProperty("studentAnswer")
        private String studentAnswer;
        
        @JsonProperty("correctAnswer")
        private String correctAnswer;
        
        @JsonProperty("pointsAwarded")
        private double pointsAwarded;
        
        @JsonProperty("maxPoints")
        private double maxPoints;
        
        @JsonProperty("feedback")
        private String feedback;
        
        // Default constructor
        public QuestionResult() {}
        
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
        
        public String getStudentAnswer() {
            return studentAnswer;
        }
        
        public void setStudentAnswer(String studentAnswer) {
            this.studentAnswer = studentAnswer;
        }
        
        public String getCorrectAnswer() {
            return correctAnswer;
        }
        
        public void setCorrectAnswer(String correctAnswer) {
            this.correctAnswer = correctAnswer;
        }
        
        public double getPointsAwarded() {
            return pointsAwarded;
        }
        
        public void setPointsAwarded(double pointsAwarded) {
            this.pointsAwarded = pointsAwarded;
        }
        
        public double getMaxPoints() {
            return maxPoints;
        }
        
        public void setMaxPoints(double maxPoints) {
            this.maxPoints = maxPoints;
        }
        
        public String getFeedback() {
            return feedback;
        }
        
        public void setFeedback(String feedback) {
            this.feedback = feedback;
        }
    }
}
