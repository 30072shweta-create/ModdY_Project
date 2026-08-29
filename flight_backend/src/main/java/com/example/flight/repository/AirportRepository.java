package com.example.flight.repository;

import com.example.flight.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AirportRepository extends JpaRepository<Airport, String> {
}