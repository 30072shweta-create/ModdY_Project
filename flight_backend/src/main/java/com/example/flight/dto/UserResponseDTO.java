package com.example.flight.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    private Long userId;

    private String email;

    private String firstName;

    private String lastName;

    private String role;

    private boolean emailVerified;
}