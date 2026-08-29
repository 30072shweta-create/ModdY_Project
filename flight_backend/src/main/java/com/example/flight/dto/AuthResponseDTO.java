package com.example.flight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private long expiresIn; // Expiration in seconds (e.g. 900 for 15 minutes)

    private UserResponseDTO user;
}
