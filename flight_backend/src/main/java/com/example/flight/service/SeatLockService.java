package com.example.flight.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight.dto.SeatLockRequestDTO;
import com.example.flight.dto.SeatLockResponseDTO;
import com.example.flight.entity.Booking;
import com.example.flight.entity.BookingSegment;
import com.example.flight.entity.BookingStatus;
import com.example.flight.entity.Flight;
import com.example.flight.entity.Passenger;
import com.example.flight.entity.SeatLock;
import com.example.flight.entity.SeatLockStatus;
import com.example.flight.repository.BookingSegmentRepository;
import com.example.flight.repository.FlightRepository;
import com.example.flight.repository.PassengerRepository;
import com.example.flight.repository.SeatLockRepository;

@Service
public class SeatLockService {

    private final SeatLockRepository seatLockRepository;
    private final BookingSegmentRepository bookingSegmentRepository;
    private final PassengerRepository passengerRepository;
    private final FlightRepository flightRepository;
    private final StringRedisTemplate redisTemplate;
    private final int lockDurationMinutes;

    private static final int TOTAL_ROWS = 30;
    private static final String[] SEAT_LETTERS = {"A", "B", "C", "D"};

    // Lua script for atomic owner-verification & lock deletion
    private static final String UNLOCK_LUA_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else " +
            "return 0 " +
            "end";

    public SeatLockService(
            SeatLockRepository seatLockRepository,
            BookingSegmentRepository bookingSegmentRepository,
            PassengerRepository passengerRepository,
            FlightRepository flightRepository,
            StringRedisTemplate redisTemplate,
            @Value("${seat.lock.duration-minutes:10}") int lockDurationMinutes) {
        this.seatLockRepository = seatLockRepository;
        this.bookingSegmentRepository = bookingSegmentRepository;
        this.passengerRepository = passengerRepository;
        this.flightRepository = flightRepository;
        this.redisTemplate = redisTemplate;
        this.lockDurationMinutes = lockDurationMinutes;
    }

    /**
     * Lock a specific seat using Redis SETNX (setIfAbsent) with TTL.
     */
    @Transactional
    public SeatLockResponseDTO lockSpecificSeat(
            Long flightId,
            Long segmentId,
            String seatNumber,
            Long passengerId,
            String userEmail) {

        // 1. Verify seat is not permanently booked in DB
        List<SeatLockStatus> confirmedStatus = List.of(SeatLockStatus.CONFIRMED);
        boolean permanentlyBooked = seatLockRepository
                .findByFlightFlightIdAndSeatNumberAndStatusIn(flightId, seatNumber, confirmedStatus)
                .isPresent();
        if (permanentlyBooked) {
            throw new RuntimeException("Seat " + seatNumber + " is already permanently booked for flight " + flightId);
        }

        // 2. Atomic Redis lock acquisition (setIfAbsent) with fallback if Redis connection fails
        boolean acquired = false;
        try {
            String lockKey = buildLockKey(flightId, seatNumber);
            Boolean res = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, userEmail, Duration.ofMinutes(lockDurationMinutes));
            acquired = Boolean.TRUE.equals(res);
        } catch (Exception e) {
            List<SeatLockStatus> activeStatuses = List.of(SeatLockStatus.CONFIRMED, SeatLockStatus.LOCKED);
            Optional<SeatLock> existingLock = seatLockRepository
                    .findByFlightFlightIdAndSeatNumberAndStatusIn(flightId, seatNumber, activeStatuses);
            if (existingLock.isPresent()) {
                SeatLock l = existingLock.get();
                if (l.getStatus() == SeatLockStatus.CONFIRMED || (l.getLockedUntil() != null && l.getLockedUntil().isAfter(LocalDateTime.now()))) {
                    acquired = false;
                } else {
                    acquired = true;
                }
            } else {
                acquired = true;
            }
        }

        if (!acquired) {
            throw new RuntimeException("Seat " + seatNumber + " is currently locked by another user");
        }

        // 3. Find domain entities
        BookingSegment segment = bookingSegmentRepository.findById(segmentId)
                .orElseThrow(() -> new RuntimeException("Booking segment not found with ID: " + segmentId));
        Booking booking = segment.getBooking();
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found with ID: " + passengerId));
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Flight not found with ID: " + flightId));

        // 4. Save or update SeatLock in DB
        LocalDateTime now = LocalDateTime.now();
        SeatLock seatLock = new SeatLock();
        seatLock.setFlight(flight);
        seatLock.setBooking(booking);
        seatLock.setBookingSegment(segment);
        seatLock.setPassenger(passenger);
        seatLock.setSeatNumber(seatNumber);
        seatLock.setStatus(SeatLockStatus.LOCKED);
        seatLock.setLockedAt(now);
        seatLock.setLockedUntil(now.plusMinutes(lockDurationMinutes));

        passenger.setSeatNumber(seatNumber);
        passengerRepository.save(passenger);

        SeatLock savedLock = seatLockRepository.save(seatLock);
        return convertToResponse(savedLock);
    }

    /**
     * Automatically allocate and lock the first available seat for a passenger.
     */
    @Transactional
    public SeatLockResponseDTO allocateAndLockSeat(SeatLockRequestDTO request, String email) {
        BookingSegment segment = bookingSegmentRepository.findById(request.getSegmentId())
                .orElseThrow(() -> new RuntimeException("Booking segment not found with ID: " + request.getSegmentId()));

        Booking booking = segment.getBooking();
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Seat can only be allocated for a pending booking");
        }

        if (booking.getUser() != null && booking.getUser().getEmail() != null &&
                !booking.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("You are not allowed to modify this booking");
        }

        Passenger passenger = passengerRepository.findById(request.getPassengerId())
                .orElseThrow(() -> new RuntimeException("Passenger not found with ID: " + request.getPassengerId()));

        if (!passenger.getBooking().getBookingId().equals(booking.getBookingId())) {
            throw new RuntimeException("Passenger does not belong to this booking");
        }

        Flight flight = segment.getFlight();
        if (flight.getAvailableSeats() == null || flight.getAvailableSeats() <= 0) {
            throw new RuntimeException("No seats available for this flight");
        }

        // Find and lock available seat atomically
        String seatNumber = findAvailableSeat(flight.getFlightId());
        return lockSpecificSeat(flight.getFlightId(), segment.getSegmentId(), seatNumber, passenger.getPassengerId(), email);
    }

    /**
     * Release seat lock using atomic Lua script (owner-only) with DB status update.
     */
    @Transactional
    public boolean releaseSeatLock(Long flightId, String seatNumber, String userEmail) {
        try {
            String lockKey = buildLockKey(flightId, seatNumber);
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_LUA_SCRIPT, Long.class);
            redisTemplate.execute(script, Collections.singletonList(lockKey), userEmail);
        } catch (Exception ignored) {}

        // Update DB record if exists
        Optional<SeatLock> lockOpt = seatLockRepository.findByFlightFlightIdAndSeatNumberAndStatusIn(
                flightId, seatNumber, List.of(SeatLockStatus.LOCKED));
        lockOpt.ifPresent(lock -> {
            lock.setStatus(SeatLockStatus.RELEASED);
            seatLockRepository.save(lock);
            if (lock.getPassenger() != null) {
                lock.getPassenger().setSeatNumber(null);
                passengerRepository.save(lock.getPassenger());
            }
        });
        return true;
    }

    public boolean isSeatLockedInRedis(Long flightId, String seatNumber) {
        try {
            String lockKey = buildLockKey(flightId, seatNumber);
            Boolean hasKey = redisTemplate.hasKey(lockKey);
            return Boolean.TRUE.equals(hasKey);
        } catch (Exception e) {
            return false;
        }
    }

    public String getSeatLockOwner(Long flightId, String seatNumber) {
        try {
            String lockKey = buildLockKey(flightId, seatNumber);
            return redisTemplate.opsForValue().get(lockKey);
        } catch (Exception e) {
            return null;
        }
    }

    public String buildLockKey(Long flightId, String seatNumber) {
        return "seat-lock:" + flightId + ":" + seatNumber;
    }

    private String findAvailableSeat(Long flightId) {
        List<SeatLockStatus> confirmedStatus = List.of(SeatLockStatus.CONFIRMED);

        for (int row = 1; row <= TOTAL_ROWS; row++) {
            for (String letter : SEAT_LETTERS) {
                String seatNumber = row + letter;

                boolean DBConfirmed = seatLockRepository
                        .findByFlightFlightIdAndSeatNumberAndStatusIn(flightId, seatNumber, confirmedStatus)
                        .isPresent();

                boolean redisLocked = isSeatLockedInRedis(flightId, seatNumber);

                if (!DBConfirmed && !redisLocked) {
                    return seatNumber;
                }
            }
        }
        throw new RuntimeException("No seat available for this flight");
    }

    public List<SeatLockResponseDTO> getBookingSeatLocks(Long bookingId) {
        return seatLockRepository.findByBookingBookingId(bookingId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    public List<Map<String, Object>> getOccupiedSeatsForFlight(Long flightId, String currentUserEmail) {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> processedSeats = new HashSet<>();

        List<SeatLockStatus> activeStatuses = List.of(SeatLockStatus.CONFIRMED, SeatLockStatus.LOCKED);
        List<SeatLock> dbLocks = seatLockRepository.findByFlightFlightIdAndStatusIn(flightId, activeStatuses);

        for (SeatLock lock : dbLocks) {
            String seatNum = lock.getSeatNumber();
            boolean isRedisLocked = isSeatLockedInRedis(flightId, seatNum);

            if (lock.getStatus() == SeatLockStatus.CONFIRMED) {
                processedSeats.add(seatNum);
                Map<String, Object> map = new HashMap<>();
                map.put("seatNumber", seatNum);
                map.put("status", "BOOKED");
                map.put("owner", lock.getBooking() != null && lock.getBooking().getUser() != null ? lock.getBooking().getUser().getEmail() : "booked");
                map.put("passengerId", lock.getPassenger() != null ? lock.getPassenger().getPassengerId() : null);
                map.put("bookingId", lock.getBooking() != null ? lock.getBooking().getBookingId() : null);
                result.add(map);
            } else if (lock.getStatus() == SeatLockStatus.LOCKED && (isRedisLocked || (lock.getLockedUntil() != null && lock.getLockedUntil().isAfter(LocalDateTime.now())))) {
                processedSeats.add(seatNum);
                String redisOwner = getSeatLockOwner(flightId, seatNum);
                Map<String, Object> map = new HashMap<>();
                map.put("seatNumber", seatNum);
                map.put("status", "LOCKED");
                map.put("owner", redisOwner != null ? redisOwner : (lock.getBooking() != null && lock.getBooking().getUser() != null ? lock.getBooking().getUser().getEmail() : "locked"));
                map.put("passengerId", lock.getPassenger() != null ? lock.getPassenger().getPassengerId() : null);
                map.put("bookingId", lock.getBooking() != null ? lock.getBooking().getBookingId() : null);
                result.add(map);
            }
        }

        try {
            Set<String> redisKeys = redisTemplate.keys("seat-lock:" + flightId + ":*");
            if (redisKeys != null) {
                for (String key : redisKeys) {
                    String seatNum = key.substring(key.lastIndexOf(":") + 1);
                    if (!processedSeats.contains(seatNum)) {
                        String owner = redisTemplate.opsForValue().get(key);
                        Map<String, Object> map = new HashMap<>();
                        map.put("seatNumber", seatNum);
                        map.put("status", "LOCKED");
                        map.put("owner", owner != null ? owner : "locked");
                        result.add(map);
                    }
                }
            }
        } catch (Exception ignored) {}

        return result;
    }

    private SeatLockResponseDTO convertToResponse(SeatLock seatLock) {
        SeatLockResponseDTO response = new SeatLockResponseDTO();
        response.setSeatLockId(seatLock.getSeatLockId());
        response.setBookingId(seatLock.getBooking().getBookingId());
        response.setSegmentId(seatLock.getBookingSegment().getSegmentId());
        response.setFlightId(seatLock.getFlight().getFlightId());
        response.setPassengerId(seatLock.getPassenger().getPassengerId());
        response.setSeatNumber(seatLock.getSeatNumber());
        response.setStatus(seatLock.getStatus());
        response.setLockedAt(seatLock.getLockedAt());
        response.setLockedUntil(seatLock.getLockedUntil());
        return response;
    }
}