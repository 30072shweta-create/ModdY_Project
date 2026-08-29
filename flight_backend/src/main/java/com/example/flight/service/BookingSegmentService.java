package com.example.flight.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight.dto.BookingSegmentRequestDTO;
import com.example.flight.dto.BookingSegmentResponseDTO;
import com.example.flight.entity.Booking;
import com.example.flight.entity.BookingSegment;
import com.example.flight.entity.BookingStatus;
import com.example.flight.entity.Flight;
import com.example.flight.repository.BookingSegmentRepository;
import com.example.flight.repository.FlightRepository;
import com.example.flight.entity.BookingStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingSegmentService {

    private final BookingSegmentRepository
            bookingSegmentRepository;

    private final FlightRepository flightRepository;

    private final BookingAccessService
            bookingAccessService;


    // ================= ADD SEGMENT =================

    @Transactional
    public BookingSegmentResponseDTO addSegment(
            Long bookingId,
            BookingSegmentRequestDTO request,
            String email) {


        // 1. Verify booking ownership

        Booking booking =
                bookingAccessService.getUserBooking(
                        bookingId,
                        email
                );


        // 2. Only pending bookings can
        //    be modified

       if (booking.getStatus() != BookingStatus.PENDING) {

    throw new RuntimeException(
            "Only pending bookings can be modified"
    );
}
        

        // 3. Find flight

        Flight flight =
                flightRepository.findById(
                        request.getFlightId()
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Flight not found with ID: "
                                + request.getFlightId()
                        )
                );


        // 4. Create segment

        BookingSegment segment =
                new BookingSegment();

        segment.setBooking(booking);

        segment.setFlight(flight);

        segment.setSegmentOrder(
                request.getSegmentOrder()
        );


        // 5. Save

        BookingSegment savedSegment =
                bookingSegmentRepository.save(
                        segment
                );


        return convertToResponse(
                savedSegment
        );
    }


    // ================= GET SEGMENTS =================

    @Transactional(readOnly = true)
    public List<BookingSegmentResponseDTO>
            getSegments(
                    Long bookingId,
                    String email) {

        // Verify ownership

        bookingAccessService.getUserBooking(
                bookingId,
                email
        );


        return bookingSegmentRepository
                .findByBookingBookingIdOrderBySegmentOrderAsc(
                        bookingId
                )
                .stream()
                .map(this::convertToResponse)
                .toList();
    }


    // ================= CONVERSION =================

    private BookingSegmentResponseDTO
            convertToResponse(
                    BookingSegment segment) {

        BookingSegmentResponseDTO response =
                new BookingSegmentResponseDTO();


        response.setSegmentId(
                segment.getSegmentId()
        );

        response.setBookingId(
                segment.getBooking()
                        .getBookingId()
        );

        response.setFlightId(
                segment.getFlight()
                        .getFlightId()
        );

        response.setAirlineCode(
                segment.getFlight()
                        .getAirline()
                        .getAirlineCode()
        );

        response.setFromAirport(
                segment.getFlight()
                        .getFromAirport()
                        .getAirportCode()
        );

        response.setToAirport(
                segment.getFlight()
                        .getToAirport()
                        .getAirportCode()
        );

        response.setDepartureTs(
                segment.getFlight()
                        .getDepartureTs()
        );

        response.setArrivalTs(
                segment.getFlight()
                        .getArrivalTs()
        );

        response.setSegmentOrder(
                segment.getSegmentOrder()
        );

        return response;
    }
}