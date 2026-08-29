package com.example.flight.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.flight.entity.Airline;

public interface AirlineRepository
        extends JpaRepository<Airline, String> {
}