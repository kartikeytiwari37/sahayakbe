package com.sahayak.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahayak.model.LiveConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SahayakTeacherService {
    
    private static final Logger logger = LoggerFactory.getLogger(SahayakTeacherService.class);
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;
    
    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    
    @Value("${gemini.api.model}")
    private String geminiModel;
    
    @Value("${sahayak.teacher.system-instruction}")
    private String systemInstruction;
    
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, GeminiLiveWebSocketClient> activeSessions = new ConcurrentHashMap<>();
    
    public SahayakTeacherService(ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher) {
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }
    
    public CompletableFuture<String> createTeacherSession() {
        String sessionId = UUID.randomUUID().toString();
        logger.info("Creating new teacher session: {}", sessionId);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                GeminiLiveWebSocketClient client = new GeminiLiveWebSocketClient(
                    geminiApiUrl, geminiApiKey, objectMapper, eventPublisher
                );
                
                // Connect to Gemini Live API and wait for connection
                client.connectAsync().get();
                
                // Add a small delay to ensure connection is fully established
                Thread.sleep(1000);
                
                // Setup the AI teacher configuration
                LiveConfig config = createTeacherConfig();
                client.sendSetupMessage(config);
                
                activeSessions.put(sessionId, client);
                logger.info("Teacher session created successfully: {}", sessionId);
                
                return sessionId;
            } catch (Exception e) {
                logger.error("Failed to create teacher session", e);
                throw new RuntimeException("Failed to create teacher session", e);
            }
        });
    }
    
    private LiveConfig createTeacherConfig() {
        LiveConfig config = new LiveConfig(geminiModel);
        
        // MINIMAL CONFIG - Test if basic setup works first
        LiveConfig.GenerationConfig genConfig = new LiveConfig.GenerationConfig();
        genConfig.setResponseModalities("audio");
        
        // Configure voice settings
        LiveConfig.PrebuiltVoiceConfig voiceConfig = new LiveConfig.PrebuiltVoiceConfig("Aoede");
        LiveConfig.VoiceConfig voice = new LiveConfig.VoiceConfig();
        voice.setPrebuiltVoiceConfig(voiceConfig);
        LiveConfig.SpeechConfig speechConfig = new LiveConfig.SpeechConfig();
        speechConfig.setVoiceConfig(voice);
        genConfig.setSpeechConfig(speechConfig);
        
        config.setGenerationConfig(genConfig);
        
        // NO TOOLS OR SYSTEM INSTRUCTION FOR NOW - test minimal config
        
        return config;
    }
    
    public void sendAudioToTeacher(String sessionId, String base64AudioData) {
        GeminiLiveWebSocketClient client = activeSessions.get(sessionId);
        if (client != null && client.isOpen()) {
            logger.debug("Sending audio data to teacher session: {}", sessionId);
            client.sendAudioData(base64AudioData);
        } else {
            logger.warn("Teacher session not found or closed: {}", sessionId);
            throw new RuntimeException("Teacher session not available: " + sessionId);
        }
    }
    
    public void sendVideoToTeacher(String sessionId, String base64VideoData) {
        GeminiLiveWebSocketClient client = activeSessions.get(sessionId);
        if (client != null && client.isOpen()) {
            logger.debug("Sending video data to teacher session: {}", sessionId);
            client.sendVideoData(base64VideoData);
        } else {
            logger.warn("Teacher session not found or closed: {}", sessionId);
            throw new RuntimeException("Teacher session not available: " + sessionId);
        }
    }
    
    public void sendTextToTeacher(String sessionId, String text) {
        GeminiLiveWebSocketClient client = activeSessions.get(sessionId);
        if (client != null && client.isOpen()) {
            logger.info("Sending text to teacher session {}: {}", sessionId, text);
            client.sendTextMessage(text);
        } else {
            logger.warn("Teacher session not found or closed: {}", sessionId);
            throw new RuntimeException("Teacher session not available: " + sessionId);
        }
    }
    
    public void setAudioHandler(String sessionId, java.util.function.Consumer<String> audioHandler) {
        GeminiLiveWebSocketClient client = activeSessions.get(sessionId);
        if (client != null) {
            client.setAudioDataHandler(audioHandler);
        }
    }
    
    public void setContentHandler(String sessionId, java.util.function.Consumer<String> contentHandler) {
        GeminiLiveWebSocketClient client = activeSessions.get(sessionId);
        if (client != null) {
            client.setContentHandler(contentHandler);
        }
    }
    
    public void setErrorHandler(String sessionId, java.util.function.Consumer<String> errorHandler) {
        GeminiLiveWebSocketClient client = activeSessions.get(sessionId);
        if (client != null) {
            client.setErrorHandler(errorHandler);
        }
    }
    
    public void closeTeacherSession(String sessionId) {
        GeminiLiveWebSocketClient client = activeSessions.remove(sessionId);
        if (client != null) {
            logger.info("Closing teacher session: {}", sessionId);
            client.close();
        }
    }
    
    public boolean isSessionActive(String sessionId) {
        GeminiLiveWebSocketClient client = activeSessions.get(sessionId);
        return client != null && client.isOpen();
    }
    
    public Map<String, String> getActiveSessionsStatus() {
        Map<String, String> status = new HashMap<>();
        activeSessions.forEach((sessionId, client) -> {
            status.put(sessionId, client.isOpen() ? "ACTIVE" : "CLOSED");
        });
        return status;
    }
}
