package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.constants.SessionConstants;

import com.wellsfargo.signaturestudio.domain.Document;
import com.wellsfargo.signaturestudio.domain.FormField;
import com.wellsfargo.signaturestudio.service.DocumentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/transactions/{transactionId}/documents")
public class DocumentController {
    
    private final DocumentService documentService;
    
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }
    
    @GetMapping
    public ResponseEntity<List<Document>> getDocuments(@PathVariable String transactionId) {
        List<Document> documents = documentService.getDocuments(transactionId);
        return ResponseEntity.ok(documents);
    }
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Document> uploadDocument(
            @PathVariable String transactionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "formFields", required = false) String formFieldsJson,
            HttpSession session) {
        String uploadedBy = (String) session.getAttribute(SessionConstants.USERNAME);
        if (uploadedBy == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Document document = documentService.uploadDocument(transactionId, file, uploadedBy, formFieldsJson);
        return ResponseEntity.status(HttpStatus.CREATED).body(document);
    }
    
    @PutMapping("/{documentId}")
    public ResponseEntity<Document> updateDocument(
            @PathVariable String transactionId,
            @PathVariable String documentId,
            @RequestBody Document documentDTO) {
        Document updated = documentService.updateDocument(transactionId, documentId, documentDTO);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable String transactionId,
            @PathVariable String documentId) {
        documentService.deleteDocument(transactionId, documentId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{documentId}")
    public ResponseEntity<Document> getDocument(
            @PathVariable String transactionId,
            @PathVariable String documentId) {
        Document document = documentService.getDocumentDetails(transactionId, documentId);
        return ResponseEntity.ok(document);
    }
    
    /**
     * Get full document details from ESignatureService
     * Includes form fields and ICMP objects
     */
    @GetMapping("/{documentId}/details")
    public ResponseEntity<Document> getDocumentDetails(
            @PathVariable String transactionId,
            @PathVariable String documentId) {
        Document document = documentService.getDocumentDetails(transactionId, documentId);
        return ResponseEntity.ok(document);
    }
    
    /**
     * Download document - files are stored in ESignatureService
     * This endpoint should redirect to ESignatureService or return the storage URL
     * Note: Document files are managed by ESignatureService, not stored in BFF
     */
    @GetMapping("/{documentId}/download")
    public ResponseEntity<Void> downloadDocument(@PathVariable String documentId) {
        // Document files are stored in ESignatureService
        // This should redirect to ESignatureService download URL or fetch from there
        // For now, return 501 Not Implemented
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
    
    /**
     * Get form fields for a document
     * Fetches form fields from ESignatureService
     */
    @GetMapping("/{documentId}/form-fields")
    public ResponseEntity<List<FormField>> getFormFields(
            @PathVariable String transactionId,
            @PathVariable String documentId) {
        List<FormField> formFields = documentService.getFormFields(transactionId, documentId);
        return ResponseEntity.ok(formFields);
    }
    
    /**
     * Update form fields for a document
     * Sends form fields to ESignatureService via changeDocuments
     */
    @PutMapping("/{documentId}/form-fields")
    public ResponseEntity<Document> updateFormFields(
            @PathVariable String transactionId,
            @PathVariable String documentId,
            @RequestBody List<FormField> formFields) {
        Document updated = documentService.updateFormFields(transactionId, documentId, formFields);
        return ResponseEntity.ok(updated);
    }
}

