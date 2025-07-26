package com.sahayak.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahayak.model.WorksheetEvaluationRequest;
import com.sahayak.model.WorksheetEvaluationResponse;
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
            "version", "1.0.0",
            "supportedFormats", new String[]{"PDF", "JPG", "JPEG", "PNG"},
            "maxFileSize", "10MB",
            "evaluationCriteria", new String[]{"strict", "moderate", "lenient"},
            "endpoints", Map.of(
                "evaluate", "POST /api/worksheet/evaluate - Evaluate worksheet with JSON metadata",
                "evaluateForm", "POST /api/worksheet/evaluate-form - Evaluate worksheet with form data",
                "health", "GET /api/worksheet/health - Service health check",
                "info", "GET /api/worksheet/info - API information"
            ),
            "requiredFields", new String[]{"studentName", "subject", "worksheetTitle"},
            "optionalFields", new String[]{"studentId", "evaluationCriteria", "additionalInstructions", "teacherNotes"}
        );
        
        return ResponseEntity.ok(info);
    }
    
    /**
     * Get example request format for the API
     * 
     * @return Example request format
     */
    @GetMapping("/example")
    public ResponseEntity<Map<String, Object>> getExampleRequest() {
        Map<String, Object> example = Map.of(
            "description", "Example request for worksheet evaluation",
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
            ),
            "alternativeEndpoint", Map.of(
                "endpoint", "POST /api/worksheet/evaluate-form",
                "description", "Alternative endpoint using form parameters instead of JSON metadata",
                "parameters", Map.of(
                    "worksheetFile", "The worksheet file - Required",
                    "studentName", "Student's name - Required",
                    "subject", "Subject - Required", 
                    "worksheetTitle", "Worksheet title - Required",
                    "studentId", "Student ID - Optional",
                    "evaluationCriteria", "strict|moderate|lenient - Optional (default: moderate)",
                    "additionalInstructions", "Additional instructions - Optional",
                    "teacherNotes", "Teacher notes - Optional"
                )
            )
        );
        
        return ResponseEntity.ok(example);
    }
}
