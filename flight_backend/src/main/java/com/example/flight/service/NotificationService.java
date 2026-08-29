package com.example.flight.service;

import java.time.LocalDateTime;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.example.flight.dto.NotificationRequestDTO;
import com.example.flight.dto.NotificationResponseDTO;
import com.example.flight.entity.Booking;
import com.example.flight.entity.Notification;
import com.example.flight.entity.User;
import com.example.flight.repository.BookingRepository;
import com.example.flight.repository.NotificationRepository;
import com.example.flight.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ModelMapper modelMapper;
    private final JavaMailSender mailSender;


    // ==========================================
    // GET ALL NOTIFICATIONS
    // ==========================================

    public List<NotificationResponseDTO> getAllNotifications() {

        return notificationRepository
                .findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }


    // ==========================================
    // GET NOTIFICATION BY ID
    // ==========================================

    public NotificationResponseDTO getNotificationById(
            Long notificationId
    ) {

        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Notification not found with ID: "
                                        + notificationId
                        )
                );

        return convertToResponse(notification);
    }


    // ==========================================
    // GET NOTIFICATIONS BY USER
    // ==========================================

    public List<NotificationResponseDTO> getNotificationsByUser(
            Long userId
    ) {

        if (!userRepository.existsById(userId)) {

            throw new RuntimeException(
                    "User not found with ID: " + userId
            );
        }

        return notificationRepository
                .findByUserUserId(userId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }


    // ==========================================
    // GET NOTIFICATIONS BY BOOKING
    // ==========================================

    public List<NotificationResponseDTO> getNotificationsByBooking(
            Long bookingId
    ) {

        if (!bookingRepository.existsById(bookingId)) {

            throw new RuntimeException(
                    "Booking not found with ID: " + bookingId
            );
        }

        return notificationRepository
                .findByBookingBookingId(bookingId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }


    // ==========================================
    // GET NOTIFICATIONS BY STATUS
    // ==========================================

    public List<NotificationResponseDTO> getNotificationsByStatus(
            String status
    ) {

        return notificationRepository
                .findByStatus(status)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }


    // ==========================================
    // CREATE NOTIFICATION
    // ==========================================

    public NotificationResponseDTO createNotification(
            NotificationRequestDTO dto
    ) {

        User user = userRepository
                .findById(dto.getUserId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found with ID: "
                                        + dto.getUserId()
                        )
                );

        Notification notification = modelMapper.map(
                dto,
                Notification.class
        );

        notification.setUser(user);

        if (dto.getBookingId() != null) {

            Booking booking = bookingRepository
                    .findById(dto.getBookingId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Booking not found with ID: "
                                            + dto.getBookingId()
                            )
                    );

            notification.setBooking(booking);
        }

        if (notification.getStatus() == null
                || notification.getStatus().isBlank()) {

            notification.setStatus("PENDING");
        }

        notification.setCreatedAt(LocalDateTime.now());

        Notification savedNotification =
                notificationRepository.save(notification);

        return convertToResponse(savedNotification);
    }


    // ==========================================
    // UPDATE NOTIFICATION
    // ==========================================

    public NotificationResponseDTO updateNotification(
            Long notificationId,
            NotificationRequestDTO dto
    ) {

        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Notification not found with ID: "
                                        + notificationId
                        )
                );

        User user = userRepository
                .findById(dto.getUserId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found with ID: "
                                        + dto.getUserId()
                        )
                );

        modelMapper.map(dto, notification);

        notification.setUser(user);

        if (dto.getBookingId() != null) {

            Booking booking = bookingRepository
                    .findById(dto.getBookingId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Booking not found with ID: "
                                            + dto.getBookingId()
                            )
                    );

            notification.setBooking(booking);

        } else {

            notification.setBooking(null);
        }

        Notification updatedNotification =
                notificationRepository.save(notification);

        return convertToResponse(updatedNotification);
    }


    // ==========================================
    // DELETE NOTIFICATION
    // ==========================================

    public void deleteNotification(
            Long notificationId
    ) {

        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Notification not found with ID: "
                                        + notificationId
                        )
                );

        notificationRepository.delete(notification);
    }


    // ==========================================
    // SEND BOOKING CONFIRMATION
    // ==========================================

    public void sendBookingConfirmationNotification(
            Booking booking
    ) {

        String subject =
                "Booking Confirmed - "
                        + booking.getBookingCode();

        String message =
                "Your flight booking has been confirmed.\n\n"
                        + "Booking Code: "
                        + booking.getBookingCode()
                        + "\n"
                        + "Flight: "
                        + booking.getFlight().getFlightNumber()
                        + "\n"
                        + "Departure: "
                        + booking.getFlight().getDepartureTs();

        sendAndSaveNotification(
                booking,
                "BOOKING_CONFIRMED",
                subject,
                message
        );
    }


    // ==========================================
    // SEND PAYMENT SUCCESS
    // ==========================================

    public void sendPaymentSuccessNotification(
            Booking booking
    ) {

        String subject =
                "Payment Successful - "
                        + booking.getBookingCode();

        String message =
                "Your payment was successful.\n\n"
                        + "Booking Code: "
                        + booking.getBookingCode()
                        + "\n"
                        + "Amount Paid: ₹"
                        + booking.getTotalAmount();

        sendAndSaveNotification(
                booking,
                "PAYMENT_SUCCESS",
                subject,
                message
        );
    }


    // ==========================================
    // SEND PAYMENT FAILED
    // ==========================================

    public void sendPaymentFailedNotification(
            Booking booking
    ) {

        String subject =
                "Payment Failed - "
                        + booking.getBookingCode();

        String message =
                "Your payment was unsuccessful.\n\n"
                        + "Booking Code: "
                        + booking.getBookingCode()
                        + "\n\nPlease try again.";

        sendAndSaveNotification(
                booking,
                "PAYMENT_FAILED",
                subject,
                message
        );
    }


    // ==========================================
    // SEND BOOKING CANCELLATION
    // ==========================================

    public void sendCancellationNotification(
            Booking booking,
            String reason
    ) {

        String subject =
                "Booking Cancelled - "
                        + booking.getBookingCode();

        String message =
                "Your booking has been cancelled.\n\n"
                        + "Booking Code: "
                        + booking.getBookingCode()
                        + "\n"
                        + "Reason: "
                        + reason;

        sendAndSaveNotification(
                booking,
                "BOOKING_CANCELLED",
                subject,
                message
        );
    }


    // ==========================================
    // SEND REFUND SUCCESS
    // ==========================================

    public void sendRefundNotification(
            Booking booking,
            java.math.BigDecimal refundAmount
    ) {

        String subject =
                "Refund Processed - "
                        + booking.getBookingCode();

        String message =
                "Your refund has been processed successfully.\n\n"
                        + "Booking Code: "
                        + booking.getBookingCode()
                        + "\n"
                        + "Refund Amount: ₹"
                        + refundAmount;

        sendAndSaveNotification(
                booking,
                "REFUND_SUCCESS",
                subject,
                message
        );
    }


    // ==========================================
    // SEND EMAIL + SAVE NOTIFICATION
    // ==========================================

    private void sendAndSaveNotification(
            Booking booking,
            String type,
            String subject,
            String message
    ) {

        Notification notification =
                new Notification();

        notification.setUser(booking.getUser());
        notification.setBooking(booking);
        notification.setType(type);
        notification.setChannel("EMAIL");
        notification.setMessage(message);
        notification.setCreatedAt(LocalDateTime.now());

        try {

            SimpleMailMessage email =
                    new SimpleMailMessage();

            email.setTo(
                    booking.getUser().getEmail()
            );

            email.setSubject(subject);

            email.setText(message);

            mailSender.send(email);

            notification.setStatus("SENT");

        } catch (Exception exception) {

            notification.setStatus("FAILED");
        }

        notificationRepository.save(notification);
    }


    // ==========================================
    // ENTITY TO RESPONSE DTO
    // ==========================================

    private NotificationResponseDTO convertToResponse(
            Notification notification
    ) {

        NotificationResponseDTO response =
                modelMapper.map(
                        notification,
                        NotificationResponseDTO.class
                );

        if (notification.getUser() != null) {

            response.setUserId(
                    notification.getUser().getUserId()
            );
        }

        if (notification.getBooking() != null) {

            response.setBookingId(
                    notification.getBooking()
                            .getBookingId()
            );
        }

        return response;
    }
}