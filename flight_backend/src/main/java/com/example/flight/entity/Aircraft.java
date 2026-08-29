package com.example.flight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "aircrafts",
    schema = "flight_booking_system",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_aircraft_code", columnNames = {"aircraft_code"})
    },
    indexes = {
        @Index(name = "idx_aircraft_code", columnList = "aircraft_code"),
        @Index(name = "idx_aircraft_active", columnList = "active")
    }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Aircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "aircraft_id")
    private Long aircraftId;

    @Column(name = "aircraft_code", nullable = false, length = 50)
    private String aircraftCode;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "total_seat_capacity", nullable = false)
    private Integer totalSeatCapacity;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    public Long getAircraftId() { return aircraftId; }
    public void setAircraftId(Long aircraftId) { this.aircraftId = aircraftId; }

    public String getAircraftCode() { return aircraftCode; }
    public void setAircraftCode(String aircraftCode) { this.aircraftCode = aircraftCode; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public Integer getTotalSeatCapacity() { return totalSeatCapacity; }
    public void setTotalSeatCapacity(Integer totalSeatCapacity) { this.totalSeatCapacity = totalSeatCapacity; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
