package com.sahayak.controller;

import com.sahayak.model.ExamCreationRequest;
import com.sahayak.model.ExamCreationResponse;
import com.sahayak.service.ExamCreationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exam")
@CrossOrigin(origins = "*")
public class ExamController {

    private static final Logger logger = LoggerFactory.getLogger(ExamController.class);
    
    private final ExamCreationService examCreationService;
    
    public ExamController(ExamCreationService examCreationService) {
        this.examCreationService = examCreationService;
    }
    
    /**
     * Endpoint to create an exam based on the provided parameters
     * 
     * @param request The exam creation request
     * @return The exam creation response
     */
    @PostMapping("/create")
    public ResponseEntity<ExamCreationResponse> createExam(@RequestBody ExamCreationRequest request) {
        logger.info("Received request to create exam: {}", request);
        
        try {
            ExamCreationResponse response = examCreationService.createExam(request);
            
            if ("error".equals(response.getStatus())) {
                logger.error("Error creating exam: {}", response.getError());
                return ResponseEntity.badRequest().body(response);
            }
            
            logger.info("Exam created successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error creating exam", e);
            ExamCreationResponse errorResponse = new ExamCreationResponse("error", "Unexpected error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint for the exam API
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<ExamCreationResponse> health() {
        ExamCreationResponse response = new ExamCreationResponse();
        response.setStatus("UP");
        response.setMessage("Exam API is running");
        return ResponseEntity.ok(response);
    }
}
