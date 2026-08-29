package com.example.flight.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.flight.dto.*;
import com.example.flight.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponseDTO> getProfile(
            Authentication authentication) {

        return ResponseEntity.ok(
                userService.getProfile(
                        authentication.getName()
                )
        );
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponseDTO> updateProfile(
            Authentication authentication,
            @Valid @RequestBody
            UpdateProfileRequestDTO request) {

        return ResponseEntity.ok(
                userService.updateProfile(
                        authentication.getName(),
                        request
                )
        );
    }

    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            Authentication authentication,
            @Valid @RequestBody
            ChangePasswordRequestDTO request) {

        return ResponseEntity.ok(
                userService.changePassword(
                        authentication.getName(),
                        request
                )
        );
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{userId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PutMapping("/{userId}/role")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> updateUserRole(
            @PathVariable Long userId,
            @RequestParam String role) {
        return ResponseEntity.ok(userService.updateUserRole(userId, role));
    }

    @PutMapping("/{userId}/status")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam boolean emailVerified) {
        return ResponseEntity.ok(userService.updateUserStatus(userId, emailVerified));
    }
}