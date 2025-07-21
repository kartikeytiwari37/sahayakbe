package com.sahayak.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahayak.model.GeminiMessages.*;
import com.sahayak.model.LiveConfig;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class GeminiLiveWebSocketClient extends WebSocketClient {
    
    private static final Logger logger = LoggerFactory.getLogger(GeminiLiveWebSocketClient.class);
    
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private Consumer<String> audioDataHandler;
    private Consumer<String> contentHandler;
    private Consumer<String> errorHandler;
    private CompletableFuture<Void> connectionFuture;
    private CompletableFuture<Void> setupFuture;
    private boolean setupComplete = false;
    
    public GeminiLiveWebSocketClient(String geminiUrl, String apiKey, 
                                   ObjectMapper objectMapper, 
                                   ApplicationEventPublisher eventPublisher) {
        super(URI.create(geminiUrl + "?key=" + apiKey));
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.connectionFuture = new CompletableFuture<>();
        
        // Configure SSL to trust all certificates (for development)
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };
            
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            this.setSocketFactory(sslContext.getSocketFactory());
        } catch (Exception e) {
            logger.warn("Failed to configure SSL context: {}", e.getMessage());
        }
    }
    
    @Override
    public void onOpen(ServerHandshake handshake) {
        logger.info("Connected to Gemini Live API");
        connectionFuture.complete(null);
    }
    
    @Override
    public void onMessage(String message) {
        try {
            logger.info("Received TEXT message from Gemini: {}", message);
            // Handle text messages if any
            processMessage(message);
        } catch (Exception e) {
            logger.error("Error processing text message: {}", message, e);
        }
    }
    
    @Override
    public void onMessage(java.nio.ByteBuffer bytes) {
        try {
            // Convert ByteBuffer to String for processing
            byte[] array = new byte[bytes.remaining()];
            bytes.get(array);
            String message = new String(array, java.nio.charset.StandardCharsets.UTF_8);
            logger.info("Received BINARY message from Gemini: {}", message);
            processMessage(message);
        } catch (Exception e) {
            logger.error("Error processing binary message", e);
        }
    }
    
    private void processMessage(String message) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(message);
        
        if (jsonNode.has("setupComplete")) {
            logger.info("Setup completed successfully");
            setupComplete = true;
            if (setupFuture != null) {
                setupFuture.complete(null);
            }
            return;
        }
        
        if (jsonNode.has("serverContent")) {
            handleServerContent(jsonNode.get("serverContent"));
        } else {
            logger.warn("Received message without serverContent: {}", message);
        }
    }
    
    private void handleServerContent(JsonNode serverContent) throws Exception {
        if (serverContent.has("interrupted") && serverContent.get("interrupted").asBoolean()) {
            logger.info("Conversation interrupted");
            return;
        }
        
        if (serverContent.has("turnComplete") && serverContent.get("turnComplete").asBoolean()) {
            logger.info("Turn completed");
            return;
        }
        
        if (serverContent.has("modelTurn")) {
            JsonNode modelTurn = serverContent.get("modelTurn");
            if (modelTurn.has("parts")) {
                JsonNode parts = modelTurn.get("parts");
                
                StringBuilder textContent = new StringBuilder();
                
                for (JsonNode part : parts) {
                    if (part.has("inlineData")) {
                        JsonNode inlineData = part.get("inlineData");
                        String mimeType = inlineData.get("mimeType").asText();
                        String data = inlineData.get("data").asText();
                        
                        if (mimeType.startsWith("audio/pcm")) {
                            logger.info("Received audio data, size: {}", data.length());
                            if (audioDataHandler != null) {
                                audioDataHandler.accept(data);
                            }
                        }
                    } else if (part.has("text")) {
                        String text = part.get("text").asText();
                        logger.info("Received text part: {}", text);
                        textContent.append(text);
                    }
                }
                
                // Send accumulated text content if any
                if (textContent.length() > 0) {
                    String fullText = textContent.toString();
                    logger.info("Sending complete text response: {}", fullText);
                    if (contentHandler != null) {
                        contentHandler.accept(fullText);
                    }
                }
            }
        }
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.info("Connection closed: {} - {}", code, reason);
        if (!connectionFuture.isDone()) {
            connectionFuture.completeExceptionally(new RuntimeException("Connection closed: " + reason));
        }
    }
    
    @Override
    public void onError(Exception ex) {
        logger.error("WebSocket error", ex);
        if (!connectionFuture.isDone()) {
            connectionFuture.completeExceptionally(ex);
        }
        if (errorHandler != null) {
            errorHandler.accept("WebSocket error: " + ex.getMessage());
        }
    }
    
    public CompletableFuture<Void> connectAsync() {
        connect();
        return connectionFuture;
    }
    
    public void sendSetupMessage(LiveConfig config) {
        try {
            SetupMessage setupMessage = new SetupMessage(config);
            String json = objectMapper.writeValueAsString(setupMessage);
            logger.debug("Sending setup message: {}", json);
            send(json);
        } catch (Exception e) {
            logger.error("Error sending setup message", e);
            throw new RuntimeException("Failed to send setup message", e);
        }
    }
    
    public void sendAudioData(String base64AudioData) {
        try {
            MediaChunk audioChunk = new MediaChunk("audio/pcm;rate=16000", base64AudioData);
            RealtimeInput realtimeInput = new RealtimeInput(Arrays.asList(audioChunk));
            RealtimeInputMessage message = new RealtimeInputMessage(realtimeInput);
            
            String json = objectMapper.writeValueAsString(message);
            logger.debug("Sending audio data, size: {}", base64AudioData.length());
            send(json);
        } catch (Exception e) {
            logger.error("Error sending audio data", e);
            throw new RuntimeException("Failed to send audio data", e);
        }
    }
    
    public void sendVideoData(String base64VideoData) {
        try {
            MediaChunk videoChunk = new MediaChunk("image/jpeg", base64VideoData);
            RealtimeInput realtimeInput = new RealtimeInput(Arrays.asList(videoChunk));
            RealtimeInputMessage message = new RealtimeInputMessage(realtimeInput);
            
            String json = objectMapper.writeValueAsString(message);
            logger.debug("Sending video data, size: {}", base64VideoData.length());
            send(json);
        } catch (Exception e) {
            logger.error("Error sending video data", e);
            throw new RuntimeException("Failed to send video data", e);
        }
    }
    
    public void sendTextMessage(String text) {
        try {
            Part textPart = new Part(text);
            Content content = new Content("user", Arrays.asList(textPart));
            ClientContent clientContent = new ClientContent(Arrays.asList(content), true);
            ClientContentMessage message = new ClientContentMessage(clientContent);
            
            String json = objectMapper.writeValueAsString(message);
            logger.debug("Sending text message: {}", text);
            send(json);
        } catch (Exception e) {
            logger.error("Error sending text message", e);
            throw new RuntimeException("Failed to send text message", e);
        }
    }
    
    // Setters for handlers
    public void setAudioDataHandler(Consumer<String> audioDataHandler) {
        this.audioDataHandler = audioDataHandler;
    }
    
    public void setContentHandler(Consumer<String> contentHandler) {
        this.contentHandler = contentHandler;
    }
    
    public void setErrorHandler(Consumer<String> errorHandler) {
        this.errorHandler = errorHandler;
    }
}
