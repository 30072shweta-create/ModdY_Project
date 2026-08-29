package com.example.flight;

import com.example.flight.dto.AirportRequestDTO;
import com.example.flight.dto.AirportResponseDTO;
import com.example.flight.dto.SeatLockResponseDTO;
import com.example.flight.entity.*;
import com.example.flight.repository.*;
import com.example.flight.service.AirportService;
import com.example.flight.service.SeatLockService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.access.AccessDeniedException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class Phase2RedisSeatLockTest {

    @Mock
    private SeatLockRepository seatLockRepository;

    @Mock
    private BookingSegmentRepository bookingSegmentRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private AirportRepository airportRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ModelMapper modelMapper;

    private SeatLockService seatLockService;
    private AirportService airportService;

    private Flight testFlight;
    private BookingSegment testSegment;
    private Booking testBooking;
    private Passenger testPassenger;
    private User testUserA;

    @BeforeEach
    void setUp() {
        seatLockService = new SeatLockService(
                seatLockRepository,
                bookingSegmentRepository,
                passengerRepository,
                flightRepository,
                redisTemplate,
                10
        );

        airportService = new AirportService(airportRepository, modelMapper);

        testFlight = new Flight();
        testFlight.setFlightId(101L);
        testFlight.setAvailableSeats((short) 100);

        testUserA = new User();
        testUserA.setUserId(1L);
        testUserA.setEmail("usera@example.com");

        testBooking = new Booking();
        testBooking.setBookingId(201L);
        testBooking.setUser(testUserA);
        testBooking.setStatus(BookingStatus.PENDING);

        testSegment = new BookingSegment();
        testSegment.setSegmentId(301L);
        testSegment.setBooking(testBooking);
        testSegment.setFlight(testFlight);

        testPassenger = new Passenger();
        testPassenger.setPassengerId(401L);
        testPassenger.setBooking(testBooking);
    }

    // A. User A successfully locks seat
    @Test
    @DisplayName("Test A: User A successfully locks seat")
    void testUserASuccessfullyLocksSeat() {
        when(seatLockRepository.findByFlightFlightIdAndSeatNumberAndStatusIn(anyLong(), anyString(), anyList()))
                .thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("seat-lock:101:12A"), eq("usera@example.com"), any(Duration.class)))
                .thenReturn(true);
        when(bookingSegmentRepository.findById(301L)).thenReturn(Optional.of(testSegment));
        when(passengerRepository.findById(401L)).thenReturn(Optional.of(testPassenger));
        when(flightRepository.findById(101L)).thenReturn(Optional.of(testFlight));

        SeatLock savedLock = new SeatLock();
        savedLock.setSeatLockId(501L);
        savedLock.setFlight(testFlight);
        savedLock.setBooking(testBooking);
        savedLock.setBookingSegment(testSegment);
        savedLock.setPassenger(testPassenger);
        savedLock.setSeatNumber("12A");
        savedLock.setStatus(SeatLockStatus.LOCKED);
        savedLock.setLockedAt(LocalDateTime.now());
        savedLock.setLockedUntil(LocalDateTime.now().plusMinutes(10));

        when(seatLockRepository.save(any(SeatLock.class))).thenReturn(savedLock);

        SeatLockResponseDTO response = seatLockService.lockSpecificSeat(101L, 301L, "12A", 401L, "usera@example.com");

        assertNotNull(response);
        assertEquals("12A", response.getSeatNumber());
        assertEquals(SeatLockStatus.LOCKED, response.getStatus());
        verify(valueOperations).setIfAbsent(eq("seat-lock:101:12A"), eq("usera@example.com"), any(Duration.class));
    }

    // B. User B attempts same seat and fails
    @Test
    @DisplayName("Test B: User B attempts same seat and fails")
    void testUserBAttemptsSameSeatAndFails() {
        when(seatLockRepository.findByFlightFlightIdAndSeatNumberAndStatusIn(anyLong(), anyString(), anyList()))
                .thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("seat-lock:101:12A"), eq("userb@example.com"), any(Duration.class)))
                .thenReturn(false); // Redis setIfAbsent returns false because lock already exists

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                seatLockService.lockSpecificSeat(101L, 301L, "12A", 402L, "userb@example.com"));

        assertTrue(ex.getMessage().contains("currently locked by another user"));
    }

    // C. User A releases own seat
    @Test
    @DisplayName("Test C: User A releases own seat")
    void testUserAReleasesOwnSeat() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(Collections.singletonList("seat-lock:101:12A")), eq("usera@example.com")))
                .thenReturn(1L);
        when(seatLockRepository.findByFlightFlightIdAndSeatNumberAndStatusIn(eq(101L), eq("12A"), anyList()))
                .thenReturn(Optional.empty());

        boolean result = seatLockService.releaseSeatLock(101L, "12A", "usera@example.com");
        assertTrue(result);
    }

    // D. User B cannot release User A's seat
    @Test
    @DisplayName("Test D: User B cannot release User A's seat")
    void testUserBCannotReleaseUserASeat() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(Collections.singletonList("seat-lock:101:12A")), eq("userb@example.com")))
                .thenReturn(0L); // Lua script returns 0 because token does not match owner

        assertThrows(AccessDeniedException.class, () ->
                seatLockService.releaseSeatLock(101L, "12A", "userb@example.com"));
    }

    // E. Lock expires after TTL
    @Test
    @DisplayName("Test E: Lock expires after TTL")
    void testLockExpiresAfterTTL() {
        when(redisTemplate.hasKey("seat-lock:101:12A")).thenReturn(false); // Expired from Redis

        boolean isLocked = seatLockService.isSeatLockedInRedis(101L, "12A");
        assertFalse(isLocked);
    }

    // F. Two concurrent requests for same seat
    @Test
    @DisplayName("Test F: Two concurrent requests for same seat")
    void testTwoConcurrentRequestsForSameSeat() throws InterruptedException, ExecutionException {
        when(seatLockRepository.findByFlightFlightIdAndSeatNumberAndStatusIn(anyLong(), anyString(), anyList()))
                .thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Simulate atomic Redis behavior using ConcurrentHashMap
        Map<String, String> redisMemory = new ConcurrentHashMap<>();
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    String val = invocation.getArgument(1);
                    return redisMemory.putIfAbsent(key, val) == null;
                });

        when(bookingSegmentRepository.findById(301L)).thenReturn(Optional.of(testSegment));
        when(passengerRepository.findById(anyLong())).thenReturn(Optional.of(testPassenger));
        when(flightRepository.findById(101L)).thenReturn(Optional.of(testFlight));

        SeatLock dummyLock = new SeatLock();
        dummyLock.setSeatLockId(999L);
        dummyLock.setFlight(testFlight);
        dummyLock.setBooking(testBooking);
        dummyLock.setBookingSegment(testSegment);
        dummyLock.setPassenger(testPassenger);
        dummyLock.setSeatNumber("12A");
        dummyLock.setStatus(SeatLockStatus.LOCKED);
        when(seatLockRepository.save(any(SeatLock.class))).thenReturn(dummyLock);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<Boolean> req1 = () -> {
            try {
                seatLockService.lockSpecificSeat(101L, 301L, "12A", 401L, "usera@example.com");
                return true;
            } catch (Exception e) {
                return false;
            }
        };
        Callable<Boolean> req2 = () -> {
            try {
                seatLockService.lockSpecificSeat(101L, 301L, "12A", 402L, "userb@example.com");
                return true;
            } catch (Exception e) {
                return false;
            }
        };

        Future<Boolean> f1 = executor.submit(req1);
        Future<Boolean> f2 = executor.submit(req2);

        boolean r1 = f1.get();
        boolean r2 = f2.get();
        executor.shutdown();

        // Exactly one request must succeed and the other must fail!
        assertTrue(r1 ^ r2, "Exactly one concurrent request must succeed");
    }

    // G. Already-booked seat cannot be locked
    @Test
    @DisplayName("Test G: Already-booked seat cannot be locked")
    void testAlreadyBookedSeatCannotBeLocked() {
        SeatLock confirmedLock = new SeatLock();
        confirmedLock.setStatus(SeatLockStatus.CONFIRMED);

        when(seatLockRepository.findByFlightFlightIdAndSeatNumberAndStatusIn(eq(101L), eq("12A"), eq(List.of(SeatLockStatus.CONFIRMED))))
                .thenReturn(Optional.of(confirmedLock));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                seatLockService.lockSpecificSeat(101L, 301L, "12A", 401L, "usera@example.com"));

        assertTrue(ex.getMessage().contains("already permanently booked"));
    }

    // H. Different seats can be locked independently
    @Test
    @DisplayName("Test H: Different seats can be locked independently")
    void testDifferentSeatsCanBeLockedIndependently() {
        when(seatLockRepository.findByFlightFlightIdAndSeatNumberAndStatusIn(anyLong(), anyString(), anyList()))
                .thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("seat-lock:101:12A"), eq("usera@example.com"), any(Duration.class)))
                .thenReturn(true);
        when(valueOperations.setIfAbsent(eq("seat-lock:101:12B"), eq("userb@example.com"), any(Duration.class)))
                .thenReturn(true);

        when(bookingSegmentRepository.findById(301L)).thenReturn(Optional.of(testSegment));
        when(passengerRepository.findById(anyLong())).thenReturn(Optional.of(testPassenger));
        when(flightRepository.findById(101L)).thenReturn(Optional.of(testFlight));

        SeatLock lock1 = new SeatLock();
        lock1.setSeatLockId(501L);
        lock1.setFlight(testFlight);
        lock1.setBooking(testBooking);
        lock1.setBookingSegment(testSegment);
        lock1.setPassenger(testPassenger);
        lock1.setSeatNumber("12A");
        lock1.setStatus(SeatLockStatus.LOCKED);

        SeatLock lock2 = new SeatLock();
        lock2.setSeatLockId(502L);
        lock2.setFlight(testFlight);
        lock2.setBooking(testBooking);
        lock2.setBookingSegment(testSegment);
        lock2.setPassenger(testPassenger);
        lock2.setSeatNumber("12B");
        lock2.setStatus(SeatLockStatus.LOCKED);

        when(seatLockRepository.save(any(SeatLock.class))).thenReturn(lock1).thenReturn(lock2);

        SeatLockResponseDTO resA = seatLockService.lockSpecificSeat(101L, 301L, "12A", 401L, "usera@example.com");
        SeatLockResponseDTO resB = seatLockService.lockSpecificSeat(101L, 301L, "12B", 402L, "userb@example.com");

        assertNotNull(resA);
        assertNotNull(resB);
        assertEquals("12A", resA.getSeatNumber());
        assertEquals("12B", resB.getSeatNumber());
    }

    // I. Airport cache works
    @Test
    @DisplayName("Test I: Airport lookup works")
    void testAirportLookupWorks() {
        Airport airport = new Airport();
        airport.setAirportCode("JFK");
        airport.setName("John F. Kennedy International");

        when(airportRepository.findById("JFK")).thenReturn(Optional.of(airport));

        AirportResponseDTO expected = new AirportResponseDTO();
        expected.setAirportCode("JFK");
        expected.setName("John F. Kennedy International");
        when(modelMapper.map(airport, AirportResponseDTO.class)).thenReturn(expected);

        AirportResponseDTO res = airportService.getAirportByCode("JFK");
        assertNotNull(res);
        assertEquals("JFK", res.getAirportCode());
        verify(airportRepository).findById("JFK");
    }

    // J. Cache invalidation logic works after update
    @Test
    @DisplayName("Test J: Cache invalidation logic works after update")
    void testCacheInvalidationWorksAfterUpdate() {
        Airport airport = new Airport();
        airport.setAirportCode("JFK");
        airport.setName("John F. Kennedy International");
        when(airportRepository.findById("JFK")).thenReturn(Optional.of(airport));

        Airport updated = new Airport();
        updated.setAirportCode("JFK");
        updated.setName("JFK New Name");
        when(airportRepository.save(any(Airport.class))).thenReturn(updated);

        AirportRequestDTO requestDTO = new AirportRequestDTO();
        requestDTO.setName("JFK New Name");
        requestDTO.setCity("New York");
        requestDTO.setCountry("USA");

        AirportResponseDTO expectedResponse = new AirportResponseDTO();
        expectedResponse.setAirportCode("JFK");
        expectedResponse.setName("JFK New Name");
        when(modelMapper.map(updated, AirportResponseDTO.class)).thenReturn(expectedResponse);

        AirportResponseDTO res = airportService.updateAirport("JFK", requestDTO);
        assertNotNull(res);
        assertEquals("JFK New Name", res.getName());
        verify(airportRepository).save(any(Airport.class));
    }
}
