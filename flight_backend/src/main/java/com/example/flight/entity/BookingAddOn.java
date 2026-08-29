package com.example.flight.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "booking_add_ons")
@NoArgsConstructor
@AllArgsConstructor
public class BookingAddOn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "addon_id")
    private Long addonId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "booking_id",
            nullable = false
    )
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "addon_type",
            nullable = false,
            length = 30
    )
    private AddOnType addonType;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price",
            nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_price",
            nullable = false)
    private BigDecimal totalPrice;

    @CreationTimestamp
    @Column(name = "created_at",
            updatable = false)
    private LocalDateTime createdAt;

    public Long getAddonId() { return addonId; }
    public void setAddonId(Long addonId) { this.addonId = addonId; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public AddOnType getAddonType() { return addonType; }
    public void setAddonType(AddOnType addonType) { this.addonType = addonType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}