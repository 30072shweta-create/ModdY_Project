package com.example.flight.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenResponseDTO {

    private String accessToken;

    private String refreshToken;
}