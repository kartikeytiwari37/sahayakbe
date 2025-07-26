package com.sahayak.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WorksheetEvaluationRequest {
    
    @JsonProperty("studentName")
    private String studentName;
    
    @JsonProperty("studentId")
    private String studentId;
    
    @JsonProperty("subject")
    private String subject;
    
    @JsonProperty("worksheetTitle")
    private String worksheetTitle;
    
    @JsonProperty("evaluationCriteria")
    private String evaluationCriteria = "moderate"; // strict|lenient|moderate
    
    @JsonProperty("additionalInstructions")
    private String additionalInstructions;
    
    @JsonProperty("teacherNotes")
    private String teacherNotes;
    
    // Default constructor
    public WorksheetEvaluationRequest() {}
    
    // Constructor with required fields
    public WorksheetEvaluationRequest(String studentName, String subject, String worksheetTitle) {
        this.studentName = studentName;
        this.subject = subject;
        this.worksheetTitle = worksheetTitle;
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
    
    public String getWorksheetTitle() {
        return worksheetTitle;
    }
    
    public void setWorksheetTitle(String worksheetTitle) {
        this.worksheetTitle = worksheetTitle;
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
        return "WorksheetEvaluationRequest{" +
                "studentName='" + studentName + '\'' +
                ", studentId='" + studentId + '\'' +
                ", subject='" + subject + '\'' +
                ", worksheetTitle='" + worksheetTitle + '\'' +
                ", evaluationCriteria='" + evaluationCriteria + '\'' +
                ", additionalInstructions='" + additionalInstructions + '\'' +
                ", teacherNotes='" + teacherNotes + '\'' +
                '}';
    }
}
