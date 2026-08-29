package com.example.flight.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.flight.dto.PassengerRequestDTO;
import com.example.flight.dto.PassengerResponseDTO;
import com.example.flight.entity.Booking;
import com.example.flight.entity.Passenger;
import com.example.flight.repository.PassengerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PassengerService {

    private final PassengerRepository passengerRepository;
    private final BookingAccessService bookingAccessService;

    // ================= ADD PASSENGER =================

    public PassengerResponseDTO addPassenger(
            Long bookingId,
            PassengerRequestDTO request,
            String email) {

        // Verify that this booking belongs to the logged-in user.
        Booking booking = bookingAccessService.getUserBooking(
                bookingId,
                email
        );

        // Create Passenger
        Passenger passenger = new Passenger();
        passenger.setBooking(booking);
        passenger.setFirstName(request.getFirstName());
        passenger.setLastName(request.getLastName());
        passenger.setAge(request.getAge());
        passenger.setGender(request.getGender());
        passenger.setPassportNumber(request.getPassportNumber());

        LocalDate dob = request.getDateOfBirth();
        if (dob == null && request.getAge() != null) {
            dob = LocalDate.now().minusYears(request.getAge());
        }
        passenger.setDateOfBirth(dob);

        passenger.setSeatNumber(request.getSeatNumber());

        Passenger savedPassenger = passengerRepository.save(passenger);
        return convertToResponse(savedPassenger);
    }

    // ================= GET PASSENGERS =================

    public List<PassengerResponseDTO> getPassengers(
            Long bookingId,
            String email) {

        // Verify ownership
        bookingAccessService.getUserBooking(
                bookingId,
                email
        );

        return passengerRepository
                .findByBookingBookingId(bookingId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // ================= CONVERSION =================

    private PassengerResponseDTO convertToResponse(Passenger passenger) {
        PassengerResponseDTO response = new PassengerResponseDTO();
        response.setPassengerId(passenger.getPassengerId());
        response.setBookingId(passenger.getBooking().getBookingId());
        response.setFirstName(passenger.getFirstName());
        response.setLastName(passenger.getLastName());
        response.setDateOfBirth(passenger.getDateOfBirth());
        response.setAge(passenger.getAge());
        response.setGender(passenger.getGender());
        response.setPassportNumber(passenger.getPassportNumber());
        response.setSeatNumber(passenger.getSeatNumber());
        return response;
    }
}