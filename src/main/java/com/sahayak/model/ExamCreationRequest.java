package com.sahayak.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExamCreationRequest {
    private String subject;
    private String gradeLevel;
    private String examType;
    private int numberOfQuestions;
    private String customPrompt;

    // Default constructor
    public ExamCreationRequest() {
    }

    // Constructor with all fields
    public ExamCreationRequest(String subject, String gradeLevel, String examType, int numberOfQuestions, String customPrompt) {
        this.subject = subject;
        this.gradeLevel = gradeLevel;
        this.examType = examType;
        this.numberOfQuestions = numberOfQuestions;
        this.customPrompt = customPrompt;
    }

    // Getters and setters
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

    public int getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(int numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public String getCustomPrompt() {
        return customPrompt;
    }

    public void setCustomPrompt(String customPrompt) {
        this.customPrompt = customPrompt;
    }

    @Override
    public String toString() {
        return "ExamCreationRequest{" +
                "subject='" + subject + '\'' +
                ", gradeLevel='" + gradeLevel + '\'' +
                ", examType='" + examType + '\'' +
                ", numberOfQuestions=" + numberOfQuestions +
                ", customPrompt='" + customPrompt + '\'' +
                '}';
    }
}
