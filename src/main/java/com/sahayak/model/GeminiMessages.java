package com.sahayak.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class GeminiMessages {

    // Outgoing Messages
    public static class SetupMessage {
        private LiveConfig setup;

        public SetupMessage() {}

        public SetupMessage(LiveConfig setup) {
            this.setup = setup;
        }

        public LiveConfig getSetup() {
            return setup;
        }

        public void setSetup(LiveConfig setup) {
            this.setup = setup;
        }
    }

    public static class RealtimeInputMessage {
        @JsonProperty("realtimeInput")
        private RealtimeInput realtimeInput;

        public RealtimeInputMessage() {}

        public RealtimeInputMessage(RealtimeInput realtimeInput) {
            this.realtimeInput = realtimeInput;
        }

        public RealtimeInput getRealtimeInput() {
            return realtimeInput;
        }

        public void setRealtimeInput(RealtimeInput realtimeInput) {
            this.realtimeInput = realtimeInput;
        }
    }

    public static class RealtimeInput {
        @JsonProperty("mediaChunks")
        private List<MediaChunk> mediaChunks;

        public RealtimeInput() {}

        public RealtimeInput(List<MediaChunk> mediaChunks) {
            this.mediaChunks = mediaChunks;
        }

        public List<MediaChunk> getMediaChunks() {
            return mediaChunks;
        }

        public void setMediaChunks(List<MediaChunk> mediaChunks) {
            this.mediaChunks = mediaChunks;
        }
    }

    public static class MediaChunk {
        @JsonProperty("mimeType")
        private String mimeType;
        private String data;

        public MediaChunk() {}

        public MediaChunk(String mimeType, String data) {
            this.mimeType = mimeType;
            this.data = data;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

    public static class ClientContentMessage {
        @JsonProperty("clientContent")
        private ClientContent clientContent;

        public ClientContentMessage() {}

        public ClientContentMessage(ClientContent clientContent) {
            this.clientContent = clientContent;
        }

        public ClientContent getClientContent() {
            return clientContent;
        }

        public void setClientContent(ClientContent clientContent) {
            this.clientContent = clientContent;
        }
    }

    public static class ClientContent {
        private List<Content> turns;
        @JsonProperty("turnComplete")
        private boolean turnComplete;

        public ClientContent() {}

        public ClientContent(List<Content> turns, boolean turnComplete) {
            this.turns = turns;
            this.turnComplete = turnComplete;
        }

        public List<Content> getTurns() {
            return turns;
        }

        public void setTurns(List<Content> turns) {
            this.turns = turns;
        }

        public boolean isTurnComplete() {
            return turnComplete;
        }

        public void setTurnComplete(boolean turnComplete) {
            this.turnComplete = turnComplete;
        }
    }

    public static class Content {
        private String role;
        private List<Part> parts;

        public Content() {}

        public Content(String role, List<Part> parts) {
            this.role = role;
            this.parts = parts;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }

    public static class Part {
        private String text;
        @JsonProperty("inlineData")
        private InlineData inlineData;

        public Part() {}

        public Part(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public InlineData getInlineData() {
            return inlineData;
        }

        public void setInlineData(InlineData inlineData) {
            this.inlineData = inlineData;
        }
    }

    public static class InlineData {
        @JsonProperty("mimeType")
        private String mimeType;
        private String data;

        public InlineData() {}

        public InlineData(String mimeType, String data) {
            this.mimeType = mimeType;
            this.data = data;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

    // Incoming Messages
    public static class ServerContentMessage {
        @JsonProperty("serverContent")
        private ServerContent serverContent;

        public ServerContentMessage() {}

        public ServerContent getServerContent() {
            return serverContent;
        }

        public void setServerContent(ServerContent serverContent) {
            this.serverContent = serverContent;
        }
    }

    public static class ServerContent {
        @JsonProperty("modelTurn")
        private ModelTurn modelTurn;
        @JsonProperty("turnComplete")
        private Boolean turnComplete;
        private Boolean interrupted;

        public ModelTurn getModelTurn() {
            return modelTurn;
        }

        public void setModelTurn(ModelTurn modelTurn) {
            this.modelTurn = modelTurn;
        }

        public Boolean getTurnComplete() {
            return turnComplete;
        }

        public void setTurnComplete(Boolean turnComplete) {
            this.turnComplete = turnComplete;
        }

        public Boolean getInterrupted() {
            return interrupted;
        }

        public void setInterrupted(Boolean interrupted) {
            this.interrupted = interrupted;
        }
    }

    public static class ModelTurn {
        private List<Part> parts;

        public ModelTurn() {}

        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }

    public static class SetupCompleteMessage {
        @JsonProperty("setupComplete")
        private Map<String, Object> setupComplete;

        public Map<String, Object> getSetupComplete() {
            return setupComplete;
        }

        public void setSetupComplete(Map<String, Object> setupComplete) {
            this.setupComplete = setupComplete;
        }
    }
}
