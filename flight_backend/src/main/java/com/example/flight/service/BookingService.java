package com.example.flight.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight.dto.BookingRequestDTO;
import com.example.flight.dto.BookingResponseDTO;
import com.example.flight.dto.BookingSegmentResponseDTO;
import com.example.flight.dto.FareBreakdownDTO;
import com.example.flight.dto.FareCalculationRequestDTO;
import com.example.flight.dto.PassengerResponseDTO;
import com.example.flight.entity.Booking;
import com.example.flight.entity.BookingSegment;
import com.example.flight.entity.BookingStatus;
import com.example.flight.entity.CabinClass;
import com.example.flight.entity.Flight;
import com.example.flight.entity.PaymentStatus;
import com.example.flight.entity.SeatLock;
import com.example.flight.entity.SeatLockStatus;
import com.example.flight.entity.User;
import com.example.flight.exception.SeatLockExpiredException;
import com.example.flight.repository.BookingRepository;
import com.example.flight.repository.BookingSegmentRepository;
import com.example.flight.repository.FlightPricingRepository;
import com.example.flight.repository.FlightRepository;
import com.example.flight.repository.PassengerRepository;
import com.example.flight.repository.SeatLockRepository;
import com.example.flight.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final FlightRepository flightRepository;
    private final FlightPricingRepository flightPricingRepository;
    private final BookingSegmentRepository bookingSegmentRepository;
    private final PassengerRepository passengerRepository;
    private final SeatLockRepository seatLockRepository;
    private final SeatLockService seatLockService;
    private final TripValidationService tripValidationService;
    private final FareCalculationService fareCalculationService;
    private final CouponService couponService;
    private final NotificationService notificationService;

    // =========================================================
    // GET ALL BOOKINGS
    // =========================================================
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getAllBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // =========================================================
    // GET BOOKING BY ID
    // =========================================================
    @Transactional(readOnly = true)
    public BookingResponseDTO getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));

        return convertToResponse(booking);
    }

    // =========================================================
    // GET BOOKING BY CODE
    // =========================================================
    @Transactional(readOnly = true)
    public BookingResponseDTO getBookingByCode(String bookingCode) {
        if (bookingCode == null || bookingCode.isBlank()) {
            throw new RuntimeException("Booking code is required");
        }
        Booking booking = bookingRepository.findByBookingCodeIgnoreCaseWithDetails(bookingCode.trim())
                .or(() -> bookingRepository.findByBookingCodeIgnoreCase(bookingCode.trim()))
                .orElseThrow(() -> new RuntimeException("Booking not found with code: " + bookingCode.trim()));

        return convertToResponse(booking);
    }

    // =========================================================
    // GET BOOKINGS BY USER
    // =========================================================
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getBookingsByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with ID: " + userId);
        }

        return bookingRepository.findByUserUserIdWithDetails(userId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // =========================================================
    // GET BOOKINGS BY FLIGHT
    // =========================================================
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getBookingsByFlight(Long flightId) {
        if (!flightRepository.existsById(flightId)) {
            throw new RuntimeException("Flight not found with ID: " + flightId);
        }

        return bookingRepository.findByFlightFlightId(flightId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // =========================================================
    // CREATE BOOKING (WITH SERVER-SIDE FARE CALCULATION & PRICE SNAPSHOT)
    // =========================================================
    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO dto, String email) {
        // 1. Get logged-in user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Validate flight list
        if (dto.getFlightIds() == null || dto.getFlightIds().isEmpty()) {
            throw new RuntimeException("At least one flight is required");
        }

        // 3. Find all selected flights
        List<Flight> flights = dto.getFlightIds()
                .stream()
                .map(flightId -> flightRepository.findById(flightId)
                        .orElseThrow(() -> new RuntimeException("Flight not found with ID: " + flightId)))
                .toList();

        // 4. Validate trip
        tripValidationService.validateTrip(flights);

        // 5. Create Booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setFlight(flights.get(0));
        booking.setBookingCode(generateBookingCode());
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setBookingTs(LocalDateTime.now());

        // 6. Calculate total fare & create segments using FareCalculationService
        BigDecimal totalAmount = BigDecimal.ZERO;
        int segmentOrder = 1;

        for (Flight flight : flights) {
            if (flight.getAvailableSeats() == null || flight.getAvailableSeats() <= 0) {
                throw new RuntimeException("No seats available for flight ID: " + flight.getFlightId());
            }

            FareCalculationRequestDTO calcReq = FareCalculationRequestDTO.builder()
                    .flightId(flight.getFlightId())
                    .cabinClass(dto.getCabinClass())
                    .travelDate(dto.getTravelDate() != null ? dto.getTravelDate() : flight.getDepartureTs())
                    .baggageCount(dto.getBaggageCount())
                    .baggageKg(dto.getBaggageKg())
                    .couponCode(dto.getCouponCode())
                    .build();

            FareBreakdownDTO breakdown = fareCalculationService.calculateFare(calcReq);

            totalAmount = totalAmount.add(breakdown.getFinalPrice());

            BookingSegment segment = new BookingSegment();
            segment.setBooking(booking);
            segment.setFlight(flight);
            segment.setSegmentOrder(segmentOrder);

            booking.getSegments().add(segment);
            segmentOrder++;
        }

        if (dto.getCouponCode() != null && !dto.getCouponCode().trim().isEmpty()) {
            couponService.incrementCouponUsage(dto.getCouponCode());
        }

        booking.setTotalAmount(totalAmount);
        Booking savedBooking = bookingRepository.save(booking);

        return convertToResponse(savedBooking);
    }

    // =========================================================
    // CONFIRM BOOKING (WITH REDIS SEAT-LOCK VALIDATION)
    // =========================================================
    @Transactional
    public BookingResponseDTO confirmBooking(Long bookingId, String userEmail) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Booking is not in PENDING status");
        }

        if (booking.getUser() != null && booking.getUser().getEmail() != null
                && !booking.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("You are not allowed to confirm this booking");
        }

        // Validate Redis Seat Locks for all locked seats in this booking
        List<SeatLock> seatLocks = seatLockRepository.findByBookingBookingId(bookingId);
        for (SeatLock lock : seatLocks) {
            if (lock.getStatus() == SeatLockStatus.LOCKED) {
                Long flightId = lock.getFlight().getFlightId();
                String seatNumber = lock.getSeatNumber();

                boolean isLocked = seatLockService.isSeatLockedInRedis(flightId, seatNumber);
                String owner = seatLockService.getSeatLockOwner(flightId, seatNumber);

                if (!isLocked || owner == null || !owner.equalsIgnoreCase(userEmail)) {
                    throw new SeatLockExpiredException("Seat lock for seat " + seatNumber + " on flight " + flightId + " has expired or is invalid");
                }

                lock.setStatus(SeatLockStatus.CONFIRMED);
                seatLockRepository.save(lock);

                if (lock.getPassenger() != null) {
                    lock.getPassenger().setSeatNumber(seatNumber);
                    passengerRepository.save(lock.getPassenger());
                }
            }
        }

        // Update Booking & Flight Seats
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.SUCCESS);

        for (BookingSegment segment : booking.getSegments()) {
            Flight flight = segment.getFlight();
            if (flight.getAvailableSeats() != null && flight.getAvailableSeats() > 0) {
                flight.setAvailableSeats((short) (flight.getAvailableSeats() - 1));
                flightRepository.save(flight);
            }
        }

        Booking saved = bookingRepository.save(booking);
        notificationService.sendBookingConfirmationNotification(saved);
        return convertToResponse(saved);
    }

    // =========================================================
    // ENTITY -> RESPONSE DTO
    // =========================================================
    private BookingResponseDTO convertToResponse(Booking booking) {
        BookingResponseDTO response = new BookingResponseDTO();
        response.setBookingId(booking.getBookingId());

        if (booking.getUser() != null) {
            response.setUserId(booking.getUser().getUserId());
        }

        List<BookingSegmentResponseDTO> segmentDTOs = new ArrayList<>();
        if (booking.getSegments() != null && !booking.getSegments().isEmpty()) {
            for (BookingSegment segment : booking.getSegments()) {
                BookingSegmentResponseDTO dto = new BookingSegmentResponseDTO();
                dto.setSegmentId(segment.getSegmentId());
                dto.setBookingId(booking.getBookingId());
                dto.setSegmentOrder(segment.getSegmentOrder());
                dto.setCabinClass(CabinClass.ECONOMY.name());

                Flight f = segment.getFlight() != null ? segment.getFlight() : booking.getFlight();
                if (f != null) {
                    dto.setFlightId(f.getFlightId());
                    dto.setFlightNumber(f.getFlightNumber());
                    if (f.getAirline() != null) {
                        dto.setAirlineCode(f.getAirline().getAirlineCode());
                        dto.setAirlineName(f.getAirline().getName());
                    }
                    if (f.getFromAirport() != null) {
                        dto.setFromAirport(f.getFromAirport().getAirportCode());
                    }
                    if (f.getToAirport() != null) {
                        dto.setToAirport(f.getToAirport().getAirportCode());
                    }
                    dto.setDepartureTs(f.getDepartureTs());
                    dto.setArrivalTs(f.getArrivalTs());
                    dto.setPrice(f.getBasePrice());
                }
                segmentDTOs.add(dto);
            }
        } else if (booking.getFlight() != null) {
            Flight f = booking.getFlight();
            BookingSegmentResponseDTO dto = new BookingSegmentResponseDTO();
            dto.setBookingId(booking.getBookingId());
            dto.setSegmentOrder(1);
            dto.setCabinClass(CabinClass.ECONOMY.name());
            dto.setFlightId(f.getFlightId());
            dto.setFlightNumber(f.getFlightNumber());
            if (f.getAirline() != null) {
                dto.setAirlineCode(f.getAirline().getAirlineCode());
                dto.setAirlineName(f.getAirline().getName());
            }
            if (f.getFromAirport() != null) {
                dto.setFromAirport(f.getFromAirport().getAirportCode());
            }
            if (f.getToAirport() != null) {
                dto.setToAirport(f.getToAirport().getAirportCode());
            }
            dto.setDepartureTs(f.getDepartureTs());
            dto.setArrivalTs(f.getArrivalTs());
            dto.setPrice(f.getBasePrice());
            segmentDTOs.add(dto);
        }
        response.setSegments(segmentDTOs);

        response.setBookingCode(booking.getBookingCode());
        response.setStatus(booking.getStatus());
        response.setPaymentStatus(booking.getPaymentStatus());
        response.setTotalAmount(booking.getTotalAmount());
        response.setBookingTs(booking.getBookingTs());

        // Lookup seat locks for this booking to ensure seat number is never lost
        Map<Long, String> passengerSeatMap = new HashMap<>();
        try {
            List<SeatLock> locks = seatLockRepository.findByBookingBookingId(booking.getBookingId());
            if (locks != null) {
                for (SeatLock sl : locks) {
                    if (sl.getPassenger() != null && sl.getSeatNumber() != null &&
                            (sl.getStatus() == SeatLockStatus.CONFIRMED || sl.getStatus() == SeatLockStatus.LOCKED)) {
                        passengerSeatMap.put(sl.getPassenger().getPassengerId(), sl.getSeatNumber());
                    }
                }
            }
        } catch (Exception ignored) {}

        response.setPassengers(passengerRepository.findByBookingBookingId(booking.getBookingId())
                .stream()
                .map(passenger -> {
                    PassengerResponseDTO passengerDto = new PassengerResponseDTO();
                    passengerDto.setPassengerId(passenger.getPassengerId());
                    passengerDto.setBookingId(passenger.getBooking().getBookingId());
                    passengerDto.setFirstName(passenger.getFirstName());
                    passengerDto.setLastName(passenger.getLastName());
                    passengerDto.setAge(passenger.getAge());
                    passengerDto.setGender(passenger.getGender());
                    passengerDto.setDateOfBirth(passenger.getDateOfBirth());
                    passengerDto.setPassportNumber(passenger.getPassportNumber());

                    String seat = passenger.getSeatNumber();
                    if ((seat == null || seat.isBlank()) && passengerSeatMap.containsKey(passenger.getPassengerId())) {
                        seat = passengerSeatMap.get(passenger.getPassengerId());
                    }
                    passengerDto.setSeatNumber(seat);
                    return passengerDto;
                })
                .toList());

        return response;
    }

    private String generateBookingCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        } while (bookingRepository.existsByBookingCode(code));
        return code;
    }
}
