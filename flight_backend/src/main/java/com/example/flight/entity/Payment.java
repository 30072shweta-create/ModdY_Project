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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "payments",
        schema = "flight_booking_system",
        indexes = {
                @Index(
                        name = "idx_payment_booking_id",
                        columnList = "booking_id"
                ),
                @Index(
                        name = "idx_payment_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_payment_razorpay_order_id",
                        columnList = "razorpay_order_id"
                ),
                @Index(
                        name = "idx_payment_razorpay_payment_id",
                        columnList = "razorpay_payment_id"
                )
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "booking_id",
            nullable = false
    )
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_method",
            length = 50,
            nullable = false
    )
    private PaymentMethod paymentMethod;

    @Column(
            name = "amount",
            precision = 10,
            scale = 2,
            nullable = false
    )
    private BigDecimal amount;

    @Column(
            name = "currency",
            length = 10,
            nullable = false
    )
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            length = 50,
            nullable = false
    )
    private PaymentStatus status;

    @Column(
            name = "razorpay_order_id",
            length = 100,
            unique = true
    )
    private String razorpayOrderId;

    @Column(
            name = "razorpay_payment_id",
            length = 100,
            unique = true
    )
    private String razorpayPaymentId;

    @Column(
            name = "razorpay_signature",
            length = 500
    )
    private String razorpaySignature;

    @Column(
            name = "transaction_ref",
            length = 100,
            unique = true
    )
    private String transactionRef;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.currency == null) {
            this.currency = "INR";
        }

        if (this.status == null) {
            this.status = PaymentStatus.CREATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getRazorpaySignature() {
        return razorpaySignature;
    }

    public void setRazorpaySignature(String razorpaySignature) {
        this.razorpaySignature = razorpaySignature;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}