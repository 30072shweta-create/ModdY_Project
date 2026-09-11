package com.example.flight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.example.flight.model.ChatRequest;
import com.example.flight.model.ChatResponse;
import com.example.flight.service.AIChatService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight.dto.CouponResponseDTO;
import com.example.flight.dto.FlightResponseDTO;
import com.example.flight.entity.Aircraft;
import com.example.flight.entity.Airline;
import com.example.flight.entity.Airport;
import com.example.flight.entity.Booking;
import com.example.flight.entity.BookingStatus;
import com.example.flight.entity.Coupon;
import com.example.flight.entity.DiscountType;
import com.example.flight.entity.Flight;
import com.example.flight.entity.FlightStatus;
import com.example.flight.entity.User;
import com.example.flight.model.Domain;
import com.example.flight.repository.AircraftRepository;
import com.example.flight.repository.AirlineRepository;
import com.example.flight.repository.AirportRepository;
import com.example.flight.repository.BookingRepository;
import com.example.flight.repository.CouponRepository;
import com.example.flight.repository.FlightRepository;
import com.example.flight.repository.UserRepository;
import com.example.flight.service.KnowledgeService;
import com.example.flight.tools.BookingTools;
import com.example.flight.tools.CouponTools;
import com.example.flight.tools.FlightTools;
import com.example.flight.tools.PaymentTools;
import com.example.flight.tools.RefundTools;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChatbotStaticAndDynamicTest {

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private CacheManager cacheManager;

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private AIChatService aiChatService;

    @Autowired
    private FlightTools flightTools;

    @Autowired
    private BookingTools bookingTools;

    @Autowired
    private PaymentTools paymentTools;

    @Autowired
    private RefundTools refundTools;

    @Autowired
    private CouponTools couponTools;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private AirlineRepository airlineRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouponRepository couponRepository;

    private Flight testFlight;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        Airport from = airportRepository.save(new Airport("BLR", "Kempegowda International", "Bengaluru", "India"));
        Airport to = airportRepository.save(new Airport("DEL", "Indira Gandhi International", "Delhi", "India"));
        Airline airline = airlineRepository.save(new Airline("AI", "Air India"));
        Aircraft aircraft = aircraftRepository.save(Aircraft.builder()
                .aircraftCode("A320-AI")
                .model("Airbus A320")
                .totalSeatCapacity(180)
                .active(true)
                .build());

        testFlight = new Flight();
        testFlight.setFlightNumber("AI101");
        testFlight.setAirline(airline);
        testFlight.setFromAirport(from);
        testFlight.setToAirport(to);
        testFlight.setAircraft(aircraft);
        testFlight.setDepartureTs(LocalDateTime.now().plusHours(48));
        testFlight.setArrivalTs(LocalDateTime.now().plusHours(51));
        testFlight.setBasePrice(new BigDecimal("4500.00"));
        testFlight.setAvailableSeats((short) 120);
        testFlight.setStatus(FlightStatus.SCHEDULED);
        testFlight.setDurationMins(180);
        testFlight.setStops((short) 0);
        testFlight = flightRepository.save(testFlight);

        User user = userRepository.save(User.builder()
                .email("traveler@example.com")
                .firstName("Test")
                .lastName("Traveler")
                .passwordHash("secret")
                .role("USER")
                .emailVerified(true)
                .build());

        testBooking = new Booking();
        testBooking.setBookingCode("BK1234");
        testBooking.setUser(user);
        testBooking.setFlight(testFlight);
        testBooking.setTotalAmount(new BigDecimal("5000.00"));
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setPaymentStatus(com.example.flight.entity.PaymentStatus.SUCCESS);
        testBooking.setBookingTs(LocalDateTime.now());
        testBooking = bookingRepository.save(testBooking);

        couponRepository.save(Coupon.builder()
                .couponCode("SAVE20")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20.00"))
                .minimumBookingAmount(new BigDecimal("1000.00"))
                .maximumDiscount(new BigDecimal("1000.00"))
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(30))
                .active(true)
                .build());
    }

    // =========================================================================
    // 1. STATIC QUESTIONS: Verify Markdown Knowledge Files
    // =========================================================================

    @Test
    @DisplayName("Static questions: KnowledgeService loads exact markdown knowledge for Flights and Seats")
    void testFlightsAndSeatsMarkdownKnowledge() {
        String knowledge = knowledgeService.getKnowledge(Domain.FLIGHTS_AND_SEATS);
        assertNotNull(knowledge);
        assertFalse(knowledge.isBlank());
        assertTrue(knowledge.contains("Flights and Seats"));
        assertTrue(knowledge.contains("Flight Search"));
        assertTrue(knowledge.contains("Aircraft"));
        assertTrue(knowledge.contains("Seat Selection"));
    }

    @Test
    @DisplayName("Static questions: KnowledgeService loads exact markdown knowledge for Booking")
    void testBookingMarkdownKnowledge() {
        String knowledge = knowledgeService.getKnowledge(Domain.BOOKING);
        assertNotNull(knowledge);
        assertFalse(knowledge.isBlank());
        assertTrue(knowledge.contains("Booking"));
        assertTrue(knowledge.contains("Passenger Information") || knowledge.contains("Creating a Booking"));
        assertTrue(knowledge.contains("Booking Confirmation"));
    }

    @Test
    @DisplayName("Static questions: KnowledgeService loads exact markdown knowledge for Payment")
    void testPaymentMarkdownKnowledge() {
        String knowledge = knowledgeService.getKnowledge(Domain.PAYMENT);
        assertNotNull(knowledge);
        assertFalse(knowledge.isBlank());
        assertTrue(knowledge.contains("Payment"));
        assertTrue(knowledge.contains("UPI") || knowledge.contains("Net Banking"));
        assertTrue(knowledge.contains("Payment Methods"));
    }

    @Test
    @DisplayName("Static questions: KnowledgeService loads exact markdown knowledge for Cancellation and Refund")
    void testCancellationAndRefundMarkdownKnowledge() {
        String knowledge = knowledgeService.getKnowledge(Domain.CANCELLATION_AND_REFUND);
        assertNotNull(knowledge);
        assertFalse(knowledge.isBlank());
        assertTrue(knowledge.contains("Cancellation and Refund"));
        assertTrue(knowledge.contains("10% cancellation charge"));
        assertTrue(knowledge.contains("25% cancellation charge"));
        assertTrue(knowledge.contains("50% cancellation charge"));
    }

    @Test
    @DisplayName("Static questions: KnowledgeService loads exact markdown knowledge for Pricing and Coupons")
    void testPricingAndCouponsMarkdownKnowledge() {
        String knowledge = knowledgeService.getKnowledge(Domain.PRICING_AND_COUPONS);
        assertNotNull(knowledge);
        assertFalse(knowledge.isBlank());
        assertTrue(knowledge.contains("Pricing and Coupons"));
        assertTrue(knowledge.contains("Flight Pricing"));
        assertTrue(knowledge.contains("Cabin Classes"));
    }

    @Test
    @DisplayName("Static questions: KnowledgeService provides all knowledge when domain is null")
    void testAllKnowledgeFallback() {
        String allKnowledge = knowledgeService.getKnowledge(null);
        assertNotNull(allKnowledge);
        assertTrue(allKnowledge.contains("Flights and Seats"));
        assertTrue(allKnowledge.contains("Cancellation and Refund"));
        assertTrue(allKnowledge.contains("Pricing and Coupons"));
    }

    // =========================================================================
    // 2. DYNAMIC QUESTIONS: Live Tools
    // =========================================================================

    @Test
    @DisplayName("Dynamic questions: FlightTools search available flights from source to destination")
    void testDynamicFlightSearch() {
        List<FlightResponseDTO> flights = flightTools.searchFlights("BLR", "DEL", null);
        assertNotNull(flights);
        assertFalse(flights.isEmpty());
        assertTrue(flights.stream().anyMatch(f -> "AI101".equals(f.getFlightNumber())));
    }

    @Test
    @DisplayName("Dynamic questions: FlightTools track flight by flight number")
    void testDynamicFlightTracking() {
        FlightResponseDTO tracked = flightTools.trackFlight("AI101");
        assertNotNull(tracked);
        assertTrue("AI101".equalsIgnoreCase(tracked.getFlightNumber()));
        assertTrue("BLR".equalsIgnoreCase(tracked.getFromAirport()));
        assertTrue("DEL".equalsIgnoreCase(tracked.getToAirport()));
        assertNotNull(tracked.getStatus());
    }

    @Test
    @DisplayName("Dynamic questions: FlightTools check seat availability on flight")
    void testDynamicSeatAvailability() {
        String availability = flightTools.checkSeatAvailability("AI101");
        assertNotNull(availability);
        assertTrue(availability.contains("120 seats available"));
    }

    @Test
    @DisplayName("Dynamic questions: BookingTools get booking details by code")
    void testDynamicBookingLookup() {
        var booking = bookingTools.getBookingByCode("BK1234");
        assertNotNull(booking);
        assertTrue("BK1234".equalsIgnoreCase(booking.getBookingCode()));
        assertTrue("CONFIRMED".equalsIgnoreCase(booking.getStatus().name()));
    }

    @Test
    @DisplayName("Dynamic questions: PaymentTools get supported payment methods")
    void testDynamicPaymentMethods() {
        List<String> methods = paymentTools.getPaymentMethods();
        assertNotNull(methods);
        assertFalse(methods.isEmpty());
        assertTrue(methods.stream().anyMatch(m -> m.contains("Cards") || m.contains("UPI")));
    }

    @Test
    @DisplayName("Dynamic questions: CouponTools get active coupons and validate code")
    void testDynamicCouponTools() {
        List<CouponResponseDTO> activeCoupons = couponTools.getActiveCoupons();
        assertNotNull(activeCoupons);
        assertFalse(activeCoupons.isEmpty());
        assertTrue(activeCoupons.stream().anyMatch(c -> "SAVE20".equals(c.getCouponCode())));

        Object validationResult = couponTools.validateCoupon("SAVE20", new BigDecimal("2000.00"));
        assertNotNull(validationResult);
        assertTrue(validationResult.toString().contains("valid"));
    }

    @Test
    @DisplayName("Dynamic questions: RefundTools estimate refund amount for booking")
    void testDynamicRefundEstimate() {
        String refundEstimate = refundTools.calculateEstimatedRefund("BK1234");
        assertNotNull(refundEstimate);
        assertTrue(refundEstimate.contains("Cancellation estimate"));
        assertTrue(refundEstimate.contains("Estimated Refund"));
    }

    @Test
    @DisplayName("Dynamic questions: FlightTools search available flights with date filter")
    void testDynamicFlightSearchWithDate() {
        LocalDate flightDate = testFlight.getDepartureTs().toLocalDate();
        List<FlightResponseDTO> matching = flightTools.searchFlights("BLR", "DEL", flightDate);
        assertNotNull(matching);
        assertFalse(matching.isEmpty());
        assertTrue(matching.stream().anyMatch(f -> "AI101".equals(f.getFlightNumber())));

        LocalDate differentDate = flightDate.plusDays(10);
        List<FlightResponseDTO> nonMatching = flightTools.searchFlights("BLR", "DEL", differentDate);
        assertNotNull(nonMatching);
        assertTrue(nonMatching.isEmpty());
    }

    @Test
    @DisplayName("Chatbot: Direct flight search with date filter")
    void testChatbotFlightSearchWithDate() {
        LocalDate flightDate = testFlight.getDepartureTs().toLocalDate();
        String dateStr = flightDate.getMonthValue() + "/" + flightDate.getDayOfMonth() + "/" + flightDate.getYear();

        ChatRequest request = new ChatRequest("conv-date-1", Domain.FLIGHTS_AND_SEATS, "what are the flights available from BLR to DEL on " + dateStr);
        ChatResponse response = aiChatService.chat(request);

        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertTrue(response.getAnswer().contains("AI101"));
        assertTrue(response.getAnswer().contains("Available Flights from BLR to DEL"));

        // When searching on a date with no flights
        ChatRequest noFlightRequest = new ChatRequest("conv-date-2", Domain.FLIGHTS_AND_SEATS, "what are the flights available from BLR to DEL on 12/25/2099");
        ChatResponse noFlightResponse = aiChatService.chat(noFlightRequest);
        assertNotNull(noFlightResponse);
        assertTrue(noFlightResponse.getAnswer().contains("No scheduled flights found between BLR and DEL"));
        assertTrue(noFlightResponse.getAnswer().contains("Available flight dates on this route"));
    }

    @Test
    @DisplayName("Chatbot: Follow-up date query continues previous route search")
    void testChatbotFollowUpDateSearch() {
        String convId = "conv-follow-up-test";

        // First ask about route without date
        ChatRequest req1 = new ChatRequest(convId, Domain.FLIGHTS_AND_SEATS, "what are the flights available from BLR to DEL");
        ChatResponse res1 = aiChatService.chat(req1);
        assertNotNull(res1);
        assertTrue(res1.getAnswer().contains("AI101"));
        assertTrue(res1.getAnswer().contains("📅"));

        // Follow up with a specific date
        LocalDate flightDate = testFlight.getDepartureTs().toLocalDate();
        String dateStr = flightDate.getMonthValue() + "/" + flightDate.getDayOfMonth() + "/" + flightDate.getYear();
        ChatRequest req2 = new ChatRequest(convId, Domain.FLIGHTS_AND_SEATS, "on " + dateStr);
        ChatResponse res2 = aiChatService.chat(req2);
        assertNotNull(res2);
        assertTrue(res2.getAnswer().contains("AI101"));
        assertTrue(res2.getAnswer().contains("Available Flights from BLR to DEL"));

        // Follow up with date with no flights
        ChatRequest req3 = new ChatRequest(convId, Domain.FLIGHTS_AND_SEATS, "on 12/31/2099");
        ChatResponse res3 = aiChatService.chat(req3);
        assertNotNull(res3);
        assertTrue(res3.getAnswer().contains("No scheduled flights found between BLR and DEL"));
    }

    @Test
    @DisplayName("Chatbot: extractDate parses various date formats")
    void testExtractDateFormats() {
        assertEquals(LocalDate.of(2026, 9, 8), aiChatService.extractDate("what are the flights are avaoible from BLR to GOX on 9/8/2026"));
        assertEquals(LocalDate.of(2026, 9, 12), aiChatService.extractDate("what are the flight are avaible from BLR TO BOM 9/12/2026"));
        assertEquals(LocalDate.of(2026, 9, 13), aiChatService.extractDate("what are the flight avaible from BLR to BOM 9/13/2026"));
        assertEquals(LocalDate.of(2026, 9, 12), aiChatService.extractDate("on 9/12/2026"));
        assertEquals(LocalDate.of(2026, 9, 8), aiChatService.extractDate("BLR to BOM on 2026-09-08"));
        assertEquals(LocalDate.of(2026, 9, 8), aiChatService.extractDate("flights on September 8, 2026"));
        assertEquals(LocalDate.of(2026, 9, 8), aiChatService.extractDate("flights on 8 September 2026"));
        assertEquals(LocalDate.now().plusDays(1), aiChatService.extractDate("flights tomorrow"));
        assertEquals(LocalDate.now(), aiChatService.extractDate("flights today"));
    }

    @Test
    @DisplayName("Chatbot: User exact scenarios with date filtering, date grouping, and follow-up")
    void testExactUserScenarios() {
        Airport blr = airportRepository.findById("BLR").orElseGet(() -> airportRepository.save(new Airport("BLR", "Kempegowda", "Bengaluru", "India")));
        Airport bom = airportRepository.findById("BOM").orElseGet(() -> airportRepository.save(new Airport("BOM", "Chhatrapati Shivaji", "Mumbai", "India")));
        Airport gox = airportRepository.findById("GOX").orElseGet(() -> airportRepository.save(new Airport("GOX", "Manohar International", "Goa", "India")));
        Airline airline = airlineRepository.findById("AI").orElseGet(() -> airlineRepository.save(new Airline("AI", "Air India")));
        Aircraft aircraft = aircraftRepository.findAll().get(0);

        // Seed QP-123 on 2026-09-12 if not already present
        if (flightRepository.findByFlightNumberWithDetails("QP-123").isEmpty()) {
            Flight qp123 = new Flight();
            qp123.setFlightNumber("QP-123");
            qp123.setAirline(airline);
            qp123.setFromAirport(blr);
            qp123.setToAirport(bom);
            qp123.setAircraft(aircraft);
            qp123.setDepartureTs(LocalDateTime.of(2026, 9, 12, 12, 27));
            qp123.setArrivalTs(LocalDateTime.of(2026, 9, 12, 14, 29));
            qp123.setBasePrice(new BigDecimal("3000.00"));
            qp123.setAvailableSeats((short) 180);
            qp123.setStatus(FlightStatus.SCHEDULED);
            qp123.setDurationMins(122);
            qp123.setStops((short) 0);
            flightRepository.save(qp123);
        }

        // Seed DL-123 on 2026-09-13 if not already present
        if (flightRepository.findByFlightNumberWithDetails("DL-123").isEmpty()) {
            Flight dl123 = new Flight();
            dl123.setFlightNumber("DL-123");
            dl123.setAirline(airline);
            dl123.setFromAirport(blr);
            dl123.setToAirport(bom);
            dl123.setAircraft(aircraft);
            dl123.setDepartureTs(LocalDateTime.of(2026, 9, 13, 2, 42));
            dl123.setArrivalTs(LocalDateTime.of(2026, 9, 13, 4, 49));
            dl123.setBasePrice(new BigDecimal("3000.00"));
            dl123.setAvailableSeats((short) 180);
            dl123.setStatus(FlightStatus.SCHEDULED);
            dl123.setDurationMins(127);
            dl123.setStops((short) 0);
            flightRepository.save(dl123);
        }

        // Seed AI-206 on 2026-09-04 if not already present
        if (flightRepository.findByFlightNumberWithDetails("AI-206").isEmpty()) {
            Flight ai206 = new Flight();
            ai206.setFlightNumber("AI-206");
            ai206.setAirline(airline);
            ai206.setFromAirport(blr);
            ai206.setToAirport(gox);
            ai206.setAircraft(aircraft);
            ai206.setDepartureTs(LocalDateTime.of(2026, 9, 4, 1, 25));
            ai206.setArrivalTs(LocalDateTime.of(2026, 9, 4, 2, 41));
            ai206.setBasePrice(new BigDecimal("3003.00"));
            ai206.setAvailableSeats((short) 172);
            ai206.setStatus(FlightStatus.SCHEDULED);
            ai206.setDurationMins(76);
            ai206.setStops((short) 0);
            flightRepository.save(ai206);
        }

        // Scenario 1: user asks for BLR to GOX on 9/8/2026 (no flight on 9/8, flight is on 9/4)
        ChatResponse res1 = aiChatService.chat(new ChatRequest("user-test-c1", Domain.FLIGHTS_AND_SEATS, "what are the flights are avaoible from BLR to GOX on 9/8/2026"));
        assertNotNull(res1);
        assertTrue(res1.getAnswer().contains("No scheduled flights found between BLR and GOX on 08/09/2026"));
        assertTrue(res1.getAnswer().contains("04/09/2026"));
        assertFalse(res1.getAnswer().contains("AI-206 (Air India): BLR → GOX | Dep: 01:25"));

        // Scenario 2: user asks for BLR to BOM (no date)
        ChatResponse res2 = aiChatService.chat(new ChatRequest("user-test-c2", Domain.FLIGHTS_AND_SEATS, "what are the flight are avaible from BLR to BOM"));
        assertNotNull(res2);
        assertTrue(res2.getAnswer().contains("QP-123"));
        assertTrue(res2.getAnswer().contains("DL-123"));
        assertTrue(res2.getAnswer().contains("📅 **Date: 12/09/2026"));
        assertTrue(res2.getAnswer().contains("📅 **Date: 13/09/2026"));

        // Scenario 3: user follow-up "on 9/12/2026"
        ChatResponse res3 = aiChatService.chat(new ChatRequest("user-test-c2", Domain.FLIGHTS_AND_SEATS, "on 9/12/2026"));
        assertNotNull(res3);
        assertTrue(res3.getAnswer().contains("QP-123"));
        assertFalse(res3.getAnswer().contains("DL-123")); // DL-123 is on 9/13, must NOT appear!

        // Scenario 4: user asks for BLR to BOM 9/13/2026
        ChatResponse res4 = aiChatService.chat(new ChatRequest("user-test-c3", Domain.FLIGHTS_AND_SEATS, "what are the flight avaible from BLR to BOM 9/13/2026"));
        assertNotNull(res4);
        assertTrue(res4.getAnswer().contains("DL-123"));
        assertFalse(res4.getAnswer().contains("QP-123")); // QP-123 is on 9/12, must NOT appear!
    }
}
