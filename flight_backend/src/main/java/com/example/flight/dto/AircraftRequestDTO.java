package com.example.flight.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AircraftRequestDTO {

    @NotBlank(message = "Aircraft code is required")
    @Size(max = 50, message = "Aircraft code cannot exceed 50 characters")
    private String aircraftCode;

    @NotBlank(message = "Model is required")
    @Size(max = 100, message = "Model cannot exceed 100 characters")
    private String model;

    @Size(max = 100, message = "Manufacturer cannot exceed 100 characters")
    private String manufacturer;

    @NotNull(message = "Total seat capacity is required")
    @Min(value = 1, message = "Total seat capacity must be at least 1")
    private Integer totalSeatCapacity;

    private Boolean active;

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
