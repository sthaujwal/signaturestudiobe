package com.wellsfargo.signaturestudio.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wellsfargo.signaturestudio.client.ESignatureServiceClient;
import com.wellsfargo.signaturestudio.domain.Document;
import com.wellsfargo.signaturestudio.domain.FormField;
import com.wellsfargo.signaturestudio.exception.ErrorCode;
import com.wellsfargo.signaturestudio.exception.ServiceException;
import com.wellsfargo.signaturestudio.model.Document;
import com.wellsfargo.signaturestudio.model.Transaction;
import com.wellsfargo.signaturestudio.repository.DocumentRepository;
import com.wellsfargo.signaturestudio.repository.TransactionRepository;
import com.wellsfargo.signaturestudio.util.ESignatureIntegrationHelper;
import com.wellsfargo.signaturestudio.util.UpdateHelper;
import com.wellsfargo.signaturestudio.util.ValidationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);
    
    private final DocumentRepository documentRepository;
    private final TransactionRepository transactionRepository;
    private final ESignatureServiceClient eSignatureServiceClient;
    private final String uploadDir;
    private final ObjectMapper objectMapper;
    
    public DocumentService(
            DocumentRepository documentRepository,
            TransactionRepository transactionRepository,
            ESignatureServiceClient eSignatureServiceClient,
            @Value("${document.upload.dir:./uploads}") String uploadDir,
            ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.transactionRepository = transactionRepository;
        this.eSignatureServiceClient = eSignatureServiceClient;
        this.uploadDir = uploadDir;
        this.objectMapper = objectMapper;
        
        // Create upload directory if it doesn't exist
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            logger.error("Failed to create upload directory: {}", uploadDir, e);
        }
    }
    
    public List<Document> getDocuments(String transactionId) {
        List<Document> documents = documentRepository.findByTransactionId(transactionId);
        return documents.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public Document uploadDocument(String transactionId, MultipartFile file, String uploadedBy, String formFieldsJson) {
        Transaction transaction = findTransaction(transactionId);
        ValidationHelper.requireNonNull(file, "File");
        
        if (file.isEmpty()) {
            throw new ServiceException(ErrorCode.VALIDATION_ERROR, "File is required");
        }
        
        try {
            String documentId = UUID.randomUUID().toString();
            Path filePath = saveFileToStorage(file, documentId);
            
            try {
                Document documentDTO = buildDocument(file, transactionId, documentId, uploadedBy, formFieldsJson);
                Document eSignatureResponse = addDocumentToESignature(transactionId, documentDTO, documentId);
                Document document = saveDocumentMetadata(transaction, file, documentId, eSignatureResponse);
                logger.info("Document metadata saved to DB: {} for transaction: {}", document.getId(), transactionId);
                return toDTO(document);
            } catch (Exception e) {
                cleanupFileOnError(filePath);
                throw e;
            }
        } catch (IOException e) {
            logger.error("Failed to upload document", e);
            throw new ServiceException(ErrorCode.INTERNAL_ERROR, "Failed to upload document", e);
        }
    }
    
    private Transaction findTransaction(String transactionId) {
        return transactionRepository.findById(transactionId)
            .orElseThrow(() -> new ServiceException(ErrorCode.TRANSACTION_NOT_FOUND, 
                "Transaction ID: " + transactionId));
    }
    
    private Path saveFileToStorage(MultipartFile file, String documentId) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String fileName = documentId + "." + fileExtension;
        Path filePath = Paths.get(uploadDir, fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath;
    }
    
    private Document buildDocument(
            MultipartFile file,
            String transactionId,
            String documentId,
            String uploadedBy,
            String formFieldsJson) {
        String originalFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String fileName = documentId + "." + fileExtension;
        
        Document documentDTO = new Document();
        documentDTO.setId(documentId);
        documentDTO.setTransactionId(transactionId);
        documentDTO.setName(originalFileName);
        documentDTO.setTitle(originalFileName);
        documentDTO.setFileName(fileName);
        documentDTO.setOriginalFileName(originalFileName);
        documentDTO.setFileType(fileExtension);
        documentDTO.setFileSize(file.getSize());
        documentDTO.setStoragePath(Paths.get(uploadDir, fileName).toString());
        documentDTO.setStorageUrl("/api/transactions/" + transactionId + "/documents/" + documentId + "/download");
        documentDTO.setMimeType(file.getContentType());
        documentDTO.setUploadedBy(uploadedBy);
        documentDTO.setUploadStatus("completed");
        
        parseAndSetFormFields(documentDTO, formFieldsJson);
        return documentDTO;
    }
    
    private void parseAndSetFormFields(Document documentDTO, String formFieldsJson) {
        if (formFieldsJson == null || formFieldsJson.trim().isEmpty()) {
            return;
        }
        
        try {
            List<FormField> formFields = objectMapper.readValue(
                formFieldsJson, 
                new TypeReference<List<FormField>>() {}
            );
            documentDTO.setFormFields(formFields);
            logger.info("Parsed {} form fields from JSON for document upload", formFields.size());
        } catch (Exception e) {
            logger.warn("Failed to parse form fields JSON, continuing without form fields", e);
        }
    }
    
    private Document addDocumentToESignature(String transactionId, Document documentDTO, String documentId) {
        return ESignatureIntegrationHelper.executeWithErrorHandling(
            "add document in eSignature service",
            () -> eSignatureServiceClient.addDocumentSync(transactionId, documentDTO),
            documentId
        );
    }
    
    private void cleanupFileOnError(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ioException) {
            logger.warn("Failed to delete file after error: {}", filePath, ioException);
        }
    }
    
    private Document saveDocumentMetadata(
            Transaction transaction,
            MultipartFile file,
            String documentId,
            Document eSignatureResponse) {
        Document document = new Document();
        document.setId(documentId);
        document.setTransaction(transaction);
        document.setName(file.getOriginalFilename());
        document.setTitle(file.getOriginalFilename());
        
        if (eSignatureResponse != null && eSignatureResponse.getESignatureDocumentId() != null) {
            document.setESignatureDocumentId(eSignatureResponse.getESignatureDocumentId());
        }
        
        return documentRepository.save(document);
    }
    
    @Transactional
    public void deleteDocument(String transactionId, String documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ServiceException(ErrorCode.RESOURCE_NOT_FOUND, 
                "Document not found: " + documentId));
        
        ValidationHelper.validateResourceBelongsToTransaction(
            documentId, 
            document.getTransaction().getId(), 
            transactionId, 
            "Document");
        
        deleteDocumentFromESignature(transactionId, documentId);
        documentRepository.delete(document);
        logger.info("Document deleted from DB: {} from transaction: {}", documentId, transactionId);
    }
    
    private void deleteDocumentFromESignature(String transactionId, String documentId) {
        List<String> documentIds = List.of(documentId);
        ESignatureIntegrationHelper.executeVoidWithErrorHandling(
            "delete document in eSignature service",
            () -> eSignatureServiceClient.deleteDocuments(transactionId, documentIds),
            documentId
        );
    }
    
    public byte[] downloadDocument(String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.RESOURCE_NOT_FOUND, "Document not found: " + documentId));
        
        // Document files are stored in ESignatureService, not in BFF
        // This method should fetch from ESignatureService or redirect to ESignatureService URL
        // For now, throw an exception indicating this should be handled by ESignatureService
        throw new ServiceException(ErrorCode.INTERNAL_ERROR, 
            "Document download should be handled by ESignatureService. Use document URL from ESignatureService.");
    }
    
    /**
     * Get full document details from ESignatureService
     * Fetches form fields and ICMP objects
     */
    public Document getDocumentDetails(String transactionId, String documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ServiceException(ErrorCode.RESOURCE_NOT_FOUND, 
                "Document not found: " + documentId));
        
        ValidationHelper.validateResourceBelongsToTransaction(
            documentId,
            document.getTransaction().getId(),
            transactionId,
            "Document");
        
        Transaction transaction = document.getTransaction();
        if (transaction.getESignatureTransactionId() == null) {
            return toDTO(document);
        }
        
        return ESignatureIntegrationHelper.executeWithFallback(
            "get document details from ESignatureService",
            () -> fetchDocumentDetailsFromESignature(transaction, document, transactionId),
            () -> toDTO(document),
            documentId
        );
    }
    
    private Document fetchDocumentDetailsFromESignature(
            Transaction transaction,
            Document document,
            String transactionId) {
        String eSignatureDocumentId = document.getESignatureDocumentId() != null 
            ? document.getESignatureDocumentId() 
            : document.getId();
        
        Document fullDetails = eSignatureServiceClient.getDocumentDetails(
            transaction.getESignatureTransactionId(), 
            eSignatureDocumentId);
        
        fullDetails.setId(document.getId());
        fullDetails.setTransactionId(transactionId);
        return fullDetails;
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
    
    @Transactional
    public Document updateDocument(String transactionId, String documentId, Document dto) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ServiceException(ErrorCode.RESOURCE_NOT_FOUND, 
                "Document not found: " + documentId));
        
        ValidationHelper.validateResourceBelongsToTransaction(
            documentId,
            document.getTransaction().getId(),
            transactionId,
            "Document");
        
        prepareDocumentForUpdate(dto, document, transactionId);
        updateDocumentInESignature(transactionId, dto, documentId);
        updateDocumentMetadata(document, dto);
        
        Document saved = documentRepository.save(document);
        logger.info("Document metadata updated in DB: {}", documentId);
        return toDTO(saved);
    }
    
    private void prepareDocumentForUpdate(Document dto, Document document, String transactionId) {
        dto.setId(document.getId());
        dto.setTransactionId(transactionId);
        if (dto.getName() == null) {
            dto.setName(document.getName());
        }
        if (dto.getTitle() == null) {
            dto.setTitle(document.getTitle());
        }
    }
    
    private void updateDocumentInESignature(String transactionId, Document dto, String documentId) {
        ESignatureIntegrationHelper.executeVoidWithErrorHandling(
            "change document in eSignature service",
            () -> eSignatureServiceClient.changeDocuments(transactionId, dto),
            documentId
        );
    }
    
    private void updateDocumentMetadata(Document document, Document dto) {
        UpdateHelper.setIfNotNull(dto.getName(), document::setName);
        UpdateHelper.setIfNotNull(dto.getTitle(), document::setTitle);
    }
    
    private Document toDTO(Document document) {
        Document dto = new Document();
        dto.setId(document.getId());
        dto.setTransactionId(document.getTransaction().getId());
        dto.setName(document.getName());
        dto.setTitle(document.getTitle());
        dto.setESignatureDocumentId(document.getESignatureDocumentId());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setUpdatedAt(document.getUpdatedAt());
        
        // FormFields and ICMP objects are not stored in BFF DB
        // They will be fetched from ESignatureService when needed using getDocumentDetails()
        dto.setFormFields(null);
        dto.setIcmpObjects(null);
        
        return dto;
    }
    
    /**
     * Get form fields for a document from ESignatureService
     */
    public List<FormField> getFormFields(String transactionId, String documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ServiceException(ErrorCode.RESOURCE_NOT_FOUND, 
                "Document not found: " + documentId));
        
        ValidationHelper.validateResourceBelongsToTransaction(
            documentId,
            document.getTransaction().getId(),
            transactionId,
            "Document");
        
        Transaction transaction = document.getTransaction();
        if (transaction.getESignatureTransactionId() == null) {
            logger.warn("No eSignature transaction ID found for transaction: {}", transactionId);
            return new ArrayList<>();
        }
        
        String eSignatureDocumentId = document.getESignatureDocumentId() != null 
            ? document.getESignatureDocumentId() 
            : documentId;
        
        return ESignatureIntegrationHelper.executeWithErrorHandling(
            "get form fields from ESignatureService",
            () -> {
                List<FormField> formFields = eSignatureServiceClient.getFormFields(
                    transaction.getESignatureTransactionId(), 
                    eSignatureDocumentId);
                logger.info("Fetched {} form fields from ESignatureService for document: {}", 
                    formFields != null ? formFields.size() : 0, documentId);
                return formFields != null ? formFields : new ArrayList<>();
            },
            documentId
        );
    }
    
    /**
     * Update form fields for a document
     * Sends form fields to ESignatureService via changeDocuments
     */
    @Transactional
    public Document updateFormFields(String transactionId, String documentId, List<FormField> formFields) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new ServiceException(ErrorCode.RESOURCE_NOT_FOUND, 
                "Document not found: " + documentId));
        
        ValidationHelper.validateResourceBelongsToTransaction(
            documentId,
            document.getTransaction().getId(),
            transactionId,
            "Document");
        
        Document documentDTO = buildDocumentForFormFields(document, transactionId, formFields);
        
        ESignatureIntegrationHelper.executeVoidWithErrorHandling(
            "update form fields in eSignature service",
            () -> eSignatureServiceClient.changeDocuments(transactionId, documentDTO),
            documentId
        );
        
        return toDTO(document);
    }
    
    private Document buildDocumentForFormFields(
            Document document,
            String transactionId,
            List<FormField> formFields) {
        Document documentDTO = new Document();
        documentDTO.setId(document.getId());
        documentDTO.setTransactionId(transactionId);
        documentDTO.setName(document.getName());
        documentDTO.setTitle(document.getTitle());
        documentDTO.setFormFields(formFields);
        return documentDTO;
    }
}

