package com.sahayak.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QuestionPaperEvaluationRequest {
    
    @JsonProperty("studentName")
    private String studentName;
    
    @JsonProperty("studentId")
    private String studentId;
    
    @JsonProperty("subject")
    private String subject;
    
    @JsonProperty("examTitle")
    private String examTitle;
    
    @JsonProperty("evaluationCriteria")
    private String evaluationCriteria = "moderate"; // strict|lenient|moderate
    
    @JsonProperty("additionalInstructions")
    private String additionalInstructions;
    
    @JsonProperty("teacherNotes")
    private String teacherNotes;
    
    // Default constructor
    public QuestionPaperEvaluationRequest() {}
    
    // Constructor with required fields
    public QuestionPaperEvaluationRequest(String studentName, String subject, String examTitle) {
        this.studentName = studentName;
        this.subject = subject;
        this.examTitle = examTitle;
    }
    
    // Getters and Setters
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
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getExamTitle() {
        return examTitle;
    }
    
    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }
    
    public String getEvaluationCriteria() {
        return evaluationCriteria;
    }
    
    public void setEvaluationCriteria(String evaluationCriteria) {
        this.evaluationCriteria = evaluationCriteria;
    }
    
    public String getAdditionalInstructions() {
        return additionalInstructions;
    }
    
    public void setAdditionalInstructions(String additionalInstructions) {
        this.additionalInstructions = additionalInstructions;
    }
    
    public String getTeacherNotes() {
        return teacherNotes;
    }
    
    public void setTeacherNotes(String teacherNotes) {
        this.teacherNotes = teacherNotes;
    }
    
    @Override
    public String toString() {
        return "QuestionPaperEvaluationRequest{" +
                "studentName='" + studentName + '\'' +
                ", studentId='" + studentId + '\'' +
                ", subject='" + subject + '\'' +
                ", examTitle='" + examTitle + '\'' +
                ", evaluationCriteria='" + evaluationCriteria + '\'' +
                ", additionalInstructions='" + additionalInstructions + '\'' +
                ", teacherNotes='" + teacherNotes + '\'' +
                '}';
    }
}
