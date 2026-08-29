package com.example.flight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.flight.dto.BookingCancellationRequestDTO;
import com.example.flight.dto.BookingCancellationResponseDTO;
import com.example.flight.dto.PaymentRequestDTO;
import com.example.flight.dto.PaymentResponseDTO;
import com.example.flight.dto.PaymentVerificationDTO;
import com.example.flight.entity.Booking;
import com.example.flight.entity.BookingCancellation;
import com.example.flight.entity.BookingStatus;
import com.example.flight.entity.Flight;
import com.example.flight.entity.Payment;
import com.example.flight.entity.PaymentMethod;
import com.example.flight.entity.PaymentStatus;
import com.example.flight.entity.User;
import com.example.flight.paymentgateway.GatewayOrderResponse;
import com.example.flight.paymentgateway.GatewayRefundResponse;
import com.example.flight.paymentgateway.PaymentGateway;
import com.example.flight.repository.BookingRepository;
import com.example.flight.repository.CancellationRepository;
import com.example.flight.repository.FlightRepository;
import com.example.flight.repository.PaymentRepository;
import com.example.flight.repository.SeatLockRepository;
import com.example.flight.service.BookingCancellationService;
import com.example.flight.service.NotificationService;
import com.example.flight.service.PaymentService;

@ExtendWith(MockitoExtension.class)
class Phase5PaymentAndRefundModuleTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SeatLockRepository seatLockRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CancellationRepository cancellationRepository;

    @Mock
    private FlightRepository flightRepository;

    private PaymentService paymentService;

    private BookingCancellationService bookingCancellationService;

    private Booking booking;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                bookingRepository,
                seatLockRepository,
                paymentGateway,
                notificationService
        );

        bookingCancellationService = new BookingCancellationService(
                bookingRepository,
                cancellationRepository,
                flightRepository,
                paymentRepository,
                paymentGateway
        );

        User user = new User();
        user.setUserId(1L);
        user.setEmail("user1@example.com");

        Flight flight = new Flight();
        flight.setFlightId(10L);
        flight.setFlightNumber("6E-501");
        flight.setDepartureTs(LocalDateTime.now().plusHours(30));
        flight.setAvailableSeats((short) 20);

        booking = new Booking();
        booking.setBookingId(100L);
        booking.setBookingCode("ABC123");
        booking.setUser(user);
        booking.setFlight(flight);
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setTotalAmount(new BigDecimal("4550.00"));
    }

    @Test
    @DisplayName("Create Razorpay order stores a pending payment")
    void createPaymentOrderStoresPendingPayment() {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .bookingId(booking.getBookingId())
                .paymentMethod(PaymentMethod.UPI)
                .build();

        when(bookingRepository.findById(booking.getBookingId()))
                .thenReturn(Optional.of(booking));
        when(paymentGateway.createOrder(
                eq(new BigDecimal("4550.00")),
                eq("INR"),
                any(String.class)
        )).thenReturn(GatewayOrderResponse.builder()
                .orderId("order_123")
                .amount(new BigDecimal("4550.00"))
                .currency("INR")
                .status("created")
                .build());
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    payment.setPaymentId(1L);
                    return payment;
                });

        PaymentResponseDTO response =
                paymentService.createPaymentOrder(request);

        assertEquals(1L, response.getPaymentId());
        assertEquals(booking.getBookingId(), response.getBookingId());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertEquals("order_123", response.getRazorpayOrderId());
    }

    @Test
    @DisplayName("Verify successful Razorpay payment confirms booking")
    void verifyPaymentConfirmsBooking() {
        Payment payment = Payment.builder()
                .booking(booking)
                .paymentMethod(PaymentMethod.UPI)
                .amount(new BigDecimal("4550.00"))
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .razorpayOrderId("order_123")
                .transactionRef("TXN-123")
                .build();
        payment.setPaymentId(1L);

        PaymentVerificationDTO request = PaymentVerificationDTO.builder()
                .razorpayOrderId("order_123")
                .razorpayPaymentId("pay_123")
                .razorpaySignature("signature")
                .build();

        when(paymentRepository.findByRazorpayOrderId("order_123"))
                .thenReturn(Optional.of(payment));
        when(paymentGateway.verifyPayment(
                "order_123",
                "pay_123",
                "signature"
        )).thenReturn(true);
        when(seatLockRepository.findByBookingBookingId(booking.getBookingId()))
                .thenReturn(List.of());
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponseDTO response = paymentService.verifyPayment(request);

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(PaymentStatus.SUCCESS, booking.getPaymentStatus());
        assertEquals("pay_123", response.getRazorpayPaymentId());
        verify(notificationService).sendPaymentSuccessNotification(booking);
    }

    @Test
    @DisplayName("Retry payment creates a new pending order")
    void retryPaymentCreatesNewPendingOrder() {
        Payment failedPayment = Payment.builder()
                .booking(booking)
                .paymentMethod(PaymentMethod.CARD)
                .amount(new BigDecimal("4550.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .razorpayOrderId("order_old")
                .transactionRef("TXN-OLD")
                .build();
        failedPayment.setPaymentId(1L);

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(failedPayment));
        when(paymentGateway.createOrder(
                eq(new BigDecimal("4550.00")),
                eq("INR"),
                any(String.class)
        )).thenReturn(GatewayOrderResponse.builder()
                .orderId("order_new")
                .amount(new BigDecimal("4550.00"))
                .currency("INR")
                .status("created")
                .build());
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    payment.setPaymentId(2L);
                    return payment;
                });

        PaymentResponseDTO response = paymentService.retryPayment(1L);

        assertEquals(2L, response.getPaymentId());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertEquals("order_new", response.getRazorpayOrderId());
    }

    @Test
    @DisplayName("Cancel confirmed booking creates refund through gateway")
    void cancelConfirmedBookingCreatesRefund() {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.SUCCESS);

        Payment successfulPayment = Payment.builder()
                .booking(booking)
                .paymentMethod(PaymentMethod.UPI)
                .amount(new BigDecimal("4550.00"))
                .currency("INR")
                .status(PaymentStatus.SUCCESS)
                .razorpayOrderId("order_123")
                .razorpayPaymentId("pay_123")
                .transactionRef("TXN-123")
                .build();
        successfulPayment.setPaymentId(1L);

        BookingCancellationRequestDTO request =
                new BookingCancellationRequestDTO();
        request.setBookingId(booking.getBookingId());
        request.setReason("Change of plans");

        when(bookingRepository.findById(booking.getBookingId()))
                .thenReturn(Optional.of(booking));
        when(cancellationRepository.existsByBookingBookingId(booking.getBookingId()))
                .thenReturn(false);
        when(paymentRepository.findFirstByBookingBookingIdAndStatusOrderByCreatedAtDesc(
                booking.getBookingId(),
                PaymentStatus.SUCCESS
        )).thenReturn(Optional.of(successfulPayment));
        when(cancellationRepository.save(any(BookingCancellation.class)))
                .thenAnswer(invocation -> {
                    BookingCancellation cancellation = invocation.getArgument(0);
                    if (cancellation.getCancellationId() == null) {
                        cancellation.setCancellationId(50L);
                    }
                    return cancellation;
                });
        when(paymentGateway.refundPayment(
                eq("pay_123"),
                eq(new BigDecimal("4095.00"))
        )).thenReturn(GatewayRefundResponse.builder()
                .refundId("refund_123")
                .paymentId("pay_123")
                .amount(new BigDecimal("4095.00"))
                .status("processed")
                .build());

        BookingCancellationResponseDTO response =
                bookingCancellationService.cancelBooking(
                        request,
                        "user1@example.com"
                );

        assertEquals(50L, response.getCancellationId());
        assertEquals(booking.getBookingId(), response.getBookingId());
        assertEquals(new BigDecimal("455.00"), response.getCancellationCharge());
        assertEquals(new BigDecimal("4095.00"), response.getRefundAmount());
        assertEquals(BookingStatus.REFUNDED, response.getStatus());
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(PaymentStatus.REFUNDED, booking.getPaymentStatus());
        assertEquals((short) 21, booking.getFlight().getAvailableSeats());
    }
}
