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
     * Diagnostic endpoint to verify Document AI configuration
     * 
     * @return Configuration status
     */
    @GetMapping("/document-ai-config")
    public ResponseEntity<String> checkDocumentAiConfig() {
        StringBuilder result = new StringBuilder();
        result.append("Document AI Configuration:\n");
        result.append("- Project ID: ").append(projectId).append("\n");
        result.append("- Location: ").append(location).append("\n");
        result.append("- Processor ID: ").append(processorId).append("\n");
        result.append("- Credentials File: ").append(credentialsFile).append("\n\n");
        
        try {
            // Try to load the credentials file
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    getClass().getResourceAsStream(credentialsFile.replace("classpath:", "/")));
            result.append("✅ Credentials file loaded successfully\n");
            
            // Try to create the Document AI client
            DocumentProcessorServiceSettings settings = DocumentProcessorServiceSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();
            
            try (DocumentProcessorServiceClient client = DocumentProcessorServiceClient.create(settings)) {
                result.append("✅ Document AI client created successfully\n");
                
                // Try to format the processor name
                String name = String.format("projects/%s/locations/%s/processors/%s", 
                        projectId, location, processorId);
                result.append("✅ Processor name formatted: ").append(name).append("\n");
                
                // Try to get the processor (this will verify if the processor exists)
                try {
                    client.getProcessor(name);
                    result.append("✅ Processor exists and is accessible\n");
                    result.append("\nAll Document AI configuration checks passed!");
                } catch (Exception e) {
                    result.append("❌ Error accessing processor: ").append(e.getMessage()).append("\n");
                    result.append("\nPossible issues:\n");
                    result.append("1. The processor ID may be incorrect\n");
                    result.append("2. The processor may not exist in the specified location\n");
                    result.append("3. The service account may not have permission to access this processor\n");
                }
            }
        } catch (Exception e) {
            result.append("❌ Error: ").append(e.getMessage()).append("\n");
            result.append("\nPossible issues:\n");
            result.append("1. The credentials file may not be found in the resources directory\n");
            result.append("2. The credentials file may be invalid or corrupted\n");
            result.append("3. The service account may not have the required permissions\n");
        }
        
        return ResponseEntity.ok(result.toString());
    }


    /**
     * Endpoint to generate exam questions from a PDF file using Google Document AI
     * 
     * To set up Google Document AI:
     * 1. Project ID: 
     *    - Go to Google Cloud Console (https://console.cloud.google.com/)
     *    - The project ID is displayed at the top of the dashboard or in the project selector dropdown
     *    - Example: "my-project-123456"
     * 
     * 2. Location:
     *    - Document AI is available in specific regions
     *    - Common values: "us" (United States), "eu" (Europe)
     *    - Find available locations in the Document AI processor creation page
     * 
     * 3. Processor ID:
     *    - Go to Google Cloud Console → Document AI → Processors
     *    - Create a new processor or select an existing one
     *    - The processor ID is shown in the processor details page or in the URL
     *    - Example: "a1b2c3d4e5f6g7h8"
     * 
     * 4. Service Account Key:
     *    - Go to Google Cloud Console → IAM & Admin → Service Accounts
     *    - Create a new service account or select an existing one
     *    - Grant it the "Document AI Editor" role
     *    - To download the JSON key:
     *      a. Click on the service account name to open its details
     *      b. Go to the "Keys" tab
     *      c. Click "Add Key" → "Create new key"
     *      d. Select "JSON" as the key type
     *      e. Click "Create" - the JSON key file will be automatically downloaded
     *    - Rename the downloaded file (e.g., to "service-account-key.json")
     *    - Place the file in src/main/resources/ directory of your project
     *    - Update application.properties with the path (e.g., "classpath:service-account-key.json")
     * 
     * @param pdfFile The PDF file to extract text from
     * @param topic Optional topic to focus the questions on
     * @return Generated exam questions based on the PDF content
     */
    @PostMapping("/generate-questions")
    public String generateQuestions(@RequestParam("pdfFile") MultipartFile pdfFile,
                                    @RequestParam(value = "topic", required = false) String topic) {
        try {
            logger.info("Received request to generate questions from PDF. File size: {} bytes", pdfFile.getSize());
            logger.info("Using Document AI configuration - Project ID: {}, Location: {}, Processor ID: {}", 
                    projectId, location, processorId);
            
            // Step 3.1: Authenticate and initialize Document AI client
            DocumentProcessorServiceSettings settings = DocumentProcessorServiceSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(
                            GoogleCredentials.fromStream(
                                    getClass().getResourceAsStream(credentialsFile.replace("classpath:", "/")))))
                    .build();
            
            try (DocumentProcessorServiceClient client = DocumentProcessorServiceClient.create(settings)) {
                // Step 3.2: Prepare PDF bytes
                byte[] pdfBytes = pdfFile.getBytes();
                logger.info("PDF file loaded successfully, size: {} bytes", pdfBytes.length);
                
                // Check if the PDF file is valid
                if (pdfBytes.length < 4 || !isPdfFileSignature(pdfBytes)) {
                    return "Error: The uploaded file does not appear to be a valid PDF. Please check the file and try again.";
                }
                
                RawDocument document = RawDocument.newBuilder()
                        .setContent(ByteString.copyFrom(pdfBytes))
                        .setMimeType("application/pdf")
                        .build();

                // Step 3.3: Process PDF with Document AI
                String name = String.format("projects/%s/locations/%s/processors/%s", projectId, location, processorId);
                logger.info("Document AI processor name: {}", name);
                
                // Create a more basic request with minimal options
                ProcessRequest request = ProcessRequest.newBuilder()
                        .setName(name)
                        .setRawDocument(document)
                        .build();
                
                logger.info("Sending document to Document AI for processing...");
                ProcessResponse response = client.processDocument(request);
                Document documentOutput = response.getDocument();
                String extractedText = documentOutput.getText();
                logger.info("Document processed successfully. Extracted {} characters of text.", extractedText.length());

                // Step 3.4: Prepare LLM prompt
                String prompt = "Generate exam questions based on the following text: " + extractedText;
                return prompt;
            }
        } catch (Exception e) {
            logger.error("Error processing PDF with Document AI", e);
            return "Error processing PDF: " + e.getMessage() + 
                   "\nPlease check the following:\n" +
                   "1. Verify that your processor ID is correct and exists in the specified location\n" +
                   "2. Make sure the PDF file is valid and not corrupted\n" +
                   "3. Confirm that the service account has proper permissions";
        }
    }
    
    /**
     * Endpoint to get information about the Document AI processor
     * 
     * @return Processor information
     */
    @GetMapping("/document-ai-processor-info")
    public ResponseEntity<String> getProcessorInfo() {
        StringBuilder result = new StringBuilder();
        result.append("Document AI Processor Information:\n\n");
        
        try {
            // Load credentials
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    getClass().getResourceAsStream(credentialsFile.replace("classpath:", "/")));
            
            // Create Document AI client
            DocumentProcessorServiceSettings settings = DocumentProcessorServiceSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();
            
            try (DocumentProcessorServiceClient client = DocumentProcessorServiceClient.create(settings)) {
                // Get processor information
                String name = String.format("projects/%s/locations/%s/processors/%s", 
                        projectId, location, processorId);
                
                Processor processor = client.getProcessor(name);
                
                result.append("Processor Name: ").append(processor.getDisplayName()).append("\n");
                result.append("Processor Type: ").append(processor.getType()).append("\n");
                result.append("Processor State: ").append(processor.getState()).append("\n");
                result.append("Create Time: ").append(processor.getCreateTime()).append("\n");
                
                result.append("\nProcessor Types for Document Extraction:\n");
                result.append("- DOCUMENT_OCR: For general document text extraction\n");
                result.append("- FORM_PARSER: For extracting form fields and values\n");
                result.append("- INVOICE_PROCESSOR: For extracting information from invoices\n");
                result.append("- OCR_PROCESSOR: For basic OCR text extraction\n\n");
                
                result.append("Your processor type is: ").append(processor.getType()).append("\n\n");
                
                if (!processor.getType().contains("OCR") && !processor.getType().equals("DOCUMENT_OCR")) {
                    result.append("⚠️ Warning: Your processor type may not be optimal for general text extraction.\n");
                    result.append("For PDF text extraction, it's recommended to use a processor with type DOCUMENT_OCR or OCR_PROCESSOR.\n");
                }
                
                return ResponseEntity.ok(result.toString());
            }
        } catch (Exception e) {
            result.append("Error getting processor information: ").append(e.getMessage());
            return ResponseEntity.ok(result.toString());
        }
    }
    
    /**
     * Endpoint to provide instructions for creating a new Document AI processor for PDF text extraction
     * 
     * @return Instructions for creating a new processor
     */
    @GetMapping("/create-ocr-processor")
    public ResponseEntity<String> createOcrProcessorInstructions() {
        StringBuilder result = new StringBuilder();
        result.append("# Instructions for Creating a Document AI OCR Processor\n\n");
        
        result.append("Your current processor type is CUSTOM_EXTRACTION_PROCESSOR, which is not optimal for general PDF text extraction.\n");
        result.append("Follow these steps to create a new OCR processor specifically for PDF text extraction:\n\n");
        
        result.append("## Step 1: Go to Google Cloud Console\n");
        result.append("1. Open [Google Cloud Console](https://console.cloud.google.com/)\n");
        result.append("2. Make sure you're in the correct project: ").append(projectId).append("\n\n");
        
        result.append("## Step 2: Navigate to Document AI\n");
        result.append("1. In the navigation menu, go to 'Artificial Intelligence' > 'Document AI'\n");
        result.append("2. Click on 'Create Processor'\n\n");
        
        result.append("## Step 3: Create an OCR Processor\n");
        result.append("1. Select 'OCR' or 'Document OCR' processor type\n");
        result.append("2. Give it a name (e.g., 'pdf-text-extraction')\n");
        result.append("3. Select the same region as your current processor: ").append(location).append("\n");
        result.append("4. Click 'Create'\n\n");
        
        result.append("## Step 4: Get the New Processor ID\n");
        result.append("1. After creation, click on the new processor\n");
        result.append("2. Find the Processor ID in the processor details page or in the URL\n");
        result.append("3. Copy this ID\n\n");
        
        result.append("## Step 5: Update Your Configuration\n");
        result.append("1. Update the 'google.documentai.processor-id' property in your application.properties file with the new processor ID\n");
        result.append("2. Restart your application\n\n");
        
        result.append("## Current Configuration\n");
        result.append("- Project ID: ").append(projectId).append("\n");
        result.append("- Location: ").append(location).append("\n");
        result.append("- Current Processor ID: ").append(processorId).append(" (CUSTOM_EXTRACTION_PROCESSOR)\n\n");
        
        result.append("After creating the new OCR processor and updating your configuration, the PDF text extraction should work correctly.");
        
        return ResponseEntity.ok(result.toString());
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
