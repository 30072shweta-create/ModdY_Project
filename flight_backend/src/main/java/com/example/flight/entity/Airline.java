package com.example.flight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "airlines")
@NoArgsConstructor
@AllArgsConstructor
public class Airline {

    @Id
    @Column(name = "airline_code", length = 3)
    private String airlineCode;

    @Column(nullable = false, length = 200)
    private String name;

    public String getAirlineCode() { return airlineCode; }
    public void setAirlineCode(String airlineCode) { this.airlineCode = airlineCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}