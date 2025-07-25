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
    
    // Dual connection approach: separate connections for text and audio
    private final Map<String, GeminiLiveWebSocketClient> textSessions = new ConcurrentHashMap<>();
    private final Map<String, GeminiLiveWebSocketClient> audioSessions = new ConcurrentHashMap<>();
    
    // Track screen sharing state per session
    private final Map<String, Boolean> screenSharingStates = new ConcurrentHashMap<>();
    
    public SahayakTeacherService(ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher) {
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }
    
    public CompletableFuture<String> createTeacherSession() {
        return createTeacherSessionWithCustomPrompt(null);
    }
    
    public CompletableFuture<String> createTeacherSessionWithCustomPrompt(String customPrompt) {
        String sessionId = UUID.randomUUID().toString();
        logger.info("Creating dual teacher sessions (text + audio) with custom prompt: {}", sessionId);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create TEXT connection
                GeminiLiveWebSocketClient textClient = new GeminiLiveWebSocketClient(
                    geminiApiUrl, geminiApiKey, objectMapper, eventPublisher
                );
                textClient.connectAsync().get();
                Thread.sleep(500);
                
                LiveConfig textConfig = createTeacherConfigWithModality("text", customPrompt);
                textClient.sendSetupMessage(textConfig);
                textSessions.put(sessionId, textClient);
                logger.info("Text session created for: {}", sessionId);
                
                // Create AUDIO connection
                GeminiLiveWebSocketClient audioClient = new GeminiLiveWebSocketClient(
                    geminiApiUrl, geminiApiKey, objectMapper, eventPublisher
                );
                audioClient.connectAsync().get();
                Thread.sleep(500);
                
                LiveConfig audioConfig = createTeacherConfigWithModality("audio", customPrompt);
                audioClient.sendSetupMessage(audioConfig);
                audioSessions.put(sessionId, audioClient);
                logger.info("Audio session created for: {}", sessionId);
                
                logger.info("Dual teacher sessions created successfully: {}", sessionId);
                return sessionId;
            } catch (Exception e) {
                logger.error("Failed to create teacher sessions", e);
                throw new RuntimeException("Failed to create teacher sessions", e);
            }
        });
    }
    
    public CompletableFuture<String> createPromptCreatorSession() {
        String sessionId = UUID.randomUUID().toString();
        logger.info("Creating prompt creator session: {}", sessionId);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create TEXT connection for prompt creation
                GeminiLiveWebSocketClient textClient = new GeminiLiveWebSocketClient(
                    geminiApiUrl, geminiApiKey, objectMapper, eventPublisher
                );
                textClient.connectAsync().get();
                Thread.sleep(500);
                
                LiveConfig promptConfig = createPromptCreatorConfig();
                textClient.sendSetupMessage(promptConfig);
                textSessions.put(sessionId, textClient);
                logger.info("Prompt creator session created for: {}", sessionId);
                
                return sessionId;
            } catch (Exception e) {
                logger.error("Failed to create prompt creator session", e);
                throw new RuntimeException("Failed to create prompt creator session", e);
            }
        });
    }
    
    private LiveConfig createTeacherConfigWithModality(String modality, String customPrompt) {
        LiveConfig config = new LiveConfig(geminiModel);
        
        // Use custom prompt if provided, otherwise use default system instruction
        String instructionText = customPrompt != null && !customPrompt.trim().isEmpty() 
            ? customPrompt 
            : systemInstruction;
        
        // Add system instruction for AI teacher behavior
        if (instructionText != null && !instructionText.trim().isEmpty()) {
            LiveConfig.Part instructionPart = new LiveConfig.Part(instructionText);
            LiveConfig.SystemInstruction sysInstruction = new LiveConfig.SystemInstruction(Arrays.asList(instructionPart));
            config.setSystemInstruction(sysInstruction);
            logger.info("Added {} instruction to {} config: {}", 
                       customPrompt != null ? "custom" : "default", 
                       modality, 
                       instructionText.substring(0, Math.min(100, instructionText.length())) + "...");
        } else {
            logger.warn("No system instruction found for {} config!", modality);
        }
        
        // Configure generation settings with specified modality
        LiveConfig.GenerationConfig genConfig = new LiveConfig.GenerationConfig();
        genConfig.setResponseModalities(modality);
        
        // Configure voice settings optimized for Hindi accent
        LiveConfig.PrebuiltVoiceConfig voiceConfig;
        
        // Try different voices for better Hindi accent
        if ("audio".equals(modality)) {
            // For audio responses, try voices that handle Hindi better
            voiceConfig = new LiveConfig.PrebuiltVoiceConfig("Puck"); // Alternative voice
            logger.info("Using 'Puck' voice for audio session (better for Hindi accent)");
        } else {
            voiceConfig = new LiveConfig.PrebuiltVoiceConfig("Aoede"); // Keep Aoede for text
            logger.info("Using 'Aoede' voice for text session");
        }
        
        LiveConfig.VoiceConfig voice = new LiveConfig.VoiceConfig();
        voice.setPrebuiltVoiceConfig(voiceConfig);
        LiveConfig.SpeechConfig speechConfig = new LiveConfig.SpeechConfig();
        speechConfig.setVoiceConfig(voice);
        genConfig.setSpeechConfig(speechConfig);
        
        config.setGenerationConfig(genConfig);
        
        logger.debug("Created {} config with responseModalities: {}", modality, modality);
        return config;
    }
    
    private LiveConfig createPromptCreatorConfig() {
        LiveConfig config = new LiveConfig(geminiModel);
        
        // Special system instruction for prompt creation
        String promptCreatorInstruction = "You are Kalam Sir, a friendly AI prompt creator who helps teachers quickly create teaching assistants. " +
            "You are direct, efficient, and conversational. " +
            "\n\nCRITICAL RULES:" +
            "\n- NEVER repeat the same questions or ask long lists" +
            "\n- ALWAYS acknowledge what the teacher already told you" +
            "\n- If they give you subject + grade level + basic style, CREATE THE PROMPT IMMEDIATELY" +
            "\n- Only ask ONE short follow-up if absolutely necessary" +
            "\n- Be natural and conversational, not robotic" +
            "\n\nExample:" +
            "\nTeacher: 'I want a weather teaching assistant for 6th graders'" +
            "\nYou: 'Perfect! A weather assistant for 6th graders. Should it focus more on explaining weather patterns, doing fun experiments, or helping with weather prediction activities?'" +
            "\n[After their answer, create the prompt immediately]" +
            "\n\nWhen you have enough info (which is usually after 1-2 exchanges), respond with:" +
            "\n'FINAL_PROMPT: [detailed system instruction for the teaching assistant]'" +
            "\n\nBe efficient - teachers want results quickly, not long questionnaires!";
        
        LiveConfig.Part instructionPart = new LiveConfig.Part(promptCreatorInstruction);
        LiveConfig.SystemInstruction sysInstruction = new LiveConfig.SystemInstruction(Arrays.asList(instructionPart));
        config.setSystemInstruction(sysInstruction);
        
        // Configure for text-only responses
        LiveConfig.GenerationConfig genConfig = new LiveConfig.GenerationConfig();
        genConfig.setResponseModalities("text");
        config.setGenerationConfig(genConfig);
        
        logger.info("Created prompt creator config");
        return config;
    }
    
    public void sendAudioToTeacher(String sessionId, String base64AudioData) {
        GeminiLiveWebSocketClient audioClient = audioSessions.get(sessionId);
        if (audioClient != null && audioClient.isOpen()) {
            logger.debug("Sending audio data to AUDIO session: {}", sessionId);
            audioClient.sendAudioData(base64AudioData);
        } else {
            logger.warn("Audio session not found or closed: {}", sessionId);
            throw new RuntimeException("Audio session not available: " + sessionId);
        }
    }
    
    public void sendVideoToTeacher(String sessionId, String base64VideoData) {
        // Send video to AUDIO session for proper multimodal processing (like Live API console)
        GeminiLiveWebSocketClient audioClient = audioSessions.get(sessionId);
        if (audioClient != null && audioClient.isOpen()) {
            logger.debug("Sending video data to AUDIO session for multimodal processing: {}", sessionId);
            audioClient.sendVideoData(base64VideoData);
        } else {
            logger.warn("Audio session not found or closed: {}", sessionId);
            throw new RuntimeException("Audio session not available: " + sessionId);
        }
    }
    
    public void sendTextToTeacher(String sessionId, String text) {
        GeminiLiveWebSocketClient textClient = textSessions.get(sessionId);
        if (textClient != null && textClient.isOpen()) {
            logger.info("Sending text to TEXT session {}: {}", sessionId, text);
            textClient.sendTextMessage(text);
        } else {
            logger.warn("Text session not found or closed: {}", sessionId);
            throw new RuntimeException("Text session not available: " + sessionId);
        }
    }
    
    public void setAudioHandler(String sessionId, java.util.function.Consumer<String> audioHandler) {
        GeminiLiveWebSocketClient audioClient = audioSessions.get(sessionId);
        if (audioClient != null) {
            audioClient.setAudioDataHandler(audioHandler);
        }
    }
    
    public void setContentHandler(String sessionId, java.util.function.Consumer<String> contentHandler) {
        // Set content handler for both sessions
        GeminiLiveWebSocketClient textClient = textSessions.get(sessionId);
        if (textClient != null) {
            textClient.setContentHandler(contentHandler);
        }
        
        GeminiLiveWebSocketClient audioClient = audioSessions.get(sessionId);
        if (audioClient != null) {
            audioClient.setContentHandler(contentHandler);
        }
    }
    
    public void setErrorHandler(String sessionId, java.util.function.Consumer<String> errorHandler) {
        // Set error handler for both sessions
        GeminiLiveWebSocketClient textClient = textSessions.get(sessionId);
        if (textClient != null) {
            textClient.setErrorHandler(errorHandler);
        }
        
        GeminiLiveWebSocketClient audioClient = audioSessions.get(sessionId);
        if (audioClient != null) {
            audioClient.setErrorHandler(errorHandler);
        }
    }
    
    public void closeTeacherSession(String sessionId) {
        logger.info("Closing dual teacher sessions: {}", sessionId);
        
        // Close text session
        GeminiLiveWebSocketClient textClient = textSessions.remove(sessionId);
        if (textClient != null) {
            textClient.close();
        }
        
        // Close audio session
        GeminiLiveWebSocketClient audioClient = audioSessions.remove(sessionId);
        if (audioClient != null) {
            audioClient.close();
        }
    }
    
    public boolean isSessionActive(String sessionId) {
        GeminiLiveWebSocketClient textClient = textSessions.get(sessionId);
        GeminiLiveWebSocketClient audioClient = audioSessions.get(sessionId);
        
        boolean textActive = textClient != null && textClient.isOpen();
        boolean audioActive = audioClient != null && audioClient.isOpen();
        
        return textActive && audioActive; // Both should be active
    }
    
    public Map<String, String> getActiveSessionsStatus() {
        Map<String, String> status = new HashMap<>();
        
        // Check all session IDs from both maps
        textSessions.keySet().forEach(sessionId -> {
            boolean textActive = textSessions.get(sessionId) != null && textSessions.get(sessionId).isOpen();
            boolean audioActive = audioSessions.get(sessionId) != null && audioSessions.get(sessionId).isOpen();
            
            String sessionStatus = String.format("TEXT:%s, AUDIO:%s", 
                textActive ? "ACTIVE" : "CLOSED", 
                audioActive ? "ACTIVE" : "CLOSED");
            status.put(sessionId, sessionStatus);
        });
        
        return status;
    }
}
