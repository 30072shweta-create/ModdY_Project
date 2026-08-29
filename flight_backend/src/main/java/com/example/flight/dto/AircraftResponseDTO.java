package com.example.flight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AircraftResponseDTO {

    private Long aircraftId;
    private String aircraftCode;
    private String model;
    private String manufacturer;
    private Integer totalSeatCapacity;
    private Boolean active;

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
