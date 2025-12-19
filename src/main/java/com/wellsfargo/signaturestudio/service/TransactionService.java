package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.client.AlertServiceClient;
import com.wellsfargo.signaturestudio.client.ESignatureServiceClient;
import com.wellsfargo.signaturestudio.dto.AlertRequestDTO;
import com.wellsfargo.signaturestudio.dto.DocumentDTO;
import com.wellsfargo.signaturestudio.dto.PaginatedResponseDTO;
import com.wellsfargo.signaturestudio.dto.UserDTO;
import com.wellsfargo.signaturestudio.dto.TransactionDTO;
import com.wellsfargo.signaturestudio.exception.ErrorCode;
import com.wellsfargo.signaturestudio.exception.ServiceException;
import com.wellsfargo.signaturestudio.model.Document;
import com.wellsfargo.signaturestudio.model.User;
import com.wellsfargo.signaturestudio.model.Transaction;
import com.wellsfargo.signaturestudio.repository.DocumentRepository;
import com.wellsfargo.signaturestudio.repository.UserRepository;
import com.wellsfargo.signaturestudio.repository.TransactionRepository;
import com.wellsfargo.signaturestudio.util.ESignatureIntegrationHelper;
import com.wellsfargo.signaturestudio.util.QueryBuilderHelper;
import com.wellsfargo.signaturestudio.util.UpdateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final ESignatureServiceClient eSignatureServiceClient;
    private final AlertServiceClient alertServiceClient;
    private final com.wellsfargo.signaturestudio.service.DelegationService delegationService;
    
    public TransactionService(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            DocumentRepository documentRepository,
            ESignatureServiceClient eSignatureServiceClient,
            AlertServiceClient alertServiceClient,
            com.wellsfargo.signaturestudio.service.DelegationService delegationService) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.eSignatureServiceClient = eSignatureServiceClient;
        this.alertServiceClient = alertServiceClient;
        this.delegationService = delegationService;
    }
    
    @Transactional
    public TransactionDTO createTransaction(TransactionDTO dto, String createdBy, String creatorEmail) {
        logger.info("Creating transaction: {} by user: {} ({})", dto.getTitle(), createdBy, creatorEmail);
        
        TransactionDTO eSignatureResponse = createESignatureTransaction(dto);
        Transaction transaction = saveTransactionMetadata(dto, createdBy, creatorEmail, eSignatureResponse);
        
        processUsers(transaction, dto.getUsers());
        processDocuments(transaction, dto.getDocuments());
        sendInitialAlerts(transaction, dto);
        
        return toDTO(transaction);
    }
    
    private TransactionDTO createESignatureTransaction(TransactionDTO dto) {
        return ESignatureIntegrationHelper.executeWithErrorHandling(
            "create transaction in eSignature service",
            () -> {
                TransactionDTO response = eSignatureServiceClient.createTransaction(dto);
                if (response == null) {
                    throw new ServiceException(ErrorCode.ESIGNATURE_SERVICE_ERROR, 
                        "eSignature service returned null response");
                }
                return response;
            },
            dto.getId() != null ? dto.getId() : "new"
        );
    }
    
    private Transaction saveTransactionMetadata(
            TransactionDTO dto, 
            String createdBy, 
            String creatorEmail, 
            TransactionDTO eSignatureResponse) {
        Transaction transaction = buildTransactionEntity(dto, createdBy, creatorEmail, eSignatureResponse);
        Transaction saved = transactionRepository.save(transaction);
        logger.info("Transaction metadata saved to DB with ID: {}", saved.getId());
        return saved;
    }
    
    @NonNull
    private Transaction buildTransactionEntity(
            TransactionDTO dto, 
            String createdBy, 
            String creatorEmail, 
            TransactionDTO eSignatureResponse) {
        Transaction transaction = new Transaction();
        transaction.setId(dto.getId() != null ? dto.getId() : UUID.randomUUID().toString());
        transaction.setTitle(dto.getTitle());
        transaction.setDescription(dto.getDescription());
        transaction.setStatus("in-progress");
        transaction.setCreatedBy(createdBy);
        transaction.setCreatorUsername(createdBy);
        transaction.setCreatorEmail(creatorEmail != null ? creatorEmail : dto.getCreatorEmail());
        transaction.setAccountId(dto.getAccountId());
        transaction.setAccountCode(dto.getAccountCode());
        transaction.setDueDate(dto.getDueDate());
        transaction.setPriority(dto.getPriority());
        transaction.setEmailTemplate(dto.getEmailTemplate());
        transaction.setSystemOfRecord(dto.getSystemOfRecord());
        transaction.setFormType(dto.getFormType());
        transaction.setESignatureTransactionId(eSignatureResponse.getESignatureTransactionId());
        return transaction;
    }
    
    private void processUsers(Transaction transaction, List<UserDTO> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        
        // Note: When creating transactions, users come as UserDTO from TransactionDTO.
        // For individual user addition, use AddUserRequest which has stricter validation.
        // Here we validate basic requirements but not externalIdType/authType since
        // those are specific to AddUserRequest.
        
        ESignatureIntegrationHelper.executeVoidWithErrorHandling(
            "add users in eSignature service",
            () -> eSignatureServiceClient.addUsers(transaction.getId(), users),
            transaction.getId()
        );
        
        saveUsersToDatabase(transaction, users);
    }
    
    private void saveUsersToDatabase(Transaction transaction, List<UserDTO> userDTOs) {
        for (UserDTO userDTO : userDTOs) {
            User user = buildUserEntity(userDTO, transaction);
            userRepository.save(user);
        }
        logger.info("Users saved to DB for transaction: {}", transaction.getId());
    }
    
    @NonNull
    private User buildUserEntity(UserDTO userDTO, Transaction transaction) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setTransaction(transaction);
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setFullName(userDTO.getFullName());
        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setUniqueId(userDTO.getUniqueId());
        user.setExternalId(userDTO.getExternalId());
        user.setRole(userDTO.getRole() != null ? userDTO.getRole() : "signer");
        user.setSigningOrder(userDTO.getSigningOrder());
        user.setType(userDTO.getType() != null ? userDTO.getType() : "Signer");
        user.setUserCategory(userDTO.getUserCategory() != null ? userDTO.getUserCategory() : "customer");
        return user;
    }
    
    private void processDocuments(Transaction transaction, List<DocumentDTO> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        
        for (DocumentDTO documentDTO : documents) {
            processDocument(transaction, documentDTO);
        }
    }
    
    private void processDocument(Transaction transaction, DocumentDTO documentDTO) {
        try {
            DocumentDTO eSignatureDoc = eSignatureServiceClient.addDocumentSync(
                transaction.getId(), documentDTO);
            logger.info("Document added successfully in eSignature service: {}", documentDTO.getId());
            
            Document document = buildDocumentEntity(documentDTO, transaction, eSignatureDoc);
            documentRepository.save(document);
            logger.info("Document metadata saved to DB: {}", document.getId());
        } catch (Exception e) {
            logger.error("Failed to add document in eSignature service: {}", 
                documentDTO.getId(), e);
            // Continue with other documents - don't fail entire transaction
        }
    }
    
    @NonNull
    private Document buildDocumentEntity(
            DocumentDTO documentDTO, 
            Transaction transaction, 
            DocumentDTO eSignatureDoc) {
        Document document = new Document();
        document.setId(documentDTO.getId() != null ? 
            documentDTO.getId() : UUID.randomUUID().toString());
        document.setTransaction(transaction);
        document.setName(documentDTO.getName() != null ? 
            documentDTO.getName() : documentDTO.getOriginalFileName());
        document.setTitle(documentDTO.getTitle() != null ? 
            documentDTO.getTitle() : documentDTO.getName());
        document.setESignatureDocumentId(eSignatureDoc.getESignatureDocumentId());
        return document;
    }
    
    private void sendInitialAlerts(Transaction transaction, TransactionDTO dto) {
        if (dto.getUsers() == null || dto.getUsers().isEmpty()) {
            return;
        }
        
        try {
            for (UserDTO user : dto.getUsers()) {
                sendAlertToUser(transaction, dto, user);
            }
        } catch (Exception e) {
            logger.error("Failed to send initial email alert for transaction: {}", 
                transaction.getId(), e);
            // Don't throw - transaction is already created, alert can be retried
        }
    }
    
    private void sendAlertToUser(Transaction transaction, TransactionDTO dto, UserDTO user) {
        AlertRequestDTO alertRequest = new AlertRequestDTO();
        alertRequest.setTemplateId(dto.getEmailTemplate());
        alertRequest.setRecipientEmail(user.getEmail());
        alertRequest.setRecipientName(user.getFullName() != null ? 
            user.getFullName() : user.getName());
        alertRequest.setTransactionId(transaction.getId());
        alertServiceClient.sendAlert(alertRequest);
    }
    
    public List<TransactionDTO> getTransactions(String accountId, String createdBy) {
        List<Transaction> transactions;
        if (accountId != null && createdBy != null) {
            transactions = transactionRepository.findByCreatedByAndAccountId(createdBy, accountId);
        } else if (accountId != null) {
            transactions = transactionRepository.findByAccountId(accountId);
        } else if (createdBy != null) {
            transactions = transactionRepository.findByCreatedBy(createdBy);
        } else {
            transactions = transactionRepository.findAll();
        }
        
        return transactions.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get transactions considering delegations with pagination and search
     * Returns transactions created by the user OR transactions where user is a delegate
     * This is the main method to call after user login
     * 
     * @param accountId Optional account ID filter
     * @param userId User ID to get transactions for
     * @param searchText Optional search text to search in title and description
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortBy Field to sort by
     * @param sortDirection Sort direction (asc/desc)
     */
    public PaginatedResponseDTO<TransactionDTO> getTransactionsWithDelegations(
            String accountId, 
            String userId, 
            String searchText,
            int page, 
            int size, 
            String sortBy, 
            String sortDirection) {
        
        List<String> userIdsToQuery = buildUserIdsWithDelegations(userId, accountId);
        Pageable pageable = PageRequest.of(page, size, QueryBuilderHelper.createSort(sortBy, sortDirection));
        Page<Transaction> transactionPage = queryTransactionsWithDelegations(
            userIdsToQuery, accountId, searchText, pageable);
        
        return buildPaginatedResponse(transactionPage);
    }
    
    private List<String> buildUserIdsWithDelegations(String userId, String accountId) {
        List<String> userIdsToQuery = new ArrayList<>();
        userIdsToQuery.add(userId);
        
        List<com.wellsfargo.signaturestudio.dto.DelegationDTO> activeDelegations = 
            delegationService.getActiveDelegationsByDelegate(userId);
        
        for (com.wellsfargo.signaturestudio.dto.DelegationDTO delegation : activeDelegations) {
            if (isDelegationApplicable(delegation, accountId)) {
                userIdsToQuery.add(delegation.getDelegatorUserId());
            }
        }
        
        return userIdsToQuery.stream().distinct().collect(Collectors.toList());
    }
    
    private boolean isDelegationApplicable(
            com.wellsfargo.signaturestudio.dto.DelegationDTO delegation, 
            String accountId) {
        return accountId == null || 
               delegation.getAccountId() == null || 
               delegation.getAccountId().equals(accountId);
    }
    
    private Page<Transaction> queryTransactionsWithDelegations(
            List<String> userIdsToQuery,
            String accountId,
            String searchText,
            @NonNull Pageable pageable) {
        String normalizedSearch = QueryBuilderHelper.normalizeSearchText(searchText);
        
        if (normalizedSearch != null) {
            return accountId != null
                ? transactionRepository.findByCreatedByInAndAccountIdAndSearchText(
                    userIdsToQuery, accountId, normalizedSearch, pageable)
                : transactionRepository.findByCreatedByInAndSearchText(
                    userIdsToQuery, normalizedSearch, pageable);
        }
        
        return accountId != null
            ? transactionRepository.findByCreatedByInAndAccountId(userIdsToQuery, accountId, pageable)
            : transactionRepository.findByCreatedByIn(userIdsToQuery, pageable);
    }
    
    private PaginatedResponseDTO<TransactionDTO> buildPaginatedResponse(Page<Transaction> transactionPage) {
        List<TransactionDTO> transactionDTOs = transactionPage.getContent().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        
        return new PaginatedResponseDTO<>(
            transactionDTOs,
            transactionPage.getNumber(),
            transactionPage.getSize(),
            transactionPage.getTotalElements()
        );
    }
    
    /**
     * Get transactions with pagination and search (without delegation support)
     */
    public PaginatedResponseDTO<TransactionDTO> getTransactionsPaginated(
            String accountId, 
            String createdBy, 
            String searchText,
            int page, 
            int size, 
            String sortBy, 
            String sortDirection) {
        
        Pageable pageable = PageRequest.of(page, size, 
            QueryBuilderHelper.createSort(sortBy, sortDirection));
        Page<Transaction> transactionPage = queryTransactions(
            accountId, createdBy, searchText, pageable);
        
        return buildPaginatedResponse(transactionPage);
    }
    
    private Page<Transaction> queryTransactions(
            String accountId,
            String createdBy,
            String searchText,
            @NonNull Pageable pageable) {
        String normalizedSearch = QueryBuilderHelper.normalizeSearchText(searchText);
        
        if (normalizedSearch != null) {
            return queryWithSearch(accountId, createdBy, normalizedSearch, pageable);
        }
        
        return queryWithoutSearch(accountId, createdBy, pageable);
    }
    
    private Page<Transaction> queryWithSearch(
            String accountId,
            String createdBy,
            String searchText,
            @NonNull Pageable pageable) {
        if (accountId != null && createdBy != null) {
            return transactionRepository.findByCreatedByAndAccountIdAndSearchText(
                createdBy, accountId, searchText, pageable);
        }
        if (createdBy != null) {
            return transactionRepository.findByCreatedByAndSearchText(createdBy, searchText, pageable);
        }
        // Fallback to non-search query if no filters
        return transactionRepository.findAll(pageable);
    }
    
    private Page<Transaction> queryWithoutSearch(
            String accountId,
            String createdBy,
            @NonNull Pageable pageable) {
        if (accountId != null && createdBy != null) {
            return transactionRepository.findByCreatedByAndAccountId(createdBy, accountId, pageable);
        }
        if (accountId != null) {
            return transactionRepository.findByAccountId(accountId, pageable);
        }
        if (createdBy != null) {
            return transactionRepository.findByCreatedBy(createdBy, pageable);
        }
        return transactionRepository.findAll(pageable);
    }
    
    public TransactionDTO getTransaction(String id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.TRANSACTION_NOT_FOUND, "Transaction ID: " + id));
        return toDTO(transaction);
    }
    
    /**
     * Get full transaction details from ESignatureService
     * Fetches documents, form fields, attributes, and ICMP objects
     */
    public TransactionDTO getTransactionDetails(String id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.TRANSACTION_NOT_FOUND, "Transaction ID: " + id));
        
        if (transaction.getESignatureTransactionId() == null) {
            // If no eSignature transaction ID, return basic info
            return toDTO(transaction);
        }
        
        try {
            // Fetch full details from ESignatureService
            TransactionDTO fullDetails = eSignatureServiceClient.getTransactionDetails(transaction.getESignatureTransactionId());
            // Merge with local metadata
            fullDetails.setId(transaction.getId());
            fullDetails.setCreatedBy(transaction.getCreatedBy());
            fullDetails.setCreatorUsername(transaction.getCreatorUsername());
            fullDetails.setCreatorEmail(transaction.getCreatorEmail());
            fullDetails.setAccountId(transaction.getAccountId());
            fullDetails.setAccountCode(transaction.getAccountCode());
            return fullDetails;
        } catch (Exception e) {
            logger.warn("Failed to fetch full transaction details from ESignatureService, returning basic info", e);
            return toDTO(transaction);
        }
    }
    
    @Transactional
    public TransactionDTO updateTransaction(String id, TransactionDTO dto) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.TRANSACTION_NOT_FOUND, "Transaction ID: " + id));
        
        // Update DTO with transaction ID and existing data
        dto.setId(id);
        if (dto.getESignatureTransactionId() == null) {
            dto.setESignatureTransactionId(transaction.getESignatureTransactionId());
        }
        
        // Step 1: Call eSignature service to change transaction
        try {
            eSignatureServiceClient.changeTransaction(dto);
            logger.info("Transaction changed successfully in eSignature service: {}", id);
        } catch (Exception e) {
            logger.error("Failed to change transaction in eSignature service: {}", id, e);
            throw new ServiceException(ErrorCode.ESIGNATURE_SERVICE_ERROR, 
                "Failed to change transaction in eSignature service", e);
        }
        
        // Step 2: Update transaction in DB (only after successful eSignature call)
        UpdateHelper.setIfNotNull(dto.getTitle(), transaction::setTitle);
        UpdateHelper.setIfNotNull(dto.getDescription(), transaction::setDescription);
        UpdateHelper.ifNotNull(dto.getStatus(), status -> {
            transaction.setStatus(status);
            logger.info("Transaction status changed to: {} for ID: {}", status, id);
        });
        UpdateHelper.setIfNotNull(dto.getDueDate(), transaction::setDueDate);
        UpdateHelper.setIfNotNull(dto.getPriority(), transaction::setPriority);
        UpdateHelper.setIfNotNull(dto.getFormType(), transaction::setFormType);
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        logger.info("Transaction updated in DB: {}", id);
        
        return toDTO(savedTransaction);
    }
    
    @Transactional
    public void deleteTransaction(String id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.TRANSACTION_NOT_FOUND, "Transaction ID: " + id));
        
        // Step 1: Call eSignature service to cancel transaction
        try {
            eSignatureServiceClient.cancelTransaction(id);
            logger.info("Transaction cancelled successfully in eSignature service: {}", id);
        } catch (Exception e) {
            logger.error("Failed to cancel transaction in eSignature service: {}", id, e);
            throw new ServiceException(ErrorCode.ESIGNATURE_SERVICE_ERROR, 
                "Failed to cancel transaction in eSignature service", e);
        }
        
        // Step 2: Delete transaction from DB (only after successful eSignature call)
        transactionRepository.deleteById(id);
        logger.info("Transaction deleted from DB: {}", id);
    }
    
    public Map<String, Object> getTransactionStatus(String id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.TRANSACTION_NOT_FOUND, "Transaction ID: " + id));
        
        // Try to get latest status from eSignature service
        if (transaction.getESignatureTransactionId() != null) {
            try {
                return eSignatureServiceClient.getTransactionStatus(transaction.getESignatureTransactionId());
            } catch (Exception e) {
                logger.warn("Failed to get status from eSignature service, returning local status", e);
            }
        }
        
        // Return local status
        Map<String, Object> status = new HashMap<>();
        status.put("status", transaction.getStatus());
        status.put("transactionId", transaction.getId());
        return status;
    }
    
    private TransactionDTO toDTO(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(transaction.getId());
        dto.setTitle(transaction.getTitle());
        dto.setDescription(transaction.getDescription());
        dto.setStatus(transaction.getStatus());
        dto.setCreatedBy(transaction.getCreatedBy()); // Keep for backward compatibility
        dto.setCreatorUsername(transaction.getCreatorUsername());
        dto.setCreatorEmail(transaction.getCreatorEmail());
        dto.setAccountId(transaction.getAccountId());
        dto.setAccountCode(transaction.getAccountCode());
        dto.setESignatureTransactionId(transaction.getESignatureTransactionId());
        dto.setDocumentUrl(transaction.getDocumentUrl());
        dto.setDueDate(transaction.getDueDate());
        dto.setPriority(transaction.getPriority());
        dto.setEmailTemplate(transaction.getEmailTemplate());
        dto.setSystemOfRecord(transaction.getSystemOfRecord());
        dto.setFormType(transaction.getFormType());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setUpdatedAt(transaction.getUpdatedAt());
        
        // Load users
        List<User> users = userRepository.findByTransactionId(transaction.getId());
        dto.setUsers(users.stream()
                .map(this::userToDTO)
                .collect(Collectors.toList()));
        
        // Load basic document metadata (form fields and ICMP will be fetched from ESignatureService when needed)
        List<Document> documents = documentRepository.findByTransactionId(transaction.getId());
        dto.setDocuments(documents.stream()
                .map(this::documentToDTO)
                .collect(Collectors.toList()));
        
        // Custom attributes are not stored in BFF DB - they will be fetched from ESignatureService when needed
        // Set to null or empty map - can be populated by calling getTransactionDetails() if needed
        dto.setCustomAttributes(null);
        
        return dto;
    }
    
    private UserDTO userToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setFullName(user.getFullName());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setUniqueId(user.getUniqueId());
        dto.setExternalId(user.getExternalId());
        dto.setRole(user.getRole());
        dto.setSigningOrder(user.getSigningOrder());
        dto.setType(user.getType());
        dto.setUserCategory(user.getUserCategory());
        return dto;
    }
    
    private DocumentDTO documentToDTO(Document document) {
        DocumentDTO dto = new DocumentDTO();
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
}

