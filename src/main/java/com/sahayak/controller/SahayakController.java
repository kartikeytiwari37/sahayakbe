package com.sahayak.controller;

import com.sahayak.service.SahayakTeacherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/sahayak")
@CrossOrigin(origins = "*")
public class SahayakController {
    
    private static final Logger logger = LoggerFactory.getLogger(SahayakController.class);
    
    private final SahayakTeacherService teacherService;
    
    public SahayakController(SahayakTeacherService teacherService) {
        this.teacherService = teacherService;
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Sahayak AI Teacher");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/teacher/session")
    public CompletableFuture<ResponseEntity<Map<String, String>>> createTeacherSession() {
        logger.info("Creating new teacher session via REST API");
        
        return teacherService.createTeacherSession()
            .thenApply(sessionId -> {
                Map<String, String> response = new HashMap<>();
                response.put("sessionId", sessionId);
                response.put("status", "created");
                response.put("message", "Teacher session created successfully");
                return ResponseEntity.ok(response);
            })
            .exceptionally(throwable -> {
                logger.error("Failed to create teacher session", throwable);
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Failed to create teacher session: " + throwable.getMessage());
                return ResponseEntity.internalServerError().body(response);
            });
    }
    
    @PostMapping("/teacher/session/custom")
    public CompletableFuture<ResponseEntity<Map<String, String>>> createCustomTeacherSession(
            @RequestBody Map<String, String> request) {
        
        String customPrompt = request.get("customPrompt");
        logger.info("Creating new custom teacher session with prompt: {}", 
                   customPrompt != null ? customPrompt.substring(0, Math.min(100, customPrompt.length())) + "..." : "null");
        
        return teacherService.createTeacherSessionWithCustomPrompt(customPrompt)
            .thenApply(sessionId -> {
                Map<String, String> response = new HashMap<>();
                response.put("sessionId", sessionId);
                response.put("status", "created");
                response.put("message", "Custom teacher session created successfully");
                return ResponseEntity.ok(response);
            })
            .exceptionally(throwable -> {
                logger.error("Failed to create custom teacher session", throwable);
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Failed to create custom teacher session: " + throwable.getMessage());
                return ResponseEntity.internalServerError().body(response);
            });
    }
    
    @PostMapping("/teacher/prompt-creator")
    public CompletableFuture<ResponseEntity<Map<String, String>>> createPromptCreatorSession() {
        logger.info("Creating new prompt creator session via REST API");
        
        return teacherService.createPromptCreatorSession()
            .thenApply(sessionId -> {
                Map<String, String> response = new HashMap<>();
                response.put("sessionId", sessionId);
                response.put("status", "created");
                response.put("message", "Prompt creator session created successfully");
                return ResponseEntity.ok(response);
            })
            .exceptionally(throwable -> {
                logger.error("Failed to create prompt creator session", throwable);
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Failed to create prompt creator session: " + throwable.getMessage());
                return ResponseEntity.internalServerError().body(response);
            });
    }
    
    @GetMapping("/teacher/session/{sessionId}/status")
    public ResponseEntity<Map<String, Object>> getSessionStatus(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        boolean isActive = teacherService.isSessionActive(sessionId);
        
        response.put("sessionId", sessionId);
        response.put("active", isActive);
        response.put("status", isActive ? "ACTIVE" : "INACTIVE");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/teacher/session/{sessionId}")
    public ResponseEntity<Map<String, String>> closeTeacherSession(@PathVariable String sessionId) {
        logger.info("Closing teacher session: {}", sessionId);
        
        try {
            teacherService.closeTeacherSession(sessionId);
            Map<String, String> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("status", "closed");
            response.put("message", "Teacher session closed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to close teacher session: {}", sessionId, e);
            Map<String, String> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("status", "error");
            response.put("message", "Failed to close session: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @GetMapping("/teacher/sessions")
    public ResponseEntity<Map<String, Object>> getAllSessions() {
        Map<String, String> sessionsStatus = teacherService.getActiveSessionsStatus();
        Map<String, Object> response = new HashMap<>();
        response.put("sessions", sessionsStatus);
        response.put("totalSessions", sessionsStatus.size());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/teacher/session/{sessionId}/text")
    public ResponseEntity<Map<String, String>> sendTextMessage(
            @PathVariable String sessionId, 
            @RequestBody Map<String, String> request) {
        
        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Text message cannot be empty");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            teacherService.sendTextToTeacher(sessionId, text);
            Map<String, String> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("status", "sent");
            response.put("message", "Text message sent successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to send text message to session: {}", sessionId, e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to send message: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
