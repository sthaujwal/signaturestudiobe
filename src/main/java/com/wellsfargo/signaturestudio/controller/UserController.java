package com.wellsfargo.signaturestudio.controller;

import com.wellsfargo.signaturestudio.dto.UserDTO;
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
    public ResponseEntity<List<UserDTO>> getUsers(@PathVariable String transactionId) {
        List<UserDTO> users = userService.getUsers(transactionId);
        return ResponseEntity.ok(users);
    }
    
    @PostMapping
    public ResponseEntity<UserDTO> addUser(
            @PathVariable String transactionId,
            @Valid @RequestBody UserDTO userDTO) {
        UserDTO created = userService.addUser(transactionId, userDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{userId}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable String transactionId,
            @PathVariable String userId,
            @Valid @RequestBody UserDTO userDTO) {
        UserDTO updated = userService.updateUser(transactionId, userId, userDTO);
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

