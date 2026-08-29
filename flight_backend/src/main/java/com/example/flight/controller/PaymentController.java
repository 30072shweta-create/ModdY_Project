package com.example.flight.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.flight.dto.PaymentRequestDTO;
import com.example.flight.dto.PaymentResponseDTO;
import com.example.flight.dto.PaymentVerificationDTO;
import com.example.flight.service.BookingAccessService;
import com.example.flight.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    private final BookingAccessService bookingAccessService;


    /**
     * Create Razorpay Order
     */
    @PostMapping("/create-order")
    public ResponseEntity<PaymentResponseDTO> createOrder(
            @Valid @RequestBody
            PaymentRequestDTO request,

            Authentication authentication
    ) {

        bookingAccessService.verifyBookingAccess(
                request.getBookingId(),
                authentication
        );


        PaymentResponseDTO response =
                paymentService.createPaymentOrder(
                        request
                );


        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    /**
     * Verify Razorpay Payment
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponseDTO> verifyPayment(
            @Valid @RequestBody
            PaymentVerificationDTO request,

            Authentication authentication
    ) {

        PaymentResponseDTO response =
                paymentService.verifyPayment(
                        request
                );


        bookingAccessService.verifyBookingAccess(
                response.getBookingId(),
                authentication
        );


        return ResponseEntity.ok(
                response
        );
    }


    /**
     * Retry Failed Payment
     */
    @PostMapping("/{paymentId}/retry")
    public ResponseEntity<PaymentResponseDTO> retryPayment(
            @PathVariable Long paymentId,

            Authentication authentication
    ) {

        bookingAccessService.verifyPaymentAccess(
                paymentId,
                authentication
        );


        PaymentResponseDTO response =
                paymentService.retryPayment(
                        paymentId
                );


        return ResponseEntity.ok(
                response
        );
    }


    /**
     * Get Payment By ID
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDTO> getPaymentById(
            @PathVariable Long paymentId,

            Authentication authentication
    ) {

        bookingAccessService.verifyPaymentAccess(
                paymentId,
                authentication
        );


        return ResponseEntity.ok(
                paymentService.getPaymentById(
                        paymentId
                )
        );
    }


    /**
     * Get Payments By Booking
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<PaymentResponseDTO>>
    getPaymentsByBooking(
            @PathVariable Long bookingId,

            Authentication authentication
    ) {

        bookingAccessService.verifyBookingAccess(
                bookingId,
                authentication
        );


        return ResponseEntity.ok(
                paymentService.getPaymentsByBooking(
                        bookingId
                )
        );
    }


    /**
     * Get My Payment History
     */
    @GetMapping("/my-payments")
    public ResponseEntity<List<PaymentResponseDTO>>
    getMyPayments(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                paymentService.getUserPayments(
                        authentication.getName()
                )
        );
    }


    /**
     * Get All Payments - ADMIN
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>>
    getAllPayments() {

        return ResponseEntity.ok(
                paymentService.getAllPayments()
        );
    }
}