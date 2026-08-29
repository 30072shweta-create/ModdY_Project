package com.example.flight.dto;

import java.time.LocalDateTime;

public class SearchLogResponseDTO {

    private Long searchId;
    private Long userId;
    private String source;
    private String destination;
    private LocalDateTime searchDate;

    public Long getSearchId() { return searchId; }
    public void setSearchId(Long searchId) { this.searchId = searchId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public LocalDateTime getSearchDate() { return searchDate; }
    public void setSearchDate(LocalDateTime searchDate) { this.searchDate = searchDate; }
}