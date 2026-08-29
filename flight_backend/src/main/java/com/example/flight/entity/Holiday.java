package com.example.flight.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "holidays",
        schema = "flight_booking_system",
        indexes = {
            @Index(name = "idx_holiday_date_active", columnList = "holiday_date, active")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holiday_id")
    private Long holidayId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "holiday_date", nullable = false, unique = true)
    private LocalDate holidayDate;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    public Long getHolidayId() { return holidayId; }
    public void setHolidayId(Long holidayId) { this.holidayId = holidayId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getHolidayDate() { return holidayDate; }
    public void setHolidayDate(LocalDate holidayDate) { this.holidayDate = holidayDate; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
