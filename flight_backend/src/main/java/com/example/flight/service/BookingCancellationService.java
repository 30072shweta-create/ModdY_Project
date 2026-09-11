package com.example.flight.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight.dto.BookingCancellationRequestDTO;
import com.example.flight.dto.BookingCancellationResponseDTO;
import com.example.flight.entity.Booking;
import com.example.flight.entity.BookingCancellation;
import com.example.flight.entity.BookingStatus;
import com.example.flight.entity.Flight;
import com.example.flight.entity.Payment;
import com.example.flight.entity.PaymentStatus;
import com.example.flight.paymentgateway.GatewayRefundResponse;
import com.example.flight.paymentgateway.PaymentGateway;
import com.example.flight.repository.BookingRepository;
import com.example.flight.repository.CancellationRepository;
import com.example.flight.repository.FlightRepository;
import com.example.flight.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingCancellationService {

    private final BookingRepository bookingRepository;
    private final CancellationRepository cancellationRepository;
    private final FlightRepository flightRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;

    @Transactional
    public BookingCancellationResponseDTO cancelBooking(
            BookingCancellationRequestDTO dto,
            String userEmail
    ) {

        Booking booking = bookingRepository
                .findById(dto.getBookingId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Booking not found with ID: "
                                        + dto.getBookingId()
                        )
                );

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Booking is already cancelled"
            );
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Only confirmed bookings can be cancelled"
            );
        }

        if (cancellationRepository
                .existsByBookingBookingId(booking.getBookingId())) {

            throw new IllegalStateException(
                    "Cancellation request already exists"
            );
        }

        long hoursBeforeDeparture = Duration.between(
                LocalDateTime.now(),
                booking.getFlight().getDepartureTs()
        ).toHours();

        if (hoursBeforeDeparture < 0) {
            throw new IllegalStateException(
                    "Booking cannot be cancelled after flight departure"
            );
        }

        Payment payment = paymentRepository
                .findFirstByBookingBookingIdAndStatusOrderByCreatedAtDesc(
                        booking.getBookingId(),
                        PaymentStatus.SUCCESS
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Successful payment not found"
                        )
                );

        if (payment.getRazorpayPaymentId() == null
                || payment.getRazorpayPaymentId().isBlank()) {

            throw new IllegalStateException(
                    "Razorpay payment ID not found"
            );
        }

        BigDecimal originalAmount = booking.getTotalAmount();

        BigDecimal cancellationPercentage;

        if (hoursBeforeDeparture > 24) {
            cancellationPercentage = BigDecimal.valueOf(10);
        } else if (hoursBeforeDeparture >= 6) {
            cancellationPercentage = BigDecimal.valueOf(25);
        } else {
            cancellationPercentage = BigDecimal.valueOf(50);
        }

        BigDecimal cancellationCharge = originalAmount
                .multiply(cancellationPercentage)
                .divide(
                        BigDecimal.valueOf(100),
                        2,
                        RoundingMode.HALF_UP
                );

        BigDecimal refundAmount = originalAmount
                .subtract(cancellationCharge);

        BookingCancellation cancellation =
                new BookingCancellation();

        cancellation.setBooking(booking);
        cancellation.setReason(dto.getReason());
        cancellation.setOriginalAmount(originalAmount);
        cancellation.setCancellationCharge(cancellationCharge);
        cancellation.setRefundAmount(refundAmount);
        cancellation.setStatus(
                BookingStatus.REFUND_PENDING
        );

        BookingCancellation savedCancellation =
                cancellationRepository.save(cancellation);

        payment.setStatus(PaymentStatus.REFUND_PENDING);
        paymentRepository.save(payment);

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        bookingRepository.save(booking);

        try {

            GatewayRefundResponse refundResponse =
                    paymentGateway.refundPayment(
                            payment.getRazorpayPaymentId(),
                            refundAmount
                    );

            savedCancellation.setRazorpayRefundId(
                    refundResponse.getRefundId()
            );

            savedCancellation.setRefundedAt(
                    LocalDateTime.now()
            );

            savedCancellation.setStatus(
                    BookingStatus.REFUNDED
            );

            payment.setStatus(
                    PaymentStatus.REFUNDED
            );

            booking.setPaymentStatus(
                    PaymentStatus.REFUNDED
            );

            cancellationRepository.save(savedCancellation);
            paymentRepository.save(payment);
            bookingRepository.save(booking);

        } catch (Exception exception) {

            savedCancellation.setStatus(
                    BookingStatus.FAILED
            );

            cancellationRepository.save(savedCancellation);

            throw new RuntimeException(
                    "Booking cancelled, but Razorpay refund failed",
                    exception
            );
        }

        Flight flight = booking.getFlight();
        if (flight != null) {
            flight.setAvailableSeats(
                    (short) (
                            flight.getAvailableSeats() + 1
                    )
            );
            flightRepository.save(flight);
        } else if (booking.getSegments() != null) {
            for (var seg : booking.getSegments()) {
                if (seg.getFlight() != null) {
                    Flight f = seg.getFlight();
                    f.setAvailableSeats((short) (f.getAvailableSeats() + 1));
                    flightRepository.save(f);
                }
            }
        }

        notificationService.sendCancellationNotification(booking, dto.getReason());
        notificationService.sendRefundNotification(booking, refundAmount);

        return convertToResponse(savedCancellation);
    }

    public BookingCancellationResponseDTO getCancellationByBookingId(
            Long bookingId
    ) {

        BookingCancellation cancellation =
                cancellationRepository
                        .findByBookingBookingId(bookingId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Cancellation not found"
                                )
                        );

        return convertToResponse(cancellation);
    }

    public BookingCancellationResponseDTO getCancellationByBookingReference(
            String bookingReference
    ) {
        if (bookingReference == null || bookingReference.isBlank()) {
            throw new RuntimeException("Booking reference is required");
        }
        String ref = bookingReference.trim();
        try {
            Long bookingId = Long.parseLong(ref);
            var opt = cancellationRepository.findByBookingBookingId(bookingId);
            if (opt.isPresent()) {
                return convertToResponse(opt.get());
            }
        } catch (NumberFormatException ignored) {}

        return cancellationRepository.findByBookingBookingCodeIgnoreCase(ref)
                .map(this::convertToResponse)
                .orElseThrow(() -> new RuntimeException("Cancellation not found for booking reference: " + ref));
    }

    public String calculateEstimatedRefund(String bookingReference) {
        if (bookingReference == null || bookingReference.isBlank()) {
            return "Booking reference is required to calculate estimated refund.";
        }
        Booking booking = null;
        try {
            Long bookingId = Long.parseLong(bookingReference.trim());
            booking = bookingRepository.findById(bookingId).orElse(null);
        } catch (NumberFormatException ignored) {}

        if (booking == null) {
            booking = bookingRepository.findByBookingCodeIgnoreCase(bookingReference.trim()).orElse(null);
        }

        if (booking == null) {
            return "Booking not found with reference: " + bookingReference;
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return "Booking " + booking.getBookingCode() + " has already been cancelled.";
        }

        LocalDateTime departureTs = null;
        if (booking.getFlight() != null) {
            departureTs = booking.getFlight().getDepartureTs();
        } else if (booking.getSegments() != null && !booking.getSegments().isEmpty() && booking.getSegments().get(0).getFlight() != null) {
            departureTs = booking.getSegments().get(0).getFlight().getDepartureTs();
        }

        if (departureTs == null) {
            return "Departure time not available to calculate refund for booking " + booking.getBookingCode();
        }

        long hoursToDeparture = Duration.between(LocalDateTime.now(), departureTs).toHours();
        BigDecimal originalAmount = booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal feePercentage;
        if (hoursToDeparture > 24) {
            feePercentage = new BigDecimal("0.10");
        } else if (hoursToDeparture >= 6) {
            feePercentage = new BigDecimal("0.25");
        } else {
            feePercentage = new BigDecimal("0.50");
        }

        BigDecimal fee = originalAmount.multiply(feePercentage).setScale(2, RoundingMode.HALF_UP);
        BigDecimal refund = originalAmount.subtract(fee).setScale(2, RoundingMode.HALF_UP);

        int feePercentInt = feePercentage.multiply(BigDecimal.valueOf(100)).intValue();
        return "Cancellation estimate for booking " + booking.getBookingCode() + " (" + hoursToDeparture + " hours before flight):\n"
                + "• Original Booking Amount: ₹" + originalAmount + "\n"
                + "• Cancellation Charge (" + feePercentInt + "%): ₹" + fee + "\n"
                + "• Estimated Refund (" + (100 - feePercentInt) + "%): ₹" + refund;
    }

    private BookingCancellationResponseDTO convertToResponse(
            BookingCancellation cancellation
    ) {

        return BookingCancellationResponseDTO
                .builder()
                .cancellationId(
                        cancellation.getCancellationId()
                )
                .bookingId(
                        cancellation.getBooking().getBookingId()
                )
                .bookingCode(
                        cancellation.getBooking().getBookingCode()
                )
                .reason(
                        cancellation.getReason()
                )
                .originalAmount(
                        cancellation.getOriginalAmount()
                )
                .cancellationCharge(
                        cancellation.getCancellationCharge()
                )
                .refundAmount(
                        cancellation.getRefundAmount()
                )
                .status(
                        cancellation.getStatus()
                )
                .cancelledAt(
                        cancellation.getCancelledAt()
                )
                .build();
    }
}
