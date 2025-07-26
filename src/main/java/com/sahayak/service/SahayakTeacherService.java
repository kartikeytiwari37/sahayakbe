package com.sahayak.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahayak.model.LiveConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
    
    public CompletableFuture<String> createUdaanPromptCreatorSession() {
        String sessionId = UUID.randomUUID().toString();
        logger.info("Creating Udaan prompt creator session: {}", sessionId);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create TEXT connection for Udaan prompt creation
                GeminiLiveWebSocketClient textClient = new GeminiLiveWebSocketClient(
                    geminiApiUrl, geminiApiKey, objectMapper, eventPublisher
                );
                textClient.connectAsync().get();
                Thread.sleep(500);
                
                LiveConfig udaanConfig = createUdaanPromptCreatorConfig();
                textClient.sendSetupMessage(udaanConfig);
                textSessions.put(sessionId, textClient);
                logger.info("Udaan prompt creator session created for: {}", sessionId);
                
                return sessionId;
            } catch (Exception e) {
                logger.error("Failed to create Udaan prompt creator session", e);
                throw new RuntimeException("Failed to create Udaan prompt creator session", e);
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
    
    private LiveConfig createUdaanPromptCreatorConfig() {
        LiveConfig config = new LiveConfig(geminiModel);
        
        // Special system instruction for Udaan future planning prompt creation
        String udaanPromptCreatorInstruction = "You are Udaan, an inspiring AI future planner who helps create motivational career roadmaps for students. " +
            "You are enthusiastic, encouraging, and focused on gathering key student information efficiently. " +
            "\n\nCRITICAL RULES:" +
            "\n- ALWAYS be encouraging and positive about the student's dreams" +
            "\n- Gather these key details: student name, age, current grade/standard, location, and career goal" +
            "\n- If they provide basic info, ask for missing details in ONE friendly question" +
            "\n- Once you have name, age, location, and career goal, CREATE THE PROMPT IMMEDIATELY" +
            "\n- Be natural, motivational, and conversational" +
            "\n\nExample:" +
            "\nUser: 'Priya is 15 years old from Mumbai and wants to become a doctor'" +
            "\nYou: 'Amazing! Priya has such an inspiring goal! What grade/standard is she currently in? This will help me create the perfect roadmap for her medical journey!'" +
            "\n[After their answer, create the prompt immediately]" +
            "\n\nWhen you have enough info (name, age, location, career goal), respond with:" +
            "\n'FINAL_PROMPT: [Student name] [age] years old from [location] wants to become [career goal]'" +
            "\n\nExample final prompt: 'FINAL_PROMPT: Priya 15 years old from Mumbai wants to become doctor'" +
            "\n\nBe efficient and inspiring - students deserve quick, motivational roadmaps!";
        
        LiveConfig.Part instructionPart = new LiveConfig.Part(udaanPromptCreatorInstruction);
        LiveConfig.SystemInstruction sysInstruction = new LiveConfig.SystemInstruction(Arrays.asList(instructionPart));
        config.setSystemInstruction(sysInstruction);
        
        // Configure for text-only responses
        LiveConfig.GenerationConfig genConfig = new LiveConfig.GenerationConfig();
        genConfig.setResponseModalities("text");
        config.setGenerationConfig(genConfig);
        
        logger.info("Created Udaan prompt creator config");
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
    
    // Video Generation Methods
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    
    // Create a more permissive HTTP client for development
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
    
    public CompletableFuture<String> generateVideoPrompt(Map<String, Object> request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> context = (Map<String, Object>) request.get("context");
                
                String teachingPrompt = (String) context.get("teachingPrompt");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> chatHistory = (List<Map<String, Object>>) context.get("chatHistory");
                
                // Build context string
                StringBuilder contextBuilder = new StringBuilder();
                contextBuilder.append("Teaching Assistant Context: ").append(teachingPrompt != null ? teachingPrompt : "General teaching assistant").append("\n\n");
                contextBuilder.append("Recent Conversation:\n");
                
                if (chatHistory != null && !chatHistory.isEmpty()) {
                    for (Map<String, Object> message : chatHistory) {
                        String role = (String) message.get("role");
                        String content = (String) message.get("content");
                        contextBuilder.append(role.equals("user") ? "Student: " : "Teacher: ").append(content).append("\n");
                    }
                } else {
                    contextBuilder.append("No conversation history available.\n");
                }
                
                String contextString = contextBuilder.toString();
                
                // Create structured prompt for Veo 3.0 video generation
                String promptText = "Analyze the teaching context and conversation to create a structured educational video prompt for Veo 3.0.\n\n" +
                    "Generate a detailed video prompt using this EXACT format:\n\n" +
                    "prompt_name: \"[Educational Topic] - [Brief Description]\"\n" +
                    "base_style: \"educational, clear, engaging, 4K\"\n" +
                    "aspect_ratio: \"16:9\"\n" +
                    "setting_description: \"[Describe the educational setting - classroom, lab, outdoor, etc.]\"\n" +
                    "camera_setup: \"[Camera angle and movement - fixed wide shot, close-up, pan, etc.]\"\n" +
                    "key_elements:\n" +
                    "- \"[Main educational element 1]\"\n" +
                    "- \"[Main educational element 2]\"\n" +
                    "educational_elements:\n" +
                    "- \"[Learning visual 1]\"\n" +
                    "- \"[Learning visual 2]\"\n" +
                    "- \"[Learning visual 3]\"\n" +
                    "negative_prompts: [\"no distracting elements\", \"no inappropriate content\", \"clear audio\"]\n" +
                    "timeline:\n" +
                    "- sequence: 1\n" +
                    "  timestamp: \"00:00-00:02\"\n" +
                    "  action: \"[Opening scene description]\"\n" +
                    "  audio: \"[Narration or sound description]\"\n" +
                    "- sequence: 2\n" +
                    "  timestamp: \"00:02-00:06\"\n" +
                    "  action: \"[Main teaching demonstration]\"\n" +
                    "  audio: \"[Educational explanation]\"\n" +
                    "- sequence: 3\n" +
                    "  timestamp: \"00:06-00:08\"\n" +
                    "  action: \"[Conclusion or summary visual]\"\n" +
                    "  audio: \"[Closing narration]\"\n\n" +
                    "CONTEXT TO ANALYZE:\n" + contextString + "\n\n" +
                    "Generate the structured video prompt now:";
                
                // Create request body for Gemini API
                Map<String, Object> requestBody = new HashMap<>();
                Map<String, Object> content = new HashMap<>();
                Map<String, Object> parts = new HashMap<>();
                parts.put("text", promptText);
                content.put("parts", Arrays.asList(parts));
                requestBody.put("contents", Arrays.asList(content));
                
                String requestJson = objectMapper.writeValueAsString(requestBody);
                
                // Log the prompt generation request
                logger.info("=== VIDEO PROMPT GENERATION REQUEST ===");
                logger.info("Context String: {}", contextString);
                logger.info("Enhanced Prompt Text: {}", promptText);
                logger.info("Request JSON: {}", requestJson);
                logger.info("========================================");
                
                // Make HTTP request to Gemini API
                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
                    .header("x-goog-api-key", geminiApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();
                
                HttpClient client = createPermissiveHttpClient();
                HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Failed to generate video prompt. Status: " + response.statusCode() + ", Body: " + response.body());
                }
                
                // Parse response
                JsonNode responseJson = objectMapper.readTree(response.body());
                JsonNode candidates = responseJson.get("candidates");
                if (candidates != null && candidates.size() > 0) {
                    JsonNode firstCandidate = candidates.get(0);
                    JsonNode contentNode = firstCandidate.get("content");
                    if (contentNode != null) {
                        JsonNode partsNode = contentNode.get("parts");
                        if (partsNode != null && partsNode.size() > 0) {
                            JsonNode textNode = partsNode.get(0).get("text");
                            if (textNode != null) {
                                String generatedPrompt = textNode.asText();
                                logger.info("Generated video prompt: {}", generatedPrompt);
                                return generatedPrompt;
                            }
                        }
                    }
                }
                
                throw new RuntimeException("No valid prompt generated from Gemini API response");
                
            } catch (Exception e) {
                logger.error("Error generating video prompt", e);
                throw new RuntimeException("Failed to generate video prompt", e);
            }
        });
    }
    
    public CompletableFuture<String> generateVideo(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create request body for Veo video generation
                Map<String, Object> instance = new HashMap<>();
                instance.put("prompt", prompt);
                
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("instances", Arrays.asList(instance));
                
                String requestJson = objectMapper.writeValueAsString(requestBody);
                
                // Log the video generation request
                logger.info("=== VIDEO GENERATION REQUEST ===");
                logger.info("Video Prompt: {}", prompt);
                logger.info("Request JSON: {}", requestJson);
                logger.info("================================");
                
                // Make HTTP request to Veo API
                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/veo-3.0-generate-preview:predictLongRunning"))
                    .header("x-goog-api-key", geminiApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();
                
                HttpClient client = createPermissiveHttpClient();
                HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Failed to start video generation. Status: " + response.statusCode() + ", Body: " + response.body());
                }
                
                // Parse response to get operation name
                JsonNode responseJson = objectMapper.readTree(response.body());
                JsonNode nameNode = responseJson.get("name");
                if (nameNode != null) {
                    String operationName = nameNode.asText();
                    logger.info("Video generation started with operation: {}", operationName);
                    return operationName;
                }
                
                throw new RuntimeException("No operation name returned from video generation API");
                
            } catch (Exception e) {
                logger.error("Error starting video generation", e);
                throw new RuntimeException("Failed to start video generation", e);
            }
        });
    }
    
    public CompletableFuture<Map<String, Object>> getVideoStatus(String operationName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Make HTTP request to check operation status
                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/" + operationName))
                    .header("x-goog-api-key", geminiApiKey)
                    .GET()
                    .build();
                
                HttpClient client = createPermissiveHttpClient();
                HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Failed to check video status. Status: " + response.statusCode() + ", Body: " + response.body());
                }
                
                // Parse response
                JsonNode responseJson = objectMapper.readTree(response.body());
                Map<String, Object> result = new HashMap<>();
                
                JsonNode doneNode = responseJson.get("done");
                boolean isDone = doneNode != null && doneNode.asBoolean();
                result.put("done", isDone);
                
                if (isDone) {
                    // Extract video URI
                    JsonNode responseNode = responseJson.get("response");
                    if (responseNode != null) {
                        JsonNode generateVideoResponse = responseNode.get("generateVideoResponse");
                        if (generateVideoResponse != null) {
                            JsonNode generatedSamples = generateVideoResponse.get("generatedSamples");
                            if (generatedSamples != null && generatedSamples.size() > 0) {
                                JsonNode firstSample = generatedSamples.get(0);
                                JsonNode video = firstSample.get("video");
                                if (video != null) {
                                    JsonNode uriNode = video.get("uri");
                                    if (uriNode != null) {
                                        String videoUri = uriNode.asText();
                                        result.put("videoUri", videoUri);
                                        logger.info("Video generation completed. URI: {}", videoUri);
                                    }
                                }
                            }
                        }
                    }
                }
                
                return result;
                
            } catch (Exception e) {
                logger.error("Error checking video status", e);
                throw new RuntimeException("Failed to check video status", e);
            }
        });
    }
    
    public CompletableFuture<byte[]> downloadVideo(String videoUri) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Make HTTP request to download video
                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(videoUri))
                    .header("x-goog-api-key", geminiApiKey)
                    .GET()
                    .build();
                
                HttpClient client = createPermissiveHttpClient();
                HttpResponse<byte[]> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Failed to download video. Status: " + response.statusCode());
                }
                
                byte[] videoData = response.body();
                logger.info("Video downloaded successfully. Size: {} bytes", videoData.length);
                return videoData;
                
            } catch (Exception e) {
                logger.error("Error downloading video", e);
                throw new RuntimeException("Failed to download video", e);
            }
        });
    }
    
    public CompletableFuture<Map<String, Object>> generateFuturePlan(String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Calling external future planner API for text: {}", text);
                
                // Create request body for the external API
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("text", text);
                
                String requestJson = objectMapper.writeValueAsString(requestBody);
                
                // Make HTTP request to the external future planner API
                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://future-planner-api-1026861423924.us-central1.run.app/generate-plan-from-text"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();
                
                HttpClient client = createPermissiveHttpClient();
                HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("Failed to generate future plan. Status: " + response.statusCode() + ", Body: " + response.body());
                }
                
                // Parse response
                @SuppressWarnings("unchecked")
                Map<String, Object> responseData = objectMapper.readValue(response.body(), Map.class);
                
                logger.info("Future plan generated successfully");
                return responseData;
                
            } catch (Exception e) {
                logger.error("Error generating future plan", e);
                throw new RuntimeException("Failed to generate future plan", e);
            }
        });
    }
}
