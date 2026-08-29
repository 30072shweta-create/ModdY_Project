package com.example.flight.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import com.example.flight.entity.Booking;
import com.example.flight.entity.Payment;
import com.example.flight.entity.User;
import com.example.flight.exception.ResourceNotFoundException;
import com.example.flight.repository.BookingRepository;
import com.example.flight.repository.PaymentRepository;
import com.example.flight.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingAccessService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public Booking getUserBooking(Long bookingId, String email) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        if (booking.getUser() != null && booking.getUser().getEmail() != null && !booking.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("You are not authorized to access this booking");
        }
        return booking;
    }

    public Booking verifyBookingAccess(Long bookingId, Authentication authentication) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        if (isAdmin(authentication)) {
            return booking;
        }

        String email = authentication.getName();
        if (booking.getUser() != null && booking.getUser().getEmail() != null && !booking.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("You are not authorized to access this booking");
        }
        return booking;
    }

    public Booking verifyBookingCodeAccess(String bookingCode, Authentication authentication) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with code: " + bookingCode));

        if (isAdmin(authentication)) {
            return booking;
        }

        String email = authentication.getName();
        if (booking.getUser() != null && booking.getUser().getEmail() != null && !booking.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("You are not authorized to access this booking");
        }
        return booking;
    }

    public void verifyUserAccess(Long userId, Authentication authentication) {
        if (isAdmin(authentication)) {
            return;
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User not found for email: " + email));

        if (!user.getUserId().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to access bookings for user ID: " + userId);
        }
    }

    public Payment verifyPaymentAccess(Long paymentId, Authentication authentication) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        if (isAdmin(authentication)) {
            return payment;
        }

        String email = authentication.getName();
        if (payment.getBooking() != null && payment.getBooking().getUser() != null && payment.getBooking().getUser().getEmail() != null
                && !payment.getBooking().getUser().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("You are not authorized to access this payment");
        }
        return payment;
    }

    public boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String auth = authority.getAuthority();
            if ("ROLE_ADMIN".equalsIgnoreCase(auth) || "ADMIN".equalsIgnoreCase(auth)) {
                return true;
            }
        }
        return false;
    }
}