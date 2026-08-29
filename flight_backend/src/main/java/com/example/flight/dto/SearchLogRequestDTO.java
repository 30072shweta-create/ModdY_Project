package com.example.flight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SearchLogRequestDTO {

    private Long userId;

    @NotBlank(message = "Source is required")
    @Size(max = 100, message = "Source cannot exceed 100 characters")
    private String source;

    @NotBlank(message = "Destination is required")
    @Size(max = 100, message = "Destination cannot exceed 100 characters")
    private String destination;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
}