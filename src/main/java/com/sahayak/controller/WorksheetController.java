package com.sahayak.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahayak.model.WorksheetEvaluationRequest;
import com.sahayak.model.WorksheetEvaluationResponse;
import com.sahayak.model.QuestionPaperEvaluationRequest;
import com.sahayak.model.QuestionPaperAnalysisResult;
import com.sahayak.service.WorksheetEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/worksheet")
@CrossOrigin(origins = "*")
public class WorksheetController {
    
    private static final Logger logger = LoggerFactory.getLogger(WorksheetController.class);
    
    private final WorksheetEvaluationService worksheetEvaluationService;
    private final ObjectMapper objectMapper;
    
    public WorksheetController(WorksheetEvaluationService worksheetEvaluationService, ObjectMapper objectMapper) {
        this.worksheetEvaluationService = worksheetEvaluationService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Main endpoint to evaluate a worksheet
     * 
     * @param worksheetFile The worksheet file (PDF, JPG, PNG)
     * @param metadataJson JSON string containing evaluation metadata
     * @return Worksheet evaluation response
     */
    @PostMapping("/evaluate")
    public CompletableFuture<ResponseEntity<WorksheetEvaluationResponse>> evaluateWorksheet(
            @RequestParam("worksheetFile") MultipartFile worksheetFile,
            @RequestParam("metadata") String metadataJson) {
        
        logger.info("Received worksheet evaluation request. File: {}, Size: {} bytes", 
                   worksheetFile.getOriginalFilename(), worksheetFile.getSize());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Parse metadata JSON
                WorksheetEvaluationRequest request = objectMapper.readValue(metadataJson, WorksheetEvaluationRequest.class);
                
                // Validate required fields
                if (request.getStudentName() == null || request.getStudentName().trim().isEmpty()) {
                    WorksheetEvaluationResponse errorResponse = new WorksheetEvaluationResponse("error", "Student name is required");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                
                if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
                    WorksheetEvaluationResponse errorResponse = new WorksheetEvaluationResponse("error", "Subject is required");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                
                if (request.getWorksheetTitle() == null || request.getWorksheetTitle().trim().isEmpty()) {
                    WorksheetEvaluationResponse errorResponse = new WorksheetEvaluationResponse("error", "Worksheet title is required");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                
                logger.info("Processing worksheet evaluation for student: {}, subject: {}, worksheet: {}", 
                           request.getStudentName(), request.getSubject(), request.getWorksheetTitle());
                
                // Process the worksheet evaluation
                return worksheetEvaluationService.evaluateWorksheet(worksheetFile, request)
                    .thenApply(response -> {
                        if ("error".equals(response.getStatus())) {
                            logger.error("Worksheet evaluation failed: {}", response.getError());
                            return ResponseEntity.badRequest().body(response);
                        } else {
                            logger.info("Worksheet evaluation completed successfully for student: {}", 
                                       request.getStudentName());
                            return ResponseEntity.ok(response);
                        }
                    })
                    .exceptionally(throwable -> {
                        logger.error("Unexpected error during worksheet evaluation", throwable);
                        WorksheetEvaluationResponse errorResponse = new WorksheetEvaluationResponse("error", 
                            "Unexpected error: " + throwable.getMessage());
                        return ResponseEntity.internalServerError().body(errorResponse);
                    })
                    .join(); // Wait for completion since we're already in async context
                
            } catch (Exception e) {
                logger.error("Error processing worksheet evaluation request", e);
                WorksheetEvaluationResponse errorResponse = new WorksheetEvaluationResponse("error", 
                    "Failed to process request: " + e.getMessage());
                return ResponseEntity.badRequest().body(errorResponse);
            }
        });
    }
    
    /**
     * Alternative endpoint that accepts metadata as form data instead of JSON
     * This can be easier to use with some HTTP clients
     * 
     * @param worksheetFile The worksheet file (PDF, JPG, PNG)
     * @param studentName Student's name
     * @param studentId Student's ID (optional)
     * @param subject Subject of the worksheet
     * @param worksheetTitle Title of the worksheet
     * @param evaluationCriteria Evaluation criteria (strict|moderate|lenient)
     * @param additionalInstructions Additional instructions (optional)
     * @param teacherNotes Teacher notes (optional)
     * @return Worksheet evaluation response
     */
    @PostMapping("/evaluate-form")
    public CompletableFuture<ResponseEntity<WorksheetEvaluationResponse>> evaluateWorksheetForm(
            @RequestParam("worksheetFile") MultipartFile worksheetFile,
            @RequestParam("studentName") String studentName,
            @RequestParam(value = "studentId", required = false) String studentId,
            @RequestParam("subject") String subject,
            @RequestParam("worksheetTitle") String worksheetTitle,
            @RequestParam(value = "evaluationCriteria", defaultValue = "moderate") String evaluationCriteria,
            @RequestParam(value = "additionalInstructions", required = false) String additionalInstructions,
            @RequestParam(value = "teacherNotes", required = false) String teacherNotes) {
        
        logger.info("Received worksheet evaluation form request. File: {}, Student: {}, Subject: {}", 
                   worksheetFile.getOriginalFilename(), studentName, subject);
        
        // Create request object from form parameters
        WorksheetEvaluationRequest request = new WorksheetEvaluationRequest();
        request.setStudentName(studentName);
        request.setStudentId(studentId);
        request.setSubject(subject);
        request.setWorksheetTitle(worksheetTitle);
        request.setEvaluationCriteria(evaluationCriteria);
        request.setAdditionalInstructions(additionalInstructions);
        request.setTeacherNotes(teacherNotes);
        
        // Process the worksheet evaluation
        return worksheetEvaluationService.evaluateWorksheet(worksheetFile, request)
            .thenApply(response -> {
                if ("error".equals(response.getStatus())) {
                    logger.error("Worksheet evaluation failed: {}", response.getError());
                    return ResponseEntity.badRequest().body(response);
                } else {
                    logger.info("Worksheet evaluation completed successfully for student: {}", studentName);
                    return ResponseEntity.ok(response);
                }
            })
            .exceptionally(throwable -> {
                logger.error("Unexpected error during worksheet evaluation", throwable);
                WorksheetEvaluationResponse errorResponse = new WorksheetEvaluationResponse("error", 
                    "Unexpected error: " + throwable.getMessage());
                return ResponseEntity.internalServerError().body(errorResponse);
            });
    }
    
    /**
     * Health check endpoint for the worksheet evaluation API
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        logger.debug("Health check requested for worksheet evaluation service");
        Map<String, String> healthStatus = worksheetEvaluationService.getHealthStatus();
        return ResponseEntity.ok(healthStatus);
    }
    
    /**
     * Get information about supported file formats and limits
     * 
     * @return API information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getApiInfo() {
        Map<String, Object> info = Map.of(
            "service", "Worksheet Evaluation API",
            "version", "2.0.0",
            "supportedFormats", new String[]{"PDF", "JPG", "JPEG", "PNG"},
            "maxFileSize", "10MB",
            "evaluationCriteria", new String[]{"strict", "moderate", "lenient"},
            "endpoints", Map.of(
                "evaluate", "POST /api/worksheet/evaluate - Evaluate worksheet with JSON metadata",
                "evaluateForm", "POST /api/worksheet/evaluate-form - Evaluate worksheet with form data",
                "evaluateWithQuestionPaper", "POST /api/worksheet/evaluate-with-question-paper - NEW: Two-document evaluation with JSON metadata",
                "evaluateWithQuestionPaperForm", "POST /api/worksheet/evaluate-with-question-paper-form - NEW: Two-document evaluation with form data",
                "health", "GET /api/worksheet/health - Service health check",
                "info", "GET /api/worksheet/info - API information",
                "example", "GET /api/worksheet/example - Example request formats"
            ),
            "singleDocumentFields", Map.of(
                "required", new String[]{"studentName", "subject", "worksheetTitle"},
                "optional", new String[]{"studentId", "evaluationCriteria", "additionalInstructions", "teacherNotes"}
            ),
            "twoDocumentFields", Map.of(
                "required", new String[]{"studentName", "subject", "examTitle"},
                "optional", new String[]{"studentId", "evaluationCriteria", "additionalInstructions", "teacherNotes"}
            ),
            "newFeatures", Map.of(
                "twoDocumentEvaluation", "Separate question paper and answer sheet processing for more accurate evaluation",
                "structuredQuestionParsing", "AI-powered question paper analysis with detailed scoring rubrics",
                "enhancedAccuracy", "Better evaluation accuracy through structured question-answer matching"
            )
        );
        
        return ResponseEntity.ok(info);
    }
    
    /**
     * NEW: Two-document evaluation endpoint - accepts question paper and answer sheet
     * 
     * @param questionPaper The question paper document (PDF, JPG, PNG)
     * @param answerSheet The student's answer sheet document (PDF, JPG, PNG)
     * @param metadataJson JSON string containing evaluation metadata
     * @return Worksheet evaluation response
     */
    @PostMapping("/evaluate-with-question-paper")
    public CompletableFuture<ResponseEntity<WorksheetEvaluationResponse>> evaluateWithQuestionPaper(
            @RequestParam("questionPaper") MultipartFile questionPaper,
            @RequestParam("answerSheet") MultipartFile answerSheet,
            @RequestParam("metadata") String metadataJson) {
        
        logger.info("Received two-document evaluation request. Question paper: {}, Answer sheet: {}", 
                   questionPaper.getOriginalFilename(), answerSheet.getOriginalFilename());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Parse metadata JSON
                QuestionPaperEvaluationRequest request = objectMapper.readValue(metadataJson, QuestionPaperEvaluationRequest.class);
                
                // Validate required fields
                if (request.getStudentName() == null || request.getStudentName().trim().isEmpty()) {
                    WorksheetEvaluationResponse errorResponse = new WorksheetEvaluationResponse("error", "Student name is required");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                
                if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
                    WorksheetEvaluationResponse errorResponse = new WorksheetEvaluationResponse("error", "Subject is required");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                
                if (request.getExamTitle() == null || request.getExamTitle().trim().isEmpty()) {
                    WorksheetEvaluationResponse errorResponse = new WorksheetEvaluationResponse("error", "Exam title is required");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                
                logger.info("Processing two-document evaluation for student: {}, subject: {}, exam: {}", 
                           request.getStudentName(), request.getSubject(), request.getExamTitle());
                
                // Step 1: Analyze question paper
                return worksheetEvaluationService.analyzeQuestionPaper(questionPaper, request.getSubject())
                    .thenCompose(questionAnalysis -> {
                        if ("error".equals(questionAnalysis.getStatus())) {
                            logger.error("Question paper analysis failed: {}", questionAnalysis.getError());
                            WorksheetEvaluationResponse errorResponse = new WorksheetEvaluationResponse("error", 
                                "Failed to analyze question paper: " + questionAnalysis.getError());
                            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(errorResponse));
                        }
                        
                        logger.info("Question paper analysis completed successfully. Total questions: {}, Total marks: {}", 
                                   questionAnalysis.getTotalQuestions(), questionAnalysis.getTotalMarks());
                        
                        // Step 2: Evaluate answer sheet against parsed questions
                        return worksheetEvaluationService.evaluateAnswerSheetAgainstQuestions(answerSheet, questionAnalysis, request)
                            .thenApply(response -> {
                                if ("error".equals(response.getStatus())) {
                                    logger.error("Answer sheet evaluation failed: {}", response.getError());
                                    return ResponseEntity.badRequest().body(response);
                                } else {
                                    logger.info("Two-document evaluation completed successfully for student: {}", 
                                               request.getStudentName());
                                    return ResponseEntity.ok(response);
                                }
                            });
                    })
                    .exceptionally(throwable -> {
                        logger.error("Unexpected error during two-document evaluation", throwable);
                        WorksheetEvaluationResponse errorResponse = new WorksheetEvaluationResponse("error", 
                            "Unexpected error: " + throwable.getMessage());
                        return ResponseEntity.internalServerError().body(errorResponse);
                    })
                    .join(); // Wait for completion since we're already in async context
                
            } catch (Exception e) {
                logger.error("Error processing two-document evaluation request", e);
                WorksheetEvaluationResponse errorResponse = new WorksheetEvaluationResponse("error", 
                    "Failed to process request: " + e.getMessage());
                return ResponseEntity.badRequest().body(errorResponse);
            }
        });
    }
    
    /**
     * NEW: Alternative two-document endpoint that accepts metadata as form data
     * 
     * @param questionPaper The question paper document (PDF, JPG, PNG)
     * @param answerSheet The student's answer sheet document (PDF, JPG, PNG)
     * @param studentName Student's name
     * @param studentId Student's ID (optional)
     * @param subject Subject of the exam
     * @param examTitle Title of the exam
     * @param evaluationCriteria Evaluation criteria (strict|moderate|lenient)
     * @param additionalInstructions Additional instructions (optional)
     * @param teacherNotes Teacher notes (optional)
     * @return Worksheet evaluation response
     */
    @PostMapping("/evaluate-with-question-paper-form")
    public CompletableFuture<ResponseEntity<WorksheetEvaluationResponse>> evaluateWithQuestionPaperForm(
            @RequestParam("questionPaper") MultipartFile questionPaper,
            @RequestParam("answerSheet") MultipartFile answerSheet,
            @RequestParam("studentName") String studentName,
            @RequestParam(value = "studentId", required = false) String studentId,
            @RequestParam("subject") String subject,
            @RequestParam("examTitle") String examTitle,
            @RequestParam(value = "evaluationCriteria", defaultValue = "moderate") String evaluationCriteria,
            @RequestParam(value = "additionalInstructions", required = false) String additionalInstructions,
            @RequestParam(value = "teacherNotes", required = false) String teacherNotes) {
        
        logger.info("Received two-document evaluation form request. Question paper: {}, Answer sheet: {}, Student: {}", 
                   questionPaper.getOriginalFilename(), answerSheet.getOriginalFilename(), studentName);
        
        // Create request object from form parameters
        QuestionPaperEvaluationRequest request = new QuestionPaperEvaluationRequest();
        request.setStudentName(studentName);
        request.setStudentId(studentId);
        request.setSubject(subject);
        request.setExamTitle(examTitle);
        request.setEvaluationCriteria(evaluationCriteria);
        request.setAdditionalInstructions(additionalInstructions);
        request.setTeacherNotes(teacherNotes);
        
        // Step 1: Analyze question paper
        return worksheetEvaluationService.analyzeQuestionPaper(questionPaper, subject)
            .thenCompose(questionAnalysis -> {
                if ("error".equals(questionAnalysis.getStatus())) {
                    logger.error("Question paper analysis failed: {}", questionAnalysis.getError());
                    WorksheetEvaluationResponse errorResponse = new WorksheetEvaluationResponse("error", 
                        "Failed to analyze question paper: " + questionAnalysis.getError());
                    return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(errorResponse));
                }
                
                logger.info("Question paper analysis completed successfully. Total questions: {}, Total marks: {}", 
                           questionAnalysis.getTotalQuestions(), questionAnalysis.getTotalMarks());
                
                // Step 2: Evaluate answer sheet against parsed questions
                return worksheetEvaluationService.evaluateAnswerSheetAgainstQuestions(answerSheet, questionAnalysis, request)
                    .thenApply(response -> {
                        if ("error".equals(response.getStatus())) {
                            logger.error("Answer sheet evaluation failed: {}", response.getError());
                            return ResponseEntity.badRequest().body(response);
                        } else {
                            logger.info("Two-document evaluation completed successfully for student: {}", studentName);
                            return ResponseEntity.ok(response);
                        }
                    });
            })
            .exceptionally(throwable -> {
                logger.error("Unexpected error during two-document evaluation", throwable);
                WorksheetEvaluationResponse errorResponse = new WorksheetEvaluationResponse("error", 
                    "Unexpected error: " + throwable.getMessage());
                return ResponseEntity.internalServerError().body(errorResponse);
            });
    }
    
    /**
     * Get example request format for the API
     * 
     * @return Example request format
     */
    @GetMapping("/example")
    public ResponseEntity<Map<String, Object>> getExampleRequest() {
        Map<String, Object> example = Map.of(
            "description", "Example requests for worksheet evaluation APIs",
            "singleDocumentEndpoint", Map.of(
                "endpoint", "POST /api/worksheet/evaluate",
                "contentType", "multipart/form-data",
                "parameters", Map.of(
                    "worksheetFile", "The worksheet file (PDF, JPG, PNG) - Required",
                    "metadata", "JSON string with evaluation metadata - Required"
                ),
                "metadataExample", Map.of(
                    "studentName", "John Doe",
                    "studentId", "STU001",
                    "subject", "Mathematics",
                    "worksheetTitle", "Algebra Practice Sheet",
                    "evaluationCriteria", "moderate",
                    "additionalInstructions", "Consider partial marks for methodology",
                    "teacherNotes", "Focus on problem-solving approach"
                )
            ),
            "twoDocumentEndpoint", Map.of(
                "endpoint", "POST /api/worksheet/evaluate-with-question-paper",
                "description", "NEW: Two-document evaluation with separate question paper and answer sheet",
                "contentType", "multipart/form-data",
                "parameters", Map.of(
                    "questionPaper", "The question paper document (PDF, JPG, PNG) - Required",
                    "answerSheet", "The student's answer sheet document (PDF, JPG, PNG) - Required",
                    "metadata", "JSON string with evaluation metadata - Required"
                ),
                "metadataExample", Map.of(
                    "studentName", "Jane Smith",
                    "studentId", "STU002",
                    "subject", "Physics",
                    "examTitle", "Mechanics Test",
                    "evaluationCriteria", "moderate",
                    "additionalInstructions", "Focus on conceptual understanding",
                    "teacherNotes", "Student has been struggling with force diagrams"
                )
            ),
            "alternativeEndpoints", Map.of(
                "singleDocumentForm", "POST /api/worksheet/evaluate-form - Single document with form parameters",
                "twoDocumentForm", "POST /api/worksheet/evaluate-with-question-paper-form - Two documents with form parameters"
            )
        );
        
        return ResponseEntity.ok(example);
    }
}
