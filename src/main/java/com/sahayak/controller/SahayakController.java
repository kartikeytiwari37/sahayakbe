package com.sahayak.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahayak.service.SahayakTeacherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/sahayak")
@CrossOrigin(origins = "*")
public class SahayakController {
    
    private static final Logger logger = LoggerFactory.getLogger(SahayakController.class);
    
    private final SahayakTeacherService teacherService;
    private final ObjectMapper objectMapper;
    
    public SahayakController(SahayakTeacherService teacherService, ObjectMapper objectMapper) {
        this.teacherService = teacherService;
        this.objectMapper = objectMapper;
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
    
    @PostMapping("/video/generate-prompt")
    public CompletableFuture<ResponseEntity<Map<String, String>>> generateVideoPrompt(
            @RequestBody Map<String, Object> request) {
        
        logger.info("Generating video prompt for context");
        
        return teacherService.generateVideoPrompt(request)
            .thenApply(prompt -> {
                Map<String, String> response = new HashMap<>();
                response.put("prompt", prompt);
                response.put("status", "success");
                return ResponseEntity.ok(response);
            })
            .exceptionally(throwable -> {
                logger.error("Failed to generate video prompt", throwable);
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Failed to generate video prompt: " + throwable.getMessage());
                return ResponseEntity.internalServerError().body(response);
            });
    }
    
    @PostMapping("/video/generate")
    public CompletableFuture<ResponseEntity<Map<String, String>>> generateVideo(
            @RequestBody Map<String, String> request) {
        
        String prompt = request.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Video prompt cannot be empty");
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(response));
        }
        
        logger.info("Starting video generation with prompt: {}", 
                   prompt.substring(0, Math.min(100, prompt.length())) + "...");
        
        return teacherService.generateVideo(prompt)
            .thenApply(operationName -> {
                Map<String, String> response = new HashMap<>();
                response.put("operationName", operationName);
                response.put("status", "started");
                response.put("message", "Video generation started successfully");
                return ResponseEntity.ok(response);
            })
            .exceptionally(throwable -> {
                logger.error("Failed to start video generation", throwable);
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Failed to start video generation: " + throwable.getMessage());
                return ResponseEntity.internalServerError().body(response);
            });
    }
    
    @GetMapping("/video/status")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getVideoStatus(
            @RequestParam String operationName) {
        
        logger.info("Checking video generation status for operation: {}", operationName);
        
        // Decode the operation name if it's URL encoded
        String decodedOperationName;
        try {
            decodedOperationName = java.net.URLDecoder.decode(operationName, "UTF-8");
            logger.info("Decoded operation name: {}", decodedOperationName);
        } catch (Exception e) {
            logger.warn("Failed to decode operation name, using as-is: {}", operationName);
            decodedOperationName = operationName;
        }
        
        final String finalOperationName = decodedOperationName;
        
        return teacherService.getVideoStatus(decodedOperationName)
            .thenApply(status -> {
                Map<String, Object> response = new HashMap<>();
                response.put("operationName", operationName);
                response.put("done", status.get("done"));
                if ((Boolean) status.get("done")) {
                    response.put("videoUri", status.get("videoUri"));
                }
                response.put("status", "success");
                return ResponseEntity.ok(response);
            })
            .exceptionally(throwable -> {
                logger.error("Failed to check video status for operation: {}", operationName, throwable);
                Map<String, Object> response = new HashMap<>();
                response.put("operationName", operationName);
                response.put("status", "error");
                response.put("message", "Failed to check video status: " + throwable.getMessage());
                return ResponseEntity.internalServerError().body(response);
            });
    }
    
    @PostMapping("/video/download")
    public ResponseEntity<byte[]> downloadVideo(@RequestBody Map<String, String> request) {
        try {
            String videoUri = request.get("videoUri");
            if (videoUri == null || videoUri.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            byte[] videoData = teacherService.downloadVideo(videoUri).get();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "educational-video.mp4");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(videoData);
                
        } catch (Exception e) {
            logger.error("Error downloading video", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Future Plan Generation API
    @PostMapping("/future-plan/generate")
    public ResponseEntity<Map<String, Object>> generateFuturePlan(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            if (text == null || text.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Text parameter is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            logger.info("Generating future plan for text: {}", text);
            
            // Call the external future planner API
            Map<String, Object> futurePlanResponse = callFuturePlannerAPI(text);
            
            return ResponseEntity.ok(futurePlanResponse);
                
        } catch (Exception e) {
            logger.error("Error generating future plan", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to generate future plan: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    private Map<String, Object> callFuturePlannerAPI(String text) throws Exception {
        // Create HTTP client with SSL handling
        HttpClient client = createPermissiveHttpClient();
        
        // Create request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("text", text);
        String requestJson = objectMapper.writeValueAsString(requestBody);
        
        // Make HTTP request to future planner API
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://future-planner-api-1026861423924.us-central1.run.app/generate-plan-from-text"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestJson))
            .timeout(Duration.ofMinutes(2)) // 2 minute timeout for this API
            .build();
        
        logger.info("Calling future planner API with text: {}", text);
        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Future planner API failed. Status: " + response.statusCode() + ", Body: " + response.body());
        }
        
        // Parse and return response
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
        logger.info("Future plan generated successfully for: {}", text);
        
        return responseMap;
    }
    
    private HttpClient createPermissiveHttpClient() {
        try {
            // Create a trust manager that accepts all certificates (for development only)
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] {
                new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
            };
            
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .sslContext(sslContext)
                .build();
        } catch (Exception e) {
            logger.warn("Failed to create permissive HTTP client, using default: {}", e.getMessage());
            return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        }
    }
}
