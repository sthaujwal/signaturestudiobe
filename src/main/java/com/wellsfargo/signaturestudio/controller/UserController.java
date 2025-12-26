package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.domain.AddUserRequest;
import com.wellsfargo.signaturestudio.domain.User;
import com.wellsfargo.signaturestudio.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions/{transactionId}/users")
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping
    public ResponseEntity<List<User>> getUsers(@PathVariable String transactionId) {
        List<User> users = userService.getUsers(transactionId);
        return ResponseEntity.ok(users);
    }
    
    @PostMapping
    public ResponseEntity<User> addUser(
            @PathVariable String transactionId,
            @Valid @RequestBody AddUserRequest addUserRequest) {
        User created = userService.addUser(transactionId, addUserRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(
            @PathVariable String transactionId,
            @PathVariable String userId,
            @Valid @RequestBody User userDTO) {
        User updated = userService.updateUser(transactionId, userId, userDTO);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable String transactionId,
            @PathVariable String userId) {
        userService.deleteUser(transactionId, userId);
        return ResponseEntity.noContent().build();
    }
}

