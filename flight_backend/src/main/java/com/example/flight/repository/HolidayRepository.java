package com.example.flight.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.flight.entity.Holiday;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    Optional<Holiday> findByHolidayDateAndActiveTrue(LocalDate holidayDate);

    boolean existsByHolidayDate(LocalDate holidayDate);
}
