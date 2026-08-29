package com.example.flight.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "cancellations",
        schema = "flight_booking_system"
)

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BookingCancellation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cancellation_id")
    private Long cancellationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "booking_id",
            nullable = false,
            unique = true
    )
    private Booking booking;

    @Column(
            name = "reason",
            length = 500
    )
    private String reason;

    @Column(
            name = "original_amount",
            precision = 10,
            scale = 2,
            nullable = false
    )
    private BigDecimal originalAmount;

    @Column(
            name = "cancellation_charge",
            precision = 10,
            scale = 2,
            nullable = false
    )
    private BigDecimal cancellationCharge;

    @Column(
            name = "refund_amount",
            precision = 10,
            scale = 2,
            nullable = false
    )
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 50
    )
    private BookingStatus status;

    @Column(
            name = "cancelled_at",
            nullable = false
    )
    private LocalDateTime cancelledAt;

    @Column(
            name = "razorpay_refund_id",
            length = 100
    )
    private String razorpayRefundId;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @PrePersist
    protected void onCreate() {

        if (cancelledAt == null) {
            cancelledAt = LocalDateTime.now();
        }

        if (status == null) {
            status = BookingStatus.REQUESTED;
        }
    }

    public Long getCancellationId() {
        return cancellationId;
    }

    public void setCancellationId(Long cancellationId) {
        this.cancellationId = cancellationId;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public BigDecimal getCancellationCharge() {
        return cancellationCharge;
    }

    public void setCancellationCharge(BigDecimal cancellationCharge) {
        this.cancellationCharge = cancellationCharge;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getRazorpayRefundId() {
        return razorpayRefundId;
    }

    public void setRazorpayRefundId(String razorpayRefundId) {
        this.razorpayRefundId = razorpayRefundId;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }
}
