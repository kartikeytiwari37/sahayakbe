package com.sahayak.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)

public class LiveConfig {
    private String model;
    
    @JsonProperty("systemInstruction")
    private SystemInstruction systemInstruction;
    
    @JsonProperty("generationConfig")
    private GenerationConfig generationConfig;
    
    private List<Tool> tools;

    // Constructors
    public LiveConfig() {}

    public LiveConfig(String model) {
        this.model = model;
    }

    // Getters and Setters
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public SystemInstruction getSystemInstruction() {
        return systemInstruction;
    }

    public void setSystemInstruction(SystemInstruction systemInstruction) {
        this.systemInstruction = systemInstruction;
    }

    public GenerationConfig getGenerationConfig() {
        return generationConfig;
    }

    public void setGenerationConfig(GenerationConfig generationConfig) {
        this.generationConfig = generationConfig;
    }

    public List<Tool> getTools() {
        return tools;
    }

    public void setTools(List<Tool> tools) {
        this.tools = tools;
    }

    public static class SystemInstruction {
        private List<Part> parts;

        public SystemInstruction() {}

        public SystemInstruction(List<Part> parts) {
            this.parts = parts;
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
    }

    public static class GenerationConfig {
        private String responseModalities;
        private SpeechConfig speechConfig;

        public String getResponseModalities() {
            return responseModalities;
        }

        public void setResponseModalities(String responseModalities) {
            this.responseModalities = responseModalities;
        }

        public SpeechConfig getSpeechConfig() {
            return speechConfig;
        }

        public void setSpeechConfig(SpeechConfig speechConfig) {
            this.speechConfig = speechConfig;
        }
    }

    public static class SpeechConfig {
        private VoiceConfig voiceConfig;

        public VoiceConfig getVoiceConfig() {
            return voiceConfig;
        }

        public void setVoiceConfig(VoiceConfig voiceConfig) {
            this.voiceConfig = voiceConfig;
        }
    }

    public static class VoiceConfig {
        private PrebuiltVoiceConfig prebuiltVoiceConfig;

        public PrebuiltVoiceConfig getPrebuiltVoiceConfig() {
            return prebuiltVoiceConfig;
        }

        public void setPrebuiltVoiceConfig(PrebuiltVoiceConfig prebuiltVoiceConfig) {
            this.prebuiltVoiceConfig = prebuiltVoiceConfig;
        }
    }

    public static class PrebuiltVoiceConfig {
        private String voiceName;

        public PrebuiltVoiceConfig() {}

        public PrebuiltVoiceConfig(String voiceName) {
            this.voiceName = voiceName;
        }

        public String getVoiceName() {
            return voiceName;
        }

        public void setVoiceName(String voiceName) {
            this.voiceName = voiceName;
        }
    }

    public static class Tool {
        private Map<String, Object> googleSearch;

        public Map<String, Object> getGoogleSearch() {
            return googleSearch;
        }

        public void setGoogleSearch(Map<String, Object> googleSearch) {
            this.googleSearch = googleSearch;
        }
    }
}
