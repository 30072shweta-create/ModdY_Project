package com.example.flight.tools;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.example.flight.dto.PaymentResponseDTO;
import com.example.flight.entity.Booking;
import com.example.flight.repository.BookingRepository;
import com.example.flight.service.PaymentService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentTools {

    private final PaymentService paymentService;
    private final BookingRepository bookingRepository;

    @Tool(description = """
            Get live payment information and status for a booking using either a booking code (e.g. ABC123) or numeric booking ID.
            Use this tool when the user asks about payment status, payment amount, payment method, or payment details for a booking.
            """)
    public List<PaymentResponseDTO> getPaymentsByBookingReference(String bookingReference) {
        System.out.println("PAYMENT TOOL CALLED FOR REFERENCE: " + bookingReference);
        if (bookingReference == null || bookingReference.isBlank()) {
            return List.of();
        }
        String ref = bookingReference.trim().replaceAll("[\"']", "");
        Long bookingId = null;
        try {
            bookingId = Long.parseLong(ref);
        } catch (NumberFormatException ignored) {}

        if (bookingId == null) {
            Booking booking = bookingRepository.findByBookingCodeIgnoreCase(ref).orElse(null);
            if (booking != null) {
                bookingId = booking.getBookingId();
            }
        }

        if (bookingId != null) {
            return paymentService.getPaymentsByBooking(bookingId);
        }
        return List.of();
    }

    @Tool(description = """
            Get live payment information for a booking by numeric booking ID.
            Use this tool when the user provides a numeric booking ID and asks about payment status.
            """)
    public List<PaymentResponseDTO> getPaymentsByBooking(
            Long bookingId) {

        System.out.println("PAYMENT TOOL CALLED FOR ID: " + bookingId);
        if (bookingId == null) {
            return List.of();
        }
        return paymentService.getPaymentsByBooking(bookingId);
    }

    @Tool(description = """
            Get the supported payment methods and payment options in SkyRoute.
            Use this tool when the user asks what payment methods, payment options,
            payment modes, cards, UPI, net banking, or wallets are accepted.
            No parameters required.
            """)
    public List<String> getPaymentMethods() {
        System.out.println("PAYMENT METHODS TOOL CALLED");
        return List.of(
                "Credit & Debit Cards: Visa, MasterCard, RuPay, and American Express",
                "UPI & QR Codes: Google Pay, PhonePe, Paytm, and BHIM",
                "Net Banking: 50+ major Indian and International banks (HDFC, ICICI, SBI, Axis, etc.)",
                "Digital Wallets: Paytm, PhonePe, Amazon Pay",
                "Razorpay: Secure SSL/TLS Encrypted Payment Gateway"
        );
    }

    @Tool(description = """
            Get live payment details using a payment ID.
            Use this tool when the user provides a payment ID
            and asks about its status or details.
            """)
    public PaymentResponseDTO getPaymentById(Long paymentId) {

        System.out.println("PAYMENT BY ID TOOL CALLED");

        return paymentService.getPaymentById(paymentId);
    }
}