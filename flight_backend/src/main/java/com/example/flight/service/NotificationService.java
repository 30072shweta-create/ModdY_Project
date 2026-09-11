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

import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ModelMapper modelMapper;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:shwetagaonkar179@gmail.com}")
    private String fromEmail;


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

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(dto.getType() != null ? dto.getType() : "SYSTEM_ALERT");
        notification.setChannel(dto.getChannel() != null ? dto.getChannel() : "EMAIL");
        notification.setMessage(dto.getMessage());
        notification.setCreatedAt(LocalDateTime.now());

        if (dto.getBookingId() != null) {
            Booking booking = bookingRepository
                    .findById(dto.getBookingId())
                    .orElse(null);
            notification.setBooking(booking);
        }

        String recipientEmail = (dto.getRecipientEmail() != null && !dto.getRecipientEmail().isBlank())
                ? dto.getRecipientEmail()
                : user.getEmail();

        try {
            if (recipientEmail != null && !recipientEmail.isBlank()) {
                SimpleMailMessage email = new SimpleMailMessage();
                if (fromEmail != null && !fromEmail.isBlank()) {
                    email.setFrom(fromEmail);
                }
                email.setTo(recipientEmail);
                email.setSubject("[SkyRoute] " + (dto.getType() != null ? dto.getType().replace('_', ' ') : "Notification Alert"));
                email.setText(dto.getMessage());

                mailSender.send(email);
                notification.setStatus("SENT");
            } else {
                notification.setStatus("SKIPPED_NO_EMAIL");
            }
        } catch (Exception ex) {
            notification.setStatus("FAILED");
        }

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
        String flightNumber = "Flight";
        String departure = "Scheduled";
        String route = "";

        if (booking.getFlight() != null) {
            flightNumber = booking.getFlight().getFlightNumber();
            departure = booking.getFlight().getDepartureTs() != null ? booking.getFlight().getDepartureTs().toString() : "";
            if (booking.getFlight().getFromAirport() != null && booking.getFlight().getToAirport() != null) {
                route = booking.getFlight().getFromAirport().getAirportCode() + " -> " + booking.getFlight().getToAirport().getAirportCode();
            }
        } else if (booking.getSegments() != null && !booking.getSegments().isEmpty()) {
            var seg = booking.getSegments().get(0);
            if (seg.getFlight() != null) {
                flightNumber = seg.getFlight().getFlightNumber();
                departure = seg.getFlight().getDepartureTs() != null ? seg.getFlight().getDepartureTs().toString() : "";
                if (seg.getFlight().getFromAirport() != null && seg.getFlight().getToAirport() != null) {
                    route = seg.getFlight().getFromAirport().getAirportCode() + " -> " + seg.getFlight().getToAirport().getAirportCode();
                }
            }
        }

        String subject = "Booking Confirmed - " + booking.getBookingCode();

        String message = "Dear Customer,\n\n"
                + "Your flight booking has been successfully CONFIRMED!\n\n"
                + "Booking Reference (PNR): " + booking.getBookingCode() + "\n"
                + "Flight: " + flightNumber + (route.isEmpty() ? "" : " (" + route + ")") + "\n"
                + "Departure Time: " + departure + "\n"
                + "Total Amount Paid: ₹" + (booking.getTotalAmount() != null ? booking.getTotalAmount() : "0") + "\n"
                + "Status: CONFIRMED (CNF)\n\n"
                + "You can view, print, or download your official boarding pass anytime from your SkyRoute dashboard.\n\n"
                + "Thank you for choosing SkyRoute Airlines!";

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

        String message = "Dear Customer,\n\n"
                + "Your flight booking #" + booking.getBookingCode() + " has been successfully CANCELLED.\n\n"
                + "Booking Code: " + booking.getBookingCode() + "\n"
                + "Reason: " + (reason != null ? reason : "User requested cancellation") + "\n"
                + "Status: CANCELLED\n\n"
                + "Your refund is being processed back to your original payment method.\n\n"
                + "Thank you,\nSkyRoute Airlines Support";

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

        String message = "Dear Customer,\n\n"
                + "Your refund for booking #" + booking.getBookingCode() + " has been PROCESSED.\n\n"
                + "Booking Code: " + booking.getBookingCode() + "\n"
                + "Net Refund Amount: ₹" + (refundAmount != null ? refundAmount : "0") + "\n"
                + "Status: REFUND_SUCCESS\n\n"
                + "The refund will reflect in your original payment method (Razorpay/Bank Account) within 3-5 business days.\n\n"
                + "Thank you,\nSkyRoute Airlines Support";

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
            if (booking.getUser() != null && booking.getUser().getEmail() != null && !booking.getUser().getEmail().isBlank()) {
                SimpleMailMessage email = new SimpleMailMessage();
                if (fromEmail != null && !fromEmail.isBlank()) {
                    email.setFrom(fromEmail);
                }
                email.setTo(booking.getUser().getEmail());
                email.setSubject(subject);
                email.setText(message);

                mailSender.send(email);
                notification.setStatus("SENT");
            } else {
                notification.setStatus("SKIPPED_NO_EMAIL");
            }
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
            response.setRecipientEmail(
                    notification.getUser().getEmail()
            );
        }

        if (notification.getBooking() != null) {
            response.setBookingId(
                    notification.getBooking()
                            .getBookingId()
            );
        }

        response.setSentAt(notification.getCreatedAt());

        return response;
    }
}