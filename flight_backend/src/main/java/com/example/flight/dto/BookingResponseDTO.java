package com.example.flight.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.example.flight.entity.BookingStatus;
import com.example.flight.entity.PaymentStatus;

public class BookingResponseDTO {

    private Long bookingId;
    private Long userId;
    private String bookingCode;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private BigDecimal totalAmount;
    private LocalDateTime bookingTs;
    private List<BookingSegmentResponseDTO> segments;

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public LocalDateTime getBookingTs() { return bookingTs; }
    public void setBookingTs(LocalDateTime bookingTs) { this.bookingTs = bookingTs; }

    public List<BookingSegmentResponseDTO> getSegments() { return segments; }
    public void setSegments(List<BookingSegmentResponseDTO> segments) { this.segments = segments; }
}