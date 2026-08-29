package com.example.flight.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.flight.entity.Aircraft;

@Repository
public interface AircraftRepository extends JpaRepository<Aircraft, Long> {

    Optional<Aircraft> findByAircraftCode(String aircraftCode);

    boolean existsByAircraftCode(String aircraftCode);
}
