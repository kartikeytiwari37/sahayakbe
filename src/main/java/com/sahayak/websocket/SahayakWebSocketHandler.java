package com.sahayak.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahayak.service.SahayakTeacherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class SahayakWebSocketHandler implements WebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(SahayakWebSocketHandler.class);
    
    private final SahayakTeacherService teacherService;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, String> sessionToTeacherMapping = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, WebSocketSession> webSocketSessions = new ConcurrentHashMap<>();
    
    public SahayakWebSocketHandler(SahayakTeacherService teacherService, ObjectMapper objectMapper) {
        this.teacherService = teacherService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket connection established: {}", session.getId());
        webSocketSessions.put(session.getId(), session);
        
        // Don't create session immediately - wait for init message to determine session type
        sendToClient(session, createMessage("connection", "success", "WebSocket connected - waiting for initialization"));
    }
    
    private void setupTeacherHandlers(String webSocketSessionId, String teacherSessionId) {
        // Handle audio responses from teacher
        teacherService.setAudioHandler(teacherSessionId, audioData -> {
            WebSocketSession session = webSocketSessions.get(webSocketSessionId);
            if (session != null && session.isOpen()) {
                sendToClient(session, createMessage("audio", "data", audioData));
            }
        });
        
        // Handle text responses from teacher
        teacherService.setContentHandler(teacherSessionId, content -> {
            WebSocketSession session = webSocketSessions.get(webSocketSessionId);
            if (session != null && session.isOpen()) {
                sendToClient(session, createMessage("content", "text", content));
            }
        });
        
        // Handle errors from teacher
        teacherService.setErrorHandler(teacherSessionId, error -> {
            WebSocketSession session = webSocketSessions.get(webSocketSessionId);
            if (session != null && session.isOpen()) {
                sendToClient(session, createMessage("error", "teacher", error));
            }
        });
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            handleTextMessage(session, (TextMessage) message);
        } else if (message instanceof BinaryMessage) {
            handleBinaryMessage(session, (BinaryMessage) message);
        }
    }
    
    private void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            logger.debug("Received text message from {}: {}", session.getId(), payload);
            
            JsonNode jsonNode = objectMapper.readTree(payload);
            String type = jsonNode.get("type").asText();
            // Handle initialization message
            if ("init".equals(type)) {
                handleInitMessage(session, jsonNode);
                return;
            }
            
            String teacherSessionId = sessionToTeacherMapping.get(session.getId());
            
            if (teacherSessionId == null) {
                logger.warn("No teacher session found for WebSocket session: {}", session.getId());
                sendToClient(session, createMessage("error", "session", "No teacher session available"));
                return;
            }
            
            switch (type) {
                case "audio":
                    String audioData = jsonNode.get("data").asText();
                    teacherService.sendAudioToTeacher(teacherSessionId, audioData);
                    break;
                    
                case "video":
                    String videoData = jsonNode.get("data").asText();
                    logger.info("Received video message from {}, data size: {}", session.getId(), videoData.length());
                    teacherService.sendVideoToTeacher(teacherSessionId, videoData);
                    logger.info("Video data forwarded to teacher session: {}", teacherSessionId);
                    break;
                    
                case "text":
                    String text = jsonNode.get("data").asText();
                    teacherService.sendTextToTeacher(teacherSessionId, text);
                    break;
                    
                default:
                    logger.warn("Unknown message type: {}", type);
                    sendToClient(session, createMessage("error", "unknown", "Unknown message type: " + type));
            }
            
        } catch (Exception e) {
            logger.error("Error handling text message from session {}", session.getId(), e);
            sendToClient(session, createMessage("error", "processing", "Error processing message: " + e.getMessage()));
        }
    }
    
    private void handleInitMessage(WebSocketSession session, JsonNode jsonNode) {
        try {
            String mode = jsonNode.has("mode") ? jsonNode.get("mode").asText() : "teacher";
            logger.info("Initializing session {} with mode: {}", session.getId(), mode);
            
            if ("prompt-creator".equals(mode)) {
                // Create prompt creator session
                teacherService.createPromptCreatorSession().thenAccept(teacherSessionId -> {
                    sessionToTeacherMapping.put(session.getId(), teacherSessionId);
                    logger.info("Mapped WebSocket session {} to prompt creator session {}", session.getId(), teacherSessionId);
                    
                    // Set up handlers for prompt creator responses
                    setupTeacherHandlers(session.getId(), teacherSessionId);
                    
                    // Send connection success message to client
                    sendToClient(session, createMessage("connection", "success", "Connected to Kalam Sir - Prompt Creator"));
                }).exceptionally(throwable -> {
                    logger.error("Failed to create prompt creator session for WebSocket {}", session.getId(), throwable);
                    sendToClient(session, createMessage("connection", "error", "Failed to connect to Kalam Sir"));
                    return null;
                });
            } else {
                // Create regular teacher session with optional custom prompt
                String customPrompt = jsonNode.has("customPrompt") && !jsonNode.get("customPrompt").isNull() 
                    ? jsonNode.get("customPrompt").asText() 
                    : null;
                
                logger.info("Creating teacher session with custom prompt: {}", customPrompt != null ? "Yes" : "No");
                
                teacherService.createTeacherSessionWithCustomPrompt(customPrompt).thenAccept(teacherSessionId -> {
                    sessionToTeacherMapping.put(session.getId(), teacherSessionId);
                    logger.info("Mapped WebSocket session {} to teacher session {} with custom prompt", session.getId(), teacherSessionId);
                    
                    // Set up handlers for teacher responses
                    setupTeacherHandlers(session.getId(), teacherSessionId);
                    
                    // Send connection success message to client
                    sendToClient(session, createMessage("connection", "success", "Connected to AI Teacher"));
                }).exceptionally(throwable -> {
                    logger.error("Failed to create teacher session for WebSocket {}", session.getId(), throwable);
                    sendToClient(session, createMessage("connection", "error", "Failed to connect to AI Teacher"));
                    return null;
                });
            }
        } catch (Exception e) {
            logger.error("Error handling init message from session {}", session.getId(), e);
            sendToClient(session, createMessage("error", "init", "Error during initialization: " + e.getMessage()));
        }
    }
    
    private void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        logger.debug("Received binary message from {}, size: {}", session.getId(), message.getPayloadLength());
        // Handle binary data if needed (e.g., raw audio/video data)
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("Transport error for session {}", session.getId(), exception);
        cleanupSession(session.getId());
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        logger.info("WebSocket connection closed: {} - {}", session.getId(), closeStatus);
        cleanupSession(session.getId());
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    private void cleanupSession(String webSocketSessionId) {
        String teacherSessionId = sessionToTeacherMapping.remove(webSocketSessionId);
        webSocketSessions.remove(webSocketSessionId);
        
        if (teacherSessionId != null) {
            logger.info("Closing teacher session: {}", teacherSessionId);
            teacherService.closeTeacherSession(teacherSessionId);
        }
    }
    
    private void sendToClient(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                // Split large messages into smaller chunks if needed
                if (message.length() > 64000) { // 64KB limit
                    logger.warn("Message too large ({}), truncating", message.length());
                    message = message.substring(0, 64000) + "...";
                }
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            logger.error("Error sending message to client {}", session.getId(), e);
        }
    }
    
    private String createMessage(String type, String subType, String data) {
        try {
            return objectMapper.writeValueAsString(new MessageResponse(type, subType, data));
        } catch (Exception e) {
            logger.error("Error creating message", e);
            return "{\"type\":\"error\",\"subType\":\"internal\",\"data\":\"Internal error\"}";
        }
    }
    
    public static class MessageResponse {
        private String type;
        private String subType;
        private String data;
        private long timestamp;
        
        public MessageResponse(String type, String subType, String data) {
            this.type = type;
            this.subType = subType;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getSubType() { return subType; }
        public void setSubType(String subType) { this.subType = subType; }
        
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
