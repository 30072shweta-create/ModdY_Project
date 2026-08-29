package com.example.flight.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FlightSearchRequestDTO {

    private String source;
    private String destination;
    private LocalDate date;
    private String airline;
    private String flightNumber;
    private Short stops;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer maxDuration;
    private String sortBy;
    private String sortDirection;
    private Integer page;
    private Integer size;

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getAirline() { return airline; }
    public void setAirline(String airline) { this.airline = airline; }

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public Short getStops() { return stops; }
    public void setStops(Short stops) { this.stops = stops; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public Integer getMaxDuration() { return maxDuration; }
    public void setMaxDuration(Integer maxDuration) { this.maxDuration = maxDuration; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public String getSortDirection() { return sortDirection; }
    public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }

    public Integer getPage() { return page != null ? page : 0; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size != null ? size : 10; }
    public void setSize(Integer size) { this.size = size; }
}