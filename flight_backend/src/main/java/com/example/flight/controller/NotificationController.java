package com.example.flight.controller;

import com.example.flight.dto.NotificationRequestDTO;
import com.example.flight.dto.NotificationResponseDTO;
import com.example.flight.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // Get all notifications
    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>>
    getAllNotifications() {

        return ResponseEntity.ok(
                notificationService.getAllNotifications()
        );
    }

    // Get notification by ID
    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResponseDTO>
    getNotificationById(
            @PathVariable Long notificationId) {

        return ResponseEntity.ok(
                notificationService.getNotificationById(
                        notificationId
                )
        );
    }

    // Get notifications by user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponseDTO>>
    getNotificationsByUser(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                notificationService.getNotificationsByUser(
                        userId
                )
        );
    }

    // Get notifications by booking
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<NotificationResponseDTO>>
    getNotificationsByBooking(
            @PathVariable Long bookingId) {

        return ResponseEntity.ok(
                notificationService.getNotificationsByBooking(
                        bookingId
                )
        );
    }

    // Get notifications by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<NotificationResponseDTO>>
    getNotificationsByStatus(
            @PathVariable String status) {

        return ResponseEntity.ok(
                notificationService.getNotificationsByStatus(
                        status
                )
        );
    }

    // Create notification
    @PostMapping
    public ResponseEntity<NotificationResponseDTO>
    createNotification(
            @Valid @RequestBody NotificationRequestDTO dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        notificationService.createNotification(dto)
                );
    }

    // Update notification
    @PutMapping("/{notificationId}")
    public ResponseEntity<NotificationResponseDTO>
    updateNotification(
            @PathVariable Long notificationId,
            @Valid @RequestBody NotificationRequestDTO dto) {

        return ResponseEntity.ok(
                notificationService.updateNotification(
                        notificationId,
                        dto
                )
        );
    }

    // Delete notification
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<String>
    deleteNotification(
            @PathVariable Long notificationId) {

        notificationService.deleteNotification(
                notificationId
        );

        return ResponseEntity.ok(
                "Notification deleted successfully"
        );
    }
}