package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.dto.DocumentDTO;
import com.wellsfargo.signaturestudio.dto.FormFieldDTO;
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
    public ResponseEntity<List<DocumentDTO>> getDocuments(@PathVariable String transactionId) {
        List<DocumentDTO> documents = documentService.getDocuments(transactionId);
        return ResponseEntity.ok(documents);
    }
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDTO> uploadDocument(
            @PathVariable String transactionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "formFields", required = false) String formFieldsJson,
            HttpSession session) {
        String uploadedBy = (String) session.getAttribute("username");
        if (uploadedBy == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        DocumentDTO document = documentService.uploadDocument(transactionId, file, uploadedBy, formFieldsJson);
        return ResponseEntity.status(HttpStatus.CREATED).body(document);
    }
    
    @PutMapping("/{documentId}")
    public ResponseEntity<DocumentDTO> updateDocument(
            @PathVariable String transactionId,
            @PathVariable String documentId,
            @RequestBody DocumentDTO documentDTO) {
        DocumentDTO updated = documentService.updateDocument(transactionId, documentId, documentDTO);
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
    public ResponseEntity<DocumentDTO> getDocument(
            @PathVariable String transactionId,
            @PathVariable String documentId) {
        DocumentDTO document = documentService.getDocumentDetails(transactionId, documentId);
        return ResponseEntity.ok(document);
    }
    
    /**
     * Get full document details from ESignatureService
     * Includes form fields and ICMP objects
     */
    @GetMapping("/{documentId}/details")
    public ResponseEntity<DocumentDTO> getDocumentDetails(
            @PathVariable String transactionId,
            @PathVariable String documentId) {
        DocumentDTO document = documentService.getDocumentDetails(transactionId, documentId);
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
    public ResponseEntity<List<FormFieldDTO>> getFormFields(
            @PathVariable String transactionId,
            @PathVariable String documentId) {
        List<FormFieldDTO> formFields = documentService.getFormFields(transactionId, documentId);
        return ResponseEntity.ok(formFields);
    }
    
    /**
     * Update form fields for a document
     * Sends form fields to ESignatureService via changeDocuments
     */
    @PutMapping("/{documentId}/form-fields")
    public ResponseEntity<DocumentDTO> updateFormFields(
            @PathVariable String transactionId,
            @PathVariable String documentId,
            @RequestBody List<FormFieldDTO> formFields) {
        DocumentDTO updated = documentService.updateFormFields(transactionId, documentId, formFields);
        return ResponseEntity.ok(updated);
    }
}

