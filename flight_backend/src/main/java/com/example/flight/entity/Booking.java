package com.example.flight.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "bookings",
        schema = "flight_booking_system",
        indexes = {
            @Index(name = "idx_booking_user_id", columnList = "user_id"),
            @Index(name = "idx_booking_flight_id", columnList = "flight_id"),
            @Index(name = "idx_booking_status", columnList = "status"),
            @Index(name = "idx_booking_code", columnList = "booking_code")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(
            name = "booking_code",
            length = 6,
            unique = true,
            nullable = false
    )
    private String bookingCode;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            length = 50,
            nullable = false
    )
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_status",
            length = 50,
            nullable = false
    )
    private PaymentStatus paymentStatus;

    @Column(
            name = "total_amount",
            precision = 10,
            scale = 2,
            nullable = false
    )
    private BigDecimal totalAmount;

    @Column(name = "booking_ts")
    private LocalDateTime bookingTs;

    @OneToMany(
            mappedBy = "booking",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("segmentOrder ASC")
    private List<BookingSegment> segments = new ArrayList<>();

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Flight getFlight() { return flight; }
    public void setFlight(Flight flight) { this.flight = flight; }

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

    public List<BookingSegment> getSegments() { return segments; }
    public void setSegments(List<BookingSegment> segments) { this.segments = segments; }
}