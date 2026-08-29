package com.example.flight.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight.dto.PaymentRequestDTO;
import com.example.flight.dto.PaymentResponseDTO;
import com.example.flight.dto.PaymentVerificationDTO;
import com.example.flight.entity.Booking;
import com.example.flight.entity.BookingStatus;
import com.example.flight.entity.Payment;
import com.example.flight.entity.PaymentStatus;
import com.example.flight.entity.SeatLock;
import com.example.flight.entity.SeatLockStatus;
import com.example.flight.exception.PaymentNotFoundException;
import com.example.flight.exception.ResourceNotFoundException;
import com.example.flight.paymentgateway.GatewayOrderResponse;
import com.example.flight.paymentgateway.PaymentGateway;
import com.example.flight.repository.BookingRepository;
import com.example.flight.repository.PaymentRepository;
import com.example.flight.repository.SeatLockRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private final BookingRepository bookingRepository;

    private final SeatLockRepository seatLockRepository;

    private final PaymentGateway paymentGateway;
private final NotificationService notificationService;

    /**
     * Create Razorpay Order
     */
    @Transactional
    public PaymentResponseDTO createPaymentOrder(
            PaymentRequestDTO request
    ) {

        Booking booking = bookingRepository
                .findById(request.getBookingId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found with ID: "
                                        + request.getBookingId()
                        )
                );


        if (booking.getStatus() == BookingStatus.CANCELLED) {

            throw new IllegalStateException(
                    "Cannot make payment for cancelled booking"
            );
        }


        if (booking.getPaymentStatus() == PaymentStatus.SUCCESS) {

            throw new IllegalStateException(
                    "Payment is already completed for this booking"
            );
        }


        if (booking.getTotalAmount() == null
                || booking.getTotalAmount().signum() <= 0) {

            throw new IllegalStateException(
                    "Invalid booking amount"
            );
        }


        String transactionReference =
                generateTransactionReference();


        GatewayOrderResponse gatewayResponse =
                paymentGateway.createOrder(
                        booking.getTotalAmount(),
                        "INR",
                        transactionReference
                );


        Payment payment = Payment.builder()
                .booking(booking)
                .paymentMethod(request.getPaymentMethod())
                .amount(booking.getTotalAmount())
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .transactionRef(transactionReference)
                .razorpayOrderId(
                        gatewayResponse.getOrderId()
                )
                .build();


        Payment savedPayment =
                paymentRepository.save(payment);


        return convertToResponse(savedPayment);
    }


    /**
     * Verify Razorpay Payment
     */
    @Transactional
    public PaymentResponseDTO verifyPayment(
            PaymentVerificationDTO request
    ) {

        Payment payment = paymentRepository
                .findByRazorpayOrderId(
                        request.getRazorpayOrderId()
                )
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment not found for Razorpay order ID"
                        )
                );


        if (payment.getStatus() == PaymentStatus.SUCCESS) {

            return convertToResponse(payment);
        }


        boolean isValid =
                paymentGateway.verifyPayment(
                        payment.getRazorpayOrderId(),
                        request.getRazorpayPaymentId(),
                        request.getRazorpaySignature()
                );


        if (!isValid) {

            payment.setStatus(PaymentStatus.FAILED);

            payment.getBooking()
                    .setStatus(
                            BookingStatus.FAILED
                    );

            payment.getBooking()
                    .setPaymentStatus(
                            PaymentStatus.FAILED
                    );


            bookingRepository.save(
                    payment.getBooking()
            );

            Payment savedPayment =
                    paymentRepository.save(payment);

            return convertToResponse(savedPayment);
        }


        payment.setStatus(PaymentStatus.SUCCESS);

        payment.setRazorpayPaymentId(
                request.getRazorpayPaymentId()
        );

        payment.setRazorpaySignature(
                request.getRazorpaySignature()
        );

        payment.setPaidAt(
                LocalDateTime.now()
        );


        Booking booking = payment.getBooking();

        booking.setStatus(
                BookingStatus.CONFIRMED
        );

        booking.setPaymentStatus(
                PaymentStatus.SUCCESS
        );


        confirmSeats(
                booking.getBookingId()
        );


        bookingRepository.save(booking);

        Payment savedPayment =
                paymentRepository.save(payment);
                notificationService.sendPaymentSuccessNotification(
        booking
);


        return convertToResponse(savedPayment);
    }


    /**
     * Retry Payment
     */
    @Transactional
    public PaymentResponseDTO retryPayment(
            Long paymentId
    ) {

        Payment oldPayment =
                paymentRepository.findById(paymentId)
                        .orElseThrow(() ->
                                new PaymentNotFoundException(
                                        "Payment not found with ID: "
                                                + paymentId
                                )
                        );


        if (oldPayment.getStatus()
                == PaymentStatus.SUCCESS) {

            throw new IllegalStateException(
                    "Successful payment cannot be retried"
            );
        }


        Booking booking =
                oldPayment.getBooking();


        if (booking.getStatus()
                == BookingStatus.CANCELLED) {

            throw new IllegalStateException(
                    "Cannot retry payment for cancelled booking"
            );
        }


        String transactionReference =
                generateTransactionReference();


        GatewayOrderResponse gatewayResponse =
                paymentGateway.createOrder(
                        booking.getTotalAmount(),
                        "INR",
                        transactionReference
                );


        Payment newPayment =
                Payment.builder()
                        .booking(booking)
                        .paymentMethod(
                                oldPayment.getPaymentMethod()
                        )
                        .amount(
                                booking.getTotalAmount()
                        )
                        .currency("INR")
                        .status(
                                PaymentStatus.PENDING
                        )
                        .transactionRef(
                                transactionReference
                        )
                        .razorpayOrderId(
                                gatewayResponse.getOrderId()
                        )
                        .build();


        Payment savedPayment =
                paymentRepository.save(
                        newPayment
                );


        return convertToResponse(
                savedPayment
        );
    }


    /**
     * Get Payment By ID
     */
    public PaymentResponseDTO getPaymentById(
            Long paymentId
    ) {

        Payment payment =
                paymentRepository.findById(paymentId)
                        .orElseThrow(() ->
                                new PaymentNotFoundException(
                                        "Payment not found with ID: "
                                                + paymentId
                                )
                        );


        return convertToResponse(payment);
    }


    /**
     * Get Payments By Booking
     */
    public List<PaymentResponseDTO> getPaymentsByBooking(
            Long bookingId
    ) {

        return paymentRepository
                .findByBookingBookingIdOrderByCreatedAtDesc(
                        bookingId
                )
                .stream()
                .map(this::convertToResponse)
                .toList();
    }


    /**
     * Get Logged-in User Payments
     */
    public List<PaymentResponseDTO> getUserPayments(
            String email
    ) {

        return paymentRepository
                .findByBookingUserEmailOrderByCreatedAtDesc(
                        email
                )
                .stream()
                .map(this::convertToResponse)
                .toList();
    }


    /**
     * Get All Payments - Admin
     */
    public List<PaymentResponseDTO> getAllPayments() {

        return paymentRepository
                .findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }


    /**
     * Confirm locked seats after successful payment
     */
    private void confirmSeats(
            Long bookingId
    ) {

        List<SeatLock> seatLocks =
                seatLockRepository
                        .findByBookingBookingId(
                                bookingId
                        );


        for (SeatLock seatLock : seatLocks) {

            if (seatLock.getStatus()
                    == SeatLockStatus.LOCKED) {

                seatLock.setStatus(
                        SeatLockStatus.CONFIRMED
                );

                seatLockRepository.save(
                        seatLock
                );
            }
        }
    }


    private String generateTransactionReference() {

        return "TXN-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }


    private PaymentResponseDTO convertToResponse(
            Payment payment
    ) {

        return PaymentResponseDTO
                .builder()

                .paymentId(
                        payment.getPaymentId()
                )

                .bookingId(
                        payment.getBooking() != null
                                ? payment.getBooking()
                                        .getBookingId()
                                : null
                )

                .paymentMethod(
                        payment.getPaymentMethod()
                )

                .amount(
                        payment.getAmount()
                )

                .currency(
                        payment.getCurrency()
                )

                .status(
                        payment.getStatus()
                )

                .razorpayOrderId(
                        payment.getRazorpayOrderId()
                )

                .razorpayPaymentId(
                        payment.getRazorpayPaymentId()
                )

                .transactionRef(
                        payment.getTransactionRef()
                )

                .paidAt(
                        payment.getPaidAt()
                )

                .createdAt(
                        payment.getCreatedAt()
                )

                .build();
    }
}