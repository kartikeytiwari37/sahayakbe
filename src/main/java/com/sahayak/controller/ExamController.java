package com.sahayak.controller;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.protobuf.ByteString;
import com.sahayak.model.ExamCreationRequest;
import com.sahayak.model.ExamCreationResponse;
import com.sahayak.service.ExamCreationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import com.google.cloud.documentai.v1.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.Base64;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestController
@RequestMapping("/api/exam")
@CrossOrigin(origins = "*")
public class ExamController {

    private static final Logger logger = LoggerFactory.getLogger(ExamController.class);
    
    // Google Document AI configuration injected from application.properties
    @Value("${google.documentai.project-id}")
    private String projectId;
    
    @Value("${google.documentai.location}")
    private String location;
    
    @Value("${google.documentai.processor-id}")
    private String processorId;
    
    @Value("${google.documentai.credentials-file}")
    private String credentialsFile;
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;
    
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
     * Endpoint to create an exam based on PDF content and provided parameters
     * 
     * @param subject The subject of the exam
     * @param gradeLevel The grade level of the exam
     * @param examType The type of exam (e.g., MULTIPLE_CHOICE, TRUE_FALSE)
     * @param numberOfQuestions The number of questions to generate
     * @param customPrompt Custom instructions for question generation
     * @param pdfFile The PDF file containing content for question generation
     * @return The exam creation response
     */
    @PostMapping("/createWithPdf")
    public ResponseEntity<ExamCreationResponse> createExamWithPdf(
            @RequestParam("subject") String subject,
            @RequestParam("gradeLevel") String gradeLevel,
            @RequestParam("examType") String examType,
            @RequestParam("numberOfQuestions") int numberOfQuestions,
            @RequestParam("customPrompt") String customPrompt,
            @RequestParam("pdfFile") MultipartFile pdfFile,
            @RequestParam(value = "pageNumber", required = false) Integer pageNumber) {
        
        logger.info("Received request to create exam with PDF. Subject: {}, Grade: {}, Type: {}, Questions: {}", 
                subject, gradeLevel, examType, numberOfQuestions);
        
        try {
            // First, summarize the PDF to extract its content
            // If pageNumber is provided, extract content from that specific page
            // Otherwise, extract content from all pages (pageNumber = 0)
            ResponseEntity<String> pdfSummaryResponse = summarizePdf(
                    pdfFile, 
                    pageNumber != null ? pageNumber : 0, 
                    "Extract all text content for generating exam questions");
            
            if (pdfSummaryResponse.getStatusCode().isError()) {
                logger.error("Error extracting content from PDF: {}", pdfSummaryResponse.getBody());
                ExamCreationResponse errorResponse = new ExamCreationResponse("error", 
                        "Failed to extract content from PDF: " + pdfSummaryResponse.getBody());
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            String pdfContent = pdfSummaryResponse.getBody();
            logger.info("Successfully extracted content from PDF, length: {} characters", pdfContent.length());
            
            // Create a combined prompt with the custom prompt and PDF content
            String combinedPrompt = customPrompt + "\n\nContent from PDF:\n" + pdfContent;
            
            // Create the exam creation request
            ExamCreationRequest request = new ExamCreationRequest(
                    subject, gradeLevel, examType, numberOfQuestions, combinedPrompt);
            
            logger.info("Created exam request with PDF content");
            
            // Use the existing service to create the exam
            ExamCreationResponse response = examCreationService.createExam(request);
            
            if ("error".equals(response.getStatus())) {
                logger.error("Error creating exam with PDF: {}", response.getError());
                return ResponseEntity.badRequest().body(response);
            }
            
            logger.info("Exam with PDF content created successfully");
            return ResponseEntity.ok(response);
        } catch (HttpMessageNotReadableException e) {
            logger.error("Error parsing request parameters", e);
            ExamCreationResponse errorResponse = new ExamCreationResponse("error", 
                    "Invalid request format: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error creating exam with PDF", e);
            ExamCreationResponse errorResponse = new ExamCreationResponse("error", 
                    "Unexpected error: " + e.getMessage());
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

    
    /**
     * Endpoint to summarize content from a PDF file using Gemini API
     * 
     * @param pdfFile The PDF file to summarize
     * @param pageNumber The page number to summarize (default is 1, 0 means all pages)
     * @param prompt Custom prompt for summarization (optional)
     * @return Summary of the specified page or the entire PDF
     */
    @PostMapping("/summarize-pdf")
    public ResponseEntity<String> summarizePdf(
            @RequestParam("pdfFile") MultipartFile pdfFile,
            @RequestParam(value = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(value = "prompt", required = false) String prompt) {
        
        try {
            if (pageNumber == 0) {
                logger.info("Received request to summarize all pages of PDF. File size: {} bytes", 
                        pdfFile.getSize());
            } else {
                logger.info("Received request to summarize PDF page {}. File size: {} bytes", 
                        pageNumber, pdfFile.getSize());
            }
            
            // Check if the PDF file is valid
            byte[] pdfBytes = pdfFile.getBytes();
            if (pdfBytes.length < 4 || !isPdfFileSignature(pdfBytes)) {
                return ResponseEntity.badRequest().body(
                        "Error: The uploaded file does not appear to be a valid PDF. Please check the file and try again.");
            }
            
            // Convert PDF to base64
            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
            logger.info("PDF file converted to base64 successfully");
            
            // Create the prompt based on whether we're summarizing a specific page or the entire document
            String summarizationPrompt;
            if (pageNumber > 0) {
                // Summarize specific page
                summarizationPrompt = prompt != null && !prompt.isEmpty() 
                        ? prompt + " (focus on page " + pageNumber + ")"
                        : "Extract and summarize the content from page " + pageNumber + " of this PDF document";
            } else {
                // Summarize all pages
                summarizationPrompt = prompt != null && !prompt.isEmpty() 
                        ? prompt + " (include content from all pages)"
                        : "Extract and summarize the content from all pages of this PDF document";
            }
            
            // Create the request body for Gemini API
            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();
            List<Map<String, Object>> parts = new ArrayList<>();
            
            // Add PDF part
            Map<String, Object> pdfPart = new HashMap<>();
            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mime_type", "application/pdf");
            inlineData.put("data", base64Pdf);
            pdfPart.put("inline_data", inlineData);
            parts.add(pdfPart);
            
            // Add text prompt part
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", summarizationPrompt);
            parts.add(textPart);
            
            content.put("parts", parts);
            contents.add(content);
            requestBody.put("contents", contents);
            
            // Call Gemini API
            String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent";
            String urlWithApiKey = geminiUrl + "?key=" + geminiApiKey;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            if (pageNumber > 0) {
                logger.info("Calling Gemini API to summarize PDF page {}", pageNumber);
            } else {
                logger.info("Calling Gemini API to summarize all pages of the PDF");
            }
            
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.exchange(
                    urlWithApiKey, 
                    HttpMethod.POST, 
                    entity, 
                    Map.class);
            
            // Extract the text from the response
            String summary = extractTextFromGeminiResponse(response.getBody());
            logger.info("Successfully received summary from Gemini API");
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error summarizing PDF with Gemini API", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error summarizing PDF: " + e.getMessage());
        }
    }
    
    /**
     * Extract text from Gemini API response
     * 
     * @param response The response from Gemini API
     * @return The extracted text
     */
    @SuppressWarnings("unchecked")
    private String extractTextFromGeminiResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> candidate = candidates.get(0);
                Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
            return "No summary available in the response";
        } catch (Exception e) {
            logger.error("Error extracting text from Gemini response", e);
            return "Error extracting summary: " + e.getMessage();
        }
    }
    
    /**
     * Check if a byte array has a valid PDF file signature
     * 
     * @param bytes The byte array to check
     * @return true if the byte array starts with a PDF signature, false otherwise
     */
    private boolean isPdfFileSignature(byte[] bytes) {
        if (bytes.length < 4) {
            return false;
        }
        
        // Check for PDF signature %PDF
        return bytes[0] == 0x25 && // %
               bytes[1] == 0x50 && // P
               bytes[2] == 0x44 && // D
               bytes[3] == 0x46;   // F
    }
}
