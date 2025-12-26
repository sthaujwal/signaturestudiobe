package com.wellsfargo.signaturestudio.service;

import com.wellsfargo.signaturestudio.client.ESignatureServiceClient;
import com.wellsfargo.signaturestudio.domain.AddUserRequest;
import com.wellsfargo.signaturestudio.domain.User;
import com.wellsfargo.signaturestudio.model.User;
import com.wellsfargo.signaturestudio.model.Transaction;
import com.wellsfargo.signaturestudio.repository.UserRepository;
import com.wellsfargo.signaturestudio.repository.TransactionRepository;
import com.wellsfargo.signaturestudio.exception.ErrorCode;
import com.wellsfargo.signaturestudio.exception.ServiceException;
import com.wellsfargo.signaturestudio.util.AddRequestValidator;
import com.wellsfargo.signaturestudio.util.ESignatureIntegrationHelper;
import com.wellsfargo.signaturestudio.util.UpdateHelper;
import com.wellsfargo.signaturestudio.util.ValidationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final ESignatureServiceClient eSignatureServiceClient;
    
    public UserService(
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            ESignatureServiceClient eSignatureServiceClient) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.eSignatureServiceClient = eSignatureServiceClient;
    }
    
    public List<User> getUsers(String transactionId) {
        List<User> users = userRepository.findByTransactionIdOrderBySigningOrderAsc(transactionId);
        return users.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public User addUser(String transactionId, AddUserRequest request) {
        // Validate the request
        AddRequestValidator.validate(request);
        
        Transaction transaction = findTransaction(transactionId);
        
        // Convert AddUserRequest to User for eSignature service
        User dto = toUser(request);
        
        addUserToESignature(transactionId, dto);
        User user = saveUserToDatabase(transaction, dto);
        logger.info("User saved to DB: {} for transaction: {}", user.getId(), transactionId);
        
        return toDTO(user);
    }
    
    /**
     * Converts AddUserRequest to User for internal use.
     * Note: externalIdType and authType are not part of User,
     * they are only used during validation.
     */
    private User toUser(AddUserRequest request) {
        User dto = new User();
        dto.setFirstName(request.getFirstName());
        dto.setLastName(request.getLastName());
        dto.setFullName(request.getFullName());
        dto.setName(request.getName());
        dto.setEmail(request.getEmail());
        dto.setPhoneNumber(request.getPhoneNumber());
        dto.setUniqueId(request.getUniqueId());
        dto.setExternalId(request.getExternalId());
        dto.setRole(request.getRole());
        dto.setSigningOrder(request.getSigningOrder());
        dto.setType(request.getType());
        dto.setUserCategory(request.getUserCategory());
        return dto;
    }
    
    private Transaction findTransaction(String transactionId) {
        return transactionRepository.findById(transactionId)
            .orElseThrow(() -> new ServiceException(ErrorCode.TRANSACTION_NOT_FOUND, 
                "Transaction ID: " + transactionId));
    }
    
    private void addUserToESignature(String transactionId, User dto) {
        List<User> usersToAdd = List.of(dto);
        ESignatureIntegrationHelper.executeVoidWithErrorHandling(
            "add user in eSignature service",
            () -> eSignatureServiceClient.addUsers(transactionId, usersToAdd),
            transactionId
        );
    }
    
    private User saveUserToDatabase(Transaction transaction, User dto) {
        User user = buildUserEntity(dto, transaction);
        return userRepository.save(user);
    }
    
    private User buildUserEntity(User dto, Transaction transaction) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setTransaction(transaction);
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setFullName(dto.getFullName());
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setUniqueId(dto.getUniqueId());
        user.setExternalId(dto.getExternalId());
        user.setRole(dto.getRole() != null ? dto.getRole() : "signer");
        user.setSigningOrder(dto.getSigningOrder());
        user.setType(dto.getType() != null ? dto.getType() : "Signer");
        user.setUserCategory(dto.getUserCategory() != null ? dto.getUserCategory() : "customer");
        return user;
    }
    
    @Transactional
    public User updateUser(String transactionId, String userId, User dto) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND, 
                "User ID: " + userId));
        
        ValidationHelper.validateResourceBelongsToTransaction(
            userId,
            user.getTransaction().getId(),
            transactionId,
            "User");
        
        dto.setId(userId);
        updateUserInESignature(transactionId, dto, userId);
        updateUserFields(user, dto);
        
        User saved = userRepository.save(user);
        logger.info("User updated in DB: {}", userId);
        return toDTO(saved);
    }
    
    private void updateUserInESignature(String transactionId, User dto, String userId) {
        List<User> usersToChange = List.of(dto);
        ESignatureIntegrationHelper.executeVoidWithErrorHandling(
            "change user in eSignature service",
            () -> eSignatureServiceClient.changeUsers(transactionId, usersToChange),
            userId
        );
    }
    
    private void updateUserFields(User user, User dto) {
        UpdateHelper.setIfNotNull(dto.getFirstName(), user::setFirstName);
        UpdateHelper.setIfNotNull(dto.getLastName(), user::setLastName);
        UpdateHelper.setIfNotNull(dto.getFullName(), user::setFullName);
        UpdateHelper.setIfNotNull(dto.getName(), user::setName);
        UpdateHelper.setIfNotNull(dto.getEmail(), user::setEmail);
        UpdateHelper.setIfNotNull(dto.getPhoneNumber(), user::setPhoneNumber);
        UpdateHelper.setIfNotNull(dto.getUniqueId(), user::setUniqueId);
        UpdateHelper.setIfNotNull(dto.getExternalId(), user::setExternalId);
        UpdateHelper.setIfNotNull(dto.getRole(), user::setRole);
        UpdateHelper.setIfNotNull(dto.getSigningOrder(), user::setSigningOrder);
        UpdateHelper.setIfNotNull(dto.getType(), user::setType);
        UpdateHelper.setIfNotNull(dto.getUserCategory(), user::setUserCategory);
    }
    
    @Transactional
    public void deleteUser(String transactionId, String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND, 
                "User ID: " + userId));
        
        ValidationHelper.validateResourceBelongsToTransaction(
            userId,
            user.getTransaction().getId(),
            transactionId,
            "User");
        
        deleteUserFromESignature(transactionId, userId);
        userRepository.delete(user);
        logger.info("User deleted from DB: {} from transaction: {}", userId, transactionId);
    }
    
    private void deleteUserFromESignature(String transactionId, String userId) {
        List<String> userIdsToDelete = List.of(userId);
        ESignatureIntegrationHelper.executeVoidWithErrorHandling(
            "delete user in eSignature service",
            () -> eSignatureServiceClient.deleteUsers(transactionId, userIdsToDelete),
            userId
        );
    }
    
    private User toDTO(User user) {
        User dto = new User();
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
}

