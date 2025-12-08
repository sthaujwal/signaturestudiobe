package com.wellsfargo.signaturestudio.client;

import com.wellsfargo.signaturestudio.dto.DocumentDTO;
import com.wellsfargo.signaturestudio.dto.FormFieldDTO;
import com.wellsfargo.signaturestudio.dto.ICMPDTO;
import com.wellsfargo.signaturestudio.dto.TransactionDTO;
import com.wellsfargo.signaturestudio.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Component
public class ESignatureServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(ESignatureServiceClient.class);
    
    private final RestTemplate restTemplate;
    private final String baseUrl;
    
    public ESignatureServiceClient(
            RestTemplate restTemplate,
            @Value("${esignature.service.url:http://localhost:8081}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }
    
    /**
     * Create transaction in eSignature service
     * POST /createTransaction/V1
     */
    public TransactionDTO createTransaction(TransactionDTO transactionDTO) {
        try {
            String url = baseUrl + "/createTransaction/V1";
            logger.info("Calling eSignature service to create transaction: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<TransactionDTO> request = new HttpEntity<>(transactionDTO, headers);
            
            ResponseEntity<TransactionDTO> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, TransactionDTO.class);
            
            logger.info("Transaction created successfully in eSignature service with ID: {}", 
                    response.getBody() != null ? response.getBody().getId() : "unknown");
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error calling eSignature service to create transaction", e);
            throw new RuntimeException("Failed to create transaction in eSignature service", e);
        }
    }
    
    /**
     * Update/Change transaction in eSignature service
     * POST /changeTransaction/V1
     */
    public TransactionDTO changeTransaction(TransactionDTO transactionDTO) {
        try {
            String url = baseUrl + "/changeTransaction/V1";
            logger.info("Calling eSignature service to change transaction: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<TransactionDTO> request = new HttpEntity<>(transactionDTO, headers);
            
            ResponseEntity<TransactionDTO> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, TransactionDTO.class);
            
            logger.info("Transaction changed successfully in eSignature service: {}", transactionDTO.getId());
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error calling eSignature service to change transaction: {}", transactionDTO.getId(), e);
            throw new RuntimeException("Failed to change transaction in eSignature service", e);
        }
    }
    
    /**
     * Cancel transaction in eSignature service
     * POST /cancelTransaction/V1
     */
    public void cancelTransaction(String transactionId) {
        try {
            String url = baseUrl + "/cancelTransaction/V1";
            logger.info("Calling eSignature service to cancel transaction: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            Map<String, String> requestBody = Map.of("transactionId", transactionId);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            logger.info("Transaction cancelled successfully in eSignature service: {}", transactionId);
        } catch (RestClientException e) {
            logger.error("Error calling eSignature service to cancel transaction: {}", transactionId, e);
            throw new RuntimeException("Failed to cancel transaction in eSignature service", e);
        }
    }
    
    /**
     * Add users to transaction in eSignature service
     * POST /addUsers/V1
     */
    public void addUsers(String transactionId, List<UserDTO> users) {
        try {
            String url = baseUrl + "/addUsers/V1";
            logger.info("Calling eSignature service to add users: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            Map<String, Object> requestBody = Map.of(
                "transactionId", transactionId,
                "users", users
            );
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            logger.info("Users added successfully in eSignature service for transaction: {}", transactionId);
        } catch (RestClientException e) {
            logger.error("Error calling eSignature service to add users for transaction: {}", transactionId, e);
            throw new RuntimeException("Failed to add users in eSignature service", e);
        }
    }
    
    /**
     * Update/Change users in eSignature service
     * POST /changeUsers/V1
     */
    public void changeUsers(String transactionId, List<UserDTO> users) {
        try {
            String url = baseUrl + "/changeUsers/V1";
            logger.info("Calling eSignature service to change users: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            Map<String, Object> requestBody = Map.of(
                "transactionId", transactionId,
                "users", users
            );
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            logger.info("Users changed successfully in eSignature service for transaction: {}", transactionId);
        } catch (RestClientException e) {
            logger.error("Error calling eSignature service to change users for transaction: {}", transactionId, e);
            throw new RuntimeException("Failed to change users in eSignature service", e);
        }
    }
    
    /**
     * Delete users from transaction in eSignature service
     * POST /deleteUsers/V1
     */
    public void deleteUsers(String transactionId, List<String> userIds) {
        try {
            String url = baseUrl + "/deleteUsers/V1";
            logger.info("Calling eSignature service to delete users: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            Map<String, Object> requestBody = Map.of(
                "transactionId", transactionId,
                "userIds", userIds
            );
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            logger.info("Users deleted successfully in eSignature service for transaction: {}", transactionId);
        } catch (RestClientException e) {
            logger.error("Error calling eSignature service to delete users for transaction: {}", transactionId, e);
            throw new RuntimeException("Failed to delete users in eSignature service", e);
        }
    }
    
    /**
     * Add document to transaction in eSignature service
     * POST /addDocumentSync/V1
     */
    public DocumentDTO addDocumentSync(String transactionId, DocumentDTO document) {
        try {
            String url = baseUrl + "/addDocumentSync/V1";
            logger.info("Calling eSignature service to add document: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            Map<String, Object> requestBody = Map.of(
                "transactionId", transactionId,
                "document", document
            );
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<DocumentDTO> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, DocumentDTO.class);
            
            logger.info("Document added successfully in eSignature service: {}", document.getId());
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error calling eSignature service to add document: {}", document.getId(), e);
            throw new RuntimeException("Failed to add document in eSignature service", e);
        }
    }
    
    /**
     * Update/Change document in eSignature service
     * POST /changeDocuments/V1
     */
    public DocumentDTO changeDocuments(String transactionId, DocumentDTO document) {
        try {
            String url = baseUrl + "/changeDocuments/V1";
            logger.info("Calling eSignature service to change document: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            Map<String, Object> requestBody = Map.of(
                "transactionId", transactionId,
                "document", document
            );
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<DocumentDTO> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, DocumentDTO.class);
            
            logger.info("Document changed successfully in eSignature service: {}", document.getId());
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error calling eSignature service to change document: {}", document.getId(), e);
            throw new RuntimeException("Failed to change document in eSignature service", e);
        }
    }
    
    /**
     * Delete documents from transaction in eSignature service
     * POST /deleteDocuments/V1
     */
    public void deleteDocuments(String transactionId, List<String> documentIds) {
        try {
            String url = baseUrl + "/deleteDocuments/V1";
            logger.info("Calling eSignature service to delete documents: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            Map<String, Object> requestBody = Map.of(
                "transactionId", transactionId,
                "documentIds", documentIds
            );
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            logger.info("Documents deleted successfully in eSignature service for transaction: {}", transactionId);
        } catch (RestClientException e) {
            logger.error("Error calling eSignature service to delete documents for transaction: {}", transactionId, e);
            throw new RuntimeException("Failed to delete documents in eSignature service", e);
        }
    }
    
    /**
     * Get transaction status from eSignature service
     * POST /getTransactionStatus/V1 (assuming this is also POST)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getTransactionStatus(String transactionId) {
        try {
            String url = baseUrl + "/getTransactionStatus/V1";
            logger.info("Calling eSignature service to get transaction status: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            Map<String, String> requestBody = Map.of("transactionId", transactionId);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, 
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error calling eSignature service to get transaction status: {}", transactionId, e);
            throw new RuntimeException("Failed to get transaction status from eSignature service", e);
        }
    }
    
    /**
     * Get full transaction details from eSignature service
     * POST /getTransactionDetails/V1
     * Returns full TransactionDTO with documents, form fields, attributes, ICMP
     */
    public TransactionDTO getTransactionDetails(String eSignatureTransactionId) {
        try {
            String url = baseUrl + "/getTransactionDetails/V1";
            logger.info("Calling eSignature service to get transaction details: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            Map<String, String> requestBody = Map.of("eSignatureTransactionId", eSignatureTransactionId);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<TransactionDTO> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, TransactionDTO.class);
            
            logger.info("Transaction details fetched successfully from eSignature service: {}", eSignatureTransactionId);
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error calling eSignature service to get transaction details: {}", eSignatureTransactionId, e);
            throw new RuntimeException("Failed to get transaction details from eSignature service", e);
        }
    }
    
    /**
     * Get full document details from eSignature service
     * POST /getDocumentDetails/V1
     * Returns DocumentDTO with form fields and ICMP
     */
    public DocumentDTO getDocumentDetails(String eSignatureTransactionId, String documentId) {
        try {
            String url = baseUrl + "/getDocumentDetails/V1";
            logger.info("Calling eSignature service to get document details: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            Map<String, String> requestBody = Map.of(
                "eSignatureTransactionId", eSignatureTransactionId,
                "documentId", documentId
            );
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<DocumentDTO> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, DocumentDTO.class);
            
            logger.info("Document details fetched successfully from eSignature service: {}", documentId);
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error calling eSignature service to get document details: {}", documentId, e);
            throw new RuntimeException("Failed to get document details from eSignature service", e);
        }
    }
    
    /**
     * Get form fields for a document from eSignature service
     * POST /getFormFields/V1
     * Returns List<FormFieldDTO>
     */
    public List<FormFieldDTO> getFormFields(String eSignatureTransactionId, String documentId) {
        try {
            String url = baseUrl + "/getFormFields/V1";
            logger.info("Calling eSignature service to get form fields: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            Map<String, String> requestBody = Map.of(
                "eSignatureTransactionId", eSignatureTransactionId,
                "documentId", documentId
            );
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<List<FormFieldDTO>> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, 
                    new ParameterizedTypeReference<List<FormFieldDTO>>() {});
            
            logger.info("Form fields fetched successfully from eSignature service for document: {}", documentId);
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error calling eSignature service to get form fields: {}", documentId, e);
            throw new RuntimeException("Failed to get form fields from eSignature service", e);
        }
    }
    
    /**
     * Get transaction attributes from eSignature service
     * POST /getTransactionAttributes/V1
     * Returns Map<String, String>
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getTransactionAttributes(String eSignatureTransactionId) {
        try {
            String url = baseUrl + "/getTransactionAttributes/V1";
            logger.info("Calling eSignature service to get transaction attributes: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            Map<String, String> requestBody = Map.of("eSignatureTransactionId", eSignatureTransactionId);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, 
                    new ParameterizedTypeReference<Map<String, String>>() {});
            
            logger.info("Transaction attributes fetched successfully from eSignature service: {}", eSignatureTransactionId);
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error calling eSignature service to get transaction attributes: {}", eSignatureTransactionId, e);
            throw new RuntimeException("Failed to get transaction attributes from eSignature service", e);
        }
    }
    
    /**
     * Get ICMP objects for a document from eSignature service
     * POST /getICMPObjects/V1
     * Returns List<ICMPDTO>
     */
    public List<ICMPDTO> getICMPObjects(String eSignatureTransactionId, String documentId) {
        try {
            String url = baseUrl + "/getICMPObjects/V1";
            logger.info("Calling eSignature service to get ICMP objects: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            Map<String, String> requestBody = Map.of(
                "eSignatureTransactionId", eSignatureTransactionId,
                "documentId", documentId
            );
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<List<ICMPDTO>> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, 
                    new ParameterizedTypeReference<List<ICMPDTO>>() {});
            
            logger.info("ICMP objects fetched successfully from eSignature service for document: {}", documentId);
            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error calling eSignature service to get ICMP objects: {}", documentId, e);
            throw new RuntimeException("Failed to get ICMP objects from eSignature service", e);
        }
    }
}
