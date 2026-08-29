package com.example.flight.repository;

import com.example.flight.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByUserUserId(Long userId);

    List<Notification> findByBookingBookingId(Long bookingId);

    List<Notification> findByStatus(String status);
}