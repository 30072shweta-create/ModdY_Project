package com.example.flight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class NotificationRequestDTO {

    @NotNull(message = "User ID is required")
    private Long userId;

    private Long bookingId;

    @NotBlank(message = "Notification type is required")
    @Size(max = 50)
    private String type;

    @NotBlank(message = "Notification channel is required")
    @Size(max = 50)
    private String channel;

    @NotBlank(message = "Notification message is required")
    private String message;

    @Size(max = 50)
    private String status;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}