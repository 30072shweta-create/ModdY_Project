package com.example.flight;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.example.flight.dto.AircraftRequestDTO;
import com.example.flight.dto.AircraftResponseDTO;
import com.example.flight.dto.BookingRequestDTO;
import com.example.flight.dto.BookingResponseDTO;
import com.example.flight.dto.FlightRequestDTO;
import com.example.flight.dto.FlightResponseDTO;
import com.example.flight.dto.SeatLockResponseDTO;
import com.example.flight.entity.Aircraft;
import com.example.flight.entity.Airline;
import com.example.flight.entity.Airport;
import com.example.flight.entity.Booking;
import com.example.flight.entity.BookingStatus;
import com.example.flight.entity.CabinClass;
import com.example.flight.entity.Flight;
import com.example.flight.entity.FlightPricing;
import com.example.flight.entity.FlightStatus;
import com.example.flight.entity.Passenger;
import com.example.flight.entity.PaymentStatus;
import com.example.flight.entity.User;
import com.example.flight.exception.AircraftNotFoundException;
import com.example.flight.exception.InvalidAircraftException;
import com.example.flight.exception.SeatLockExpiredException;
import com.example.flight.repository.AircraftRepository;
import com.example.flight.repository.AirlineRepository;
import com.example.flight.repository.AirportRepository;
import com.example.flight.repository.BookingRepository;
import com.example.flight.repository.BookingSegmentRepository;
import com.example.flight.repository.FlightPricingRepository;
import com.example.flight.repository.FlightRepository;
import com.example.flight.repository.PassengerRepository;
import com.example.flight.repository.SeatLockRepository;
import com.example.flight.repository.UserRepository;
import com.example.flight.service.AircraftService;
import com.example.flight.service.BookingService;
import com.example.flight.service.FlightService;
import com.example.flight.service.SeatLockService;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
public class Phase3DomainAndTransactionTest {

    @Autowired
    private AircraftService aircraftService;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private FlightService flightService;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private AirlineRepository airlineRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private FlightPricingRepository flightPricingRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingSegmentRepository bookingSegmentRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private SeatLockService seatLockService;

    @Autowired
    private SeatLockRepository seatLockRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Autowired
    private EntityManager entityManager;

    private Aircraft testAircraft;
    private Airline testAirline;
    private Airport testFromAirport;
    private Airport testToAirport;
    private Flight testFlight;
    private User testUser;
    private ConcurrentHashMap<String, String> redisMemory;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        redisMemory = new ConcurrentHashMap<>();

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenAnswer(inv -> {
                    String key = inv.getArgument(0);
                    String val = inv.getArgument(1);
                    return redisMemory.putIfAbsent(key, val) == null;
                });
        lenient().when(valueOperations.get(anyString()))
                .thenAnswer(inv -> redisMemory.get(inv.getArgument(0)));
        lenient().when(redisTemplate.hasKey(anyString()))
                .thenAnswer(inv -> redisMemory.containsKey(inv.getArgument(0)));
        lenient().when(redisTemplate.delete(anyString()))
                .thenAnswer(inv -> redisMemory.remove(inv.getArgument(0)) != null);

        seatLockRepository.deleteAll();
        bookingSegmentRepository.deleteAll();
        passengerRepository.deleteAll();
        bookingRepository.deleteAll();
        flightPricingRepository.deleteAll();
        flightRepository.deleteAll();
        aircraftRepository.deleteAll();
        userRepository.findByEmail("phase3_user@example.com").ifPresent(userRepository::delete);

        // Setup User
        testUser = User.builder()
                .email("phase3_user@example.com")
                .passwordHash("hashedpass")
                .firstName("Phase3")
                .lastName("User")
                .role("USER")
                .emailVerified(true)
                .build();
        testUser = userRepository.save(testUser);

        // Setup Aircraft
        testAircraft = Aircraft.builder()
                .aircraftCode("A320-TEST")
                .model("Airbus A320")
                .manufacturer("Airbus")
                .totalSeatCapacity(180)
                .active(true)
                .build();
        testAircraft = aircraftRepository.save(testAircraft);

        // Setup Airline & Airports
        testAirline = airlineRepository.findById("AI").orElseGet(() ->
                airlineRepository.save(new Airline("AI", "Air India"))
        );

        testFromAirport = airportRepository.findById("DEL").orElseGet(() ->
                airportRepository.save(new Airport("DEL", "Indira Gandhi Int", "Delhi", "India"))
        );

        testToAirport = airportRepository.findById("BOM").orElseGet(() ->
                airportRepository.save(new Airport("BOM", "Chhatrapati Shivaji", "Mumbai", "India"))
        );

        // Setup Flight
        testFlight = new Flight();
        testFlight.setFlightNumber("AI-301");
        testFlight.setAirline(testAirline);
        testFlight.setFromAirport(testFromAirport);
        testFlight.setToAirport(testToAirport);
        testFlight.setAircraft(testAircraft);
        testFlight.setDepartureTs(LocalDateTime.now().plusDays(1));
        testFlight.setArrivalTs(LocalDateTime.now().plusDays(1).plusHours(2));
        testFlight.setStops((short) 0);
        testFlight.setBasePrice(new BigDecimal("5000.00"));
        testFlight.setAvailableSeats((short) 180);
        testFlight.setDurationMins(120);
        testFlight.setStatus(FlightStatus.SCHEDULED);
        testFlight = flightRepository.save(testFlight);

        // Setup Pricing
        FlightPricing pricing = new FlightPricing();
        pricing.setFlight(testFlight);
        pricing.setSeatClass(CabinClass.ECONOMY);
        pricing.setBaseFare(new BigDecimal("4000.00"));
        pricing.setTaxes(new BigDecimal("500.00"));
        pricing.setConvenienceFee(new BigDecimal("100.00"));
        flightPricingRepository.save(pricing);
    }

    @Test
    @DisplayName("1. Aircraft Domain CRUD - Create, Retrieve, Update, Deactivate")
    void testAircraftCRUD() {
        AircraftRequestDTO request = AircraftRequestDTO.builder()
                .aircraftCode("B737-MAX")
                .model("Boeing 737 MAX 8")
                .manufacturer("Boeing")
                .totalSeatCapacity(189)
                .active(true)
                .build();

        AircraftResponseDTO created = aircraftService.createAircraft(request);
        assertNotNull(created.getAircraftId());
        assertEquals("B737-MAX", created.getAircraftCode());
        assertEquals(Integer.valueOf(189), created.getTotalSeatCapacity());

        // Retrieve
        AircraftResponseDTO retrieved = aircraftService.getAircraftById(created.getAircraftId());
        assertEquals("Boeing 737 MAX 8", retrieved.getModel());

        // Update
        request.setModel("Boeing 737 MAX 9");
        AircraftResponseDTO updated = aircraftService.updateAircraft(created.getAircraftId(), request);
        assertEquals("Boeing 737 MAX 9", updated.getModel());

        // Deactivate (Delete)
        aircraftService.deleteAircraft(created.getAircraftId());
        AircraftResponseDTO deactivated = aircraftService.getAircraftById(created.getAircraftId());
        assertFalse(deactivated.getActive());
    }

    @Test
    @DisplayName("2. Aircraft Validation - Duplicate Code Rejection")
    void testDuplicateAircraftCodeRejection() {
        AircraftRequestDTO request = AircraftRequestDTO.builder()
                .aircraftCode("A320-TEST") // Duplicate
                .model("Airbus A320")
                .totalSeatCapacity(180)
                .build();

        assertThrows(InvalidAircraftException.class, () -> aircraftService.createAircraft(request));
    }

    @Test
    @DisplayName("3. Flight to Aircraft Relationship & Validation")
    void testFlightAircraftAssociationAndValidation() {
        FlightRequestDTO flightRequest = new FlightRequestDTO();
        flightRequest.setFlightNumber("AI-302");
        flightRequest.setAirlineCode("AI");
        flightRequest.setFromAirport("DEL");
        flightRequest.setToAirport("BOM");
        flightRequest.setAircraftId(testAircraft.getAircraftId());
        flightRequest.setDepartureTs(LocalDateTime.now().plusDays(2));
        flightRequest.setArrivalTs(LocalDateTime.now().plusDays(2).plusHours(2));
        flightRequest.setStops((short) 0);
        flightRequest.setBasePrice(new BigDecimal("6000.00"));
        flightRequest.setAvailableSeats((short) 180);
        flightRequest.setStatus(FlightStatus.SCHEDULED);

        FlightResponseDTO response = flightService.addFlight(flightRequest);
        assertNotNull(response.getFlightId());
        assertEquals(testAircraft.getAircraftId(), response.getAircraftId());
        assertEquals("A320-TEST", response.getAircraftCode());

        // Test non-existent aircraft ID rejection
        flightRequest.setFlightNumber("AI-303");
        flightRequest.setAircraftId(99999L);
        assertThrows(AircraftNotFoundException.class, () -> flightService.addFlight(flightRequest));

        // Test inactive aircraft ID rejection
        testAircraft.setActive(false);
        aircraftRepository.save(testAircraft);

        flightRequest.setAircraftId(testAircraft.getAircraftId());
        assertThrows(InvalidAircraftException.class, () -> flightService.addFlight(flightRequest));
    }

    @Test
    @DisplayName("4. ADMIN Security Context Authorization on Aircraft Management")
    void testAdminAircraftAuthorization() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        AircraftRequestDTO request = AircraftRequestDTO.builder()
                .aircraftCode("A350-900")
                .model("Airbus A350-900")
                .totalSeatCapacity(300)
                .active(true)
                .build();

        AircraftResponseDTO response = aircraftService.createAircraft(request);
        assertNotNull(response.getAircraftId());
    }

    @Test
    @DisplayName("5. Transactional Multi-step Booking Creation")
    void testTransactionalBookingCreation() {
        BookingRequestDTO bookingRequest = new BookingRequestDTO();
        bookingRequest.setFlightIds(List.of(testFlight.getFlightId()));
        bookingRequest.setCabinClass(CabinClass.ECONOMY);

        BookingResponseDTO bookingResponse = bookingService.createBooking(bookingRequest, testUser.getEmail());
        assertNotNull(bookingResponse.getBookingId());
        assertEquals(BookingStatus.PENDING, bookingResponse.getStatus());
        assertEquals(PaymentStatus.PENDING, bookingResponse.getPaymentStatus());

        Optional<Booking> dbBooking = bookingRepository.findByIdWithDetails(bookingResponse.getBookingId());
        assertTrue(dbBooking.isPresent());
        assertEquals(1, dbBooking.get().getSegments().size());
    }

    @Test
    @DisplayName("6. Transactional Booking Confirmation with Valid Redis Seat Lock")
    void testBookingConfirmationWithRedisSeatLock() {
        // Create booking
        BookingRequestDTO bookingRequest = new BookingRequestDTO();
        bookingRequest.setFlightIds(List.of(testFlight.getFlightId()));
        bookingRequest.setCabinClass(CabinClass.ECONOMY);
        BookingResponseDTO bookingResponse = bookingService.createBooking(bookingRequest, testUser.getEmail());

        // Create Passenger
        Booking bookingEntity = bookingRepository.findByIdWithDetails(bookingResponse.getBookingId()).orElseThrow();
        Passenger passenger = new Passenger();
        passenger.setBooking(bookingEntity);
        passenger.setFirstName("John");
        passenger.setLastName("Doe");
        passenger.setSeatNumber("12A");
        passenger = passengerRepository.save(passenger);

        // Lock Seat in Redis & DB
        SeatLockResponseDTO lockResponse = seatLockService.lockSpecificSeat(
                testFlight.getFlightId(),
                bookingEntity.getSegments().get(0).getSegmentId(),
                "12A",
                passenger.getPassengerId(),
                testUser.getEmail()
        );
        assertNotNull(lockResponse.getSeatLockId());
        assertTrue(seatLockService.isSeatLockedInRedis(testFlight.getFlightId(), "12A"));

        // Confirm Booking
        BookingResponseDTO confirmed = bookingService.confirmBooking(bookingResponse.getBookingId(), testUser.getEmail());
        assertEquals(BookingStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(PaymentStatus.SUCCESS, confirmed.getPaymentStatus());

        // Verify Flight available seats decremented
        Flight updatedFlight = flightRepository.findById(testFlight.getFlightId()).orElseThrow();
        assertEquals(Short.valueOf((short) 179), updatedFlight.getAvailableSeats());
    }

    @Test
    @DisplayName("7. Booking Confirmation Fails & Rolls Back When Redis Seat Lock Is Missing / Expired")
    void testBookingConfirmationRollbackOnMissingRedisLock() {
        // Create booking
        BookingRequestDTO bookingRequest = new BookingRequestDTO();
        bookingRequest.setFlightIds(List.of(testFlight.getFlightId()));
        bookingRequest.setCabinClass(CabinClass.ECONOMY);
        BookingResponseDTO bookingResponse = bookingService.createBooking(bookingRequest, testUser.getEmail());

        // Create Passenger
        Booking bookingEntity = bookingRepository.findByIdWithDetails(bookingResponse.getBookingId()).orElseThrow();
        Passenger passenger = new Passenger();
        passenger.setBooking(bookingEntity);
        passenger.setFirstName("Jane");
        passenger.setLastName("Doe");
        passenger.setSeatNumber("14B");
        passenger = passengerRepository.save(passenger);

        // Create DB SeatLock entry WITHOUT active Redis key (simulating expired Redis TTL)
        SeatLockResponseDTO lockResponse = seatLockService.lockSpecificSeat(
                testFlight.getFlightId(),
                bookingEntity.getSegments().get(0).getSegmentId(),
                "14B",
                passenger.getPassengerId(),
                testUser.getEmail()
        );

        // Manually delete Redis lock key to simulate expiration
        redisTemplate.delete(seatLockService.buildLockKey(testFlight.getFlightId(), "14B"));
        assertFalse(seatLockService.isSeatLockedInRedis(testFlight.getFlightId(), "14B"));

        // Attempt Confirmation -> Must throw SeatLockExpiredException
        assertThrows(SeatLockExpiredException.class, () ->
                bookingService.confirmBooking(bookingResponse.getBookingId(), testUser.getEmail())
        );

        // Verify booking status remains PENDING in DB (Transaction rolled back status change)
        Booking dbBooking = bookingRepository.findById(bookingResponse.getBookingId()).orElseThrow();
        assertEquals(BookingStatus.PENDING, dbBooking.getStatus());
        assertEquals(PaymentStatus.PENDING, dbBooking.getPaymentStatus());
    }

    @Test
    @DisplayName("8. N+1 Query Optimization with JOIN FETCH")
    void testJoinFetchNPlusOneOptimization() {
        BookingRequestDTO bookingRequest = new BookingRequestDTO();
        bookingRequest.setFlightIds(List.of(testFlight.getFlightId()));
        bookingRequest.setCabinClass(CabinClass.ECONOMY);
        BookingResponseDTO bookingResponse = bookingService.createBooking(bookingRequest, testUser.getEmail());

        entityManager.clear(); // Clear persistence context

        // Single query with JOIN FETCH
        Optional<Booking> bookingOpt = bookingRepository.findByIdWithDetails(bookingResponse.getBookingId());
        assertTrue(bookingOpt.isPresent());

        Booking booking = bookingOpt.get();
        // Access lazy associations without triggering secondary SQL SELECT queries
        assertNotNull(booking.getUser().getFirstName());
        assertNotNull(booking.getFlight().getFlightNumber());
        assertNotNull(booking.getFlight().getAirline().getName());
        assertNotNull(booking.getFlight().getAircraft().getModel());
        assertFalse(booking.getSegments().isEmpty());
    }
}
