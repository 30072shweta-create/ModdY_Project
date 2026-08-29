package com.example.flight.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.flight.dto.*;
import com.example.flight.entity.User;
import com.example.flight.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public UserResponseDTO getProfile(String email) {

        User user = getUser(email);

        return mapToResponse(user);
    }

    public UserResponseDTO updateProfile(
            String email,
            UpdateProfileRequestDTO request) {

        User user = getUser(email);

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        userRepository.save(user);

        return mapToResponse(user);
    }

    public String changePassword(
            String email,
            ChangePasswordRequestDTO request) {

        User user = getUser(email);

        if (!passwordEncoder.matches(
                request.getOldPassword(),
                user.getPasswordHash())) {

            throw new RuntimeException(
                    "Old password is incorrect"
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        userRepository.save(user);

        return "Password changed successfully";
    }

    public java.util.List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    public UserResponseDTO getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return mapToResponse(user);
    }

    public UserResponseDTO updateUserRole(Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setRole(role);
        userRepository.save(user);
        return mapToResponse(user);
    }

    public UserResponseDTO updateUserStatus(Long userId, boolean emailVerified) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setEmailVerified(emailVerified);
        userRepository.save(user);
        return mapToResponse(user);
    }

    private User getUser(String email) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        ));
    }

    private UserResponseDTO mapToResponse(User user) {

        return UserResponseDTO.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .build();
    }
}