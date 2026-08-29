package com.example.flight;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.example.flight.controller.FlightPricingController;
import com.example.flight.dto.BookingRequestDTO;
import com.example.flight.dto.BookingResponseDTO;
import com.example.flight.dto.CouponRequestDTO;
import com.example.flight.dto.CouponResponseDTO;
import com.example.flight.dto.FareBreakdownDTO;
import com.example.flight.dto.FareCalculationRequestDTO;
import com.example.flight.dto.FlightPricingRequestDTO;
import com.example.flight.dto.FlightPricingResponseDTO;
import com.example.flight.dto.HolidayRequestDTO;
import com.example.flight.dto.PricingRuleRequestDTO;
import com.example.flight.entity.AdjustmentType;
import com.example.flight.entity.Aircraft;
import com.example.flight.entity.Airline;
import com.example.flight.entity.Airport;
import com.example.flight.entity.Booking;
import com.example.flight.entity.CabinClass;
import com.example.flight.entity.DiscountType;
import com.example.flight.entity.Flight;
import com.example.flight.entity.FlightPricing;
import com.example.flight.entity.FlightStatus;
import com.example.flight.entity.RuleType;
import com.example.flight.entity.User;
import com.example.flight.exception.CouponExpiredException;
import com.example.flight.exception.CouponUsageLimitExceededException;
import com.example.flight.exception.InvalidPricingPeriodException;

import com.example.flight.repository.AircraftRepository;
import com.example.flight.repository.AirlineRepository;
import com.example.flight.repository.AirportRepository;
import com.example.flight.repository.BookingRepository;
import com.example.flight.repository.CouponRepository;
import com.example.flight.repository.FlightPricingRepository;
import com.example.flight.repository.FlightRepository;
import com.example.flight.repository.HolidayRepository;
import com.example.flight.repository.PricingRuleRepository;
import com.example.flight.repository.SeatLockRepository;
import com.example.flight.repository.UserRepository;

import com.example.flight.service.BookingService;
import com.example.flight.service.CouponService;
import com.example.flight.service.FareCalculationService;
import com.example.flight.service.FlightPricingService;
import com.example.flight.service.HolidayService;
import com.example.flight.service.PricingRuleService;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
public class Phase4PricingModuleTest {

    @Autowired
    private FlightPricingService flightPricingService;

    @Autowired
    private FlightPricingRepository flightPricingRepository;

    @Autowired
    private FareCalculationService fareCalculationService;

    @Autowired
    private PricingRuleService pricingRuleService;

    @Autowired
    private PricingRuleRepository pricingRuleRepository;

    @Autowired
    private HolidayService holidayService;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private AirlineRepository airlineRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatLockRepository seatLockRepository;

    @Autowired
    private FlightPricingController flightPricingController;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private org.springframework.cache.CacheManager cacheManager;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private Flight testFlight;
    private User testUser;
    private Aircraft testAircraft;
    private ConcurrentHashMap<String, String> redisMemory;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        redisMemory = new ConcurrentHashMap<>();
        org.springframework.cache.Cache mockCache = org.mockito.Mockito.mock(org.springframework.cache.Cache.class);
        lenient().when(cacheManager.getCache(anyString())).thenReturn(mockCache);

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
        bookingRepository.deleteAll();
        flightPricingRepository.deleteAll();
        pricingRuleRepository.deleteAll();
        holidayRepository.deleteAll();
        couponRepository.deleteAll();
        flightRepository.deleteAll();
        aircraftRepository.deleteAll();
        userRepository.findByEmail("pricing_test_user@example.com").ifPresent(userRepository::delete);

        // Setup User
        testUser = User.builder()
                .email("pricing_test_user@example.com")
                .passwordHash("hashedpass")
                .firstName("Pricing")
                .lastName("Tester")
                .role("USER")
                .emailVerified(true)
                .build();
        testUser = userRepository.save(testUser);

        // Setup Aircraft
        testAircraft = Aircraft.builder()
                .aircraftCode("B787-TEST")
                .model("Boeing 787")
                .manufacturer("Boeing")
                .totalSeatCapacity(250)
                .active(true)
                .build();
        testAircraft = aircraftRepository.save(testAircraft);

        Airline testAirline = airlineRepository.findById("AI").orElseGet(() ->
                airlineRepository.save(new Airline("AI", "Air India"))
        );

        Airport testFrom = airportRepository.findById("DEL").orElseGet(() ->
                airportRepository.save(new Airport("DEL", "Indira Gandhi Int", "Delhi", "India"))
        );

        Airport testTo = airportRepository.findById("BOM").orElseGet(() ->
                airportRepository.save(new Airport("BOM", "Chhatrapati Shivaji", "Mumbai", "India"))
        );

        // Setup Flight
        testFlight = new Flight();
        testFlight.setFlightNumber("AI-401");
        testFlight.setAirline(testAirline);
        testFlight.setFromAirport(testFrom);
        testFlight.setToAirport(testTo);
        testFlight.setAircraft(testAircraft);
        testFlight.setDepartureTs(LocalDateTime.now().plusDays(1));
        testFlight.setArrivalTs(LocalDateTime.now().plusDays(1).plusHours(2));
        testFlight.setStops((short) 0);
        testFlight.setBasePrice(new BigDecimal("5000.00"));
        testFlight.setAvailableSeats((short) 250);
        testFlight.setDurationMins(120);
        testFlight.setStatus(FlightStatus.SCHEDULED);
        testFlight = flightRepository.save(testFlight);

        // Setup Initial Base Pricing for Economy
        FlightPricing economyPricing = FlightPricing.builder()
                .flight(testFlight)
                .seatClass(CabinClass.ECONOMY)
                .baseFare(new BigDecimal("5000.00"))
                .tax(new BigDecimal("500.00"))
                .airportFee(new BigDecimal("200.00"))
                .convenienceFee(new BigDecimal("100.00"))
                .baggageFee(new BigDecimal("300.00"))
                .discount(BigDecimal.ZERO)
                .currency("INR")
                .effectiveFrom(LocalDateTime.now().minusDays(1))
                .build();
        economyPricing.calculateFinalPrice();
        flightPricingRepository.save(economyPricing);
    }

    @Test
    @DisplayName("1. Economy Cabin Pricing Calculation")
    void testEconomyPricingCalculation() {
        FareCalculationRequestDTO request = FareCalculationRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.ECONOMY)
                .build();

        FareBreakdownDTO breakdown = fareCalculationService.calculateFare(request);
        assertEquals(new BigDecimal("5000.00"), breakdown.getBaseFare());
        assertEquals(new BigDecimal("6100.00"), breakdown.getFinalPrice());
    }

    @Test
    @DisplayName("2. Premium Economy Cabin Pricing Calculation")
    void testPremiumEconomyPricingCalculation() {
        FlightPricing pePricing = FlightPricing.builder()
                .flight(testFlight)
                .seatClass(CabinClass.PREMIUM_ECONOMY)
                .baseFare(new BigDecimal("7500.00"))
                .tax(new BigDecimal("750.00"))
                .airportFee(new BigDecimal("200.00"))
                .convenienceFee(new BigDecimal("100.00"))
                .baggageFee(new BigDecimal("300.00"))
                .effectiveFrom(LocalDateTime.now().minusDays(1))
                .build();
        pePricing.calculateFinalPrice();
        flightPricingRepository.save(pePricing);

        FareCalculationRequestDTO request = FareCalculationRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.PREMIUM_ECONOMY)
                .build();

        FareBreakdownDTO breakdown = fareCalculationService.calculateFare(request);
        assertEquals(new BigDecimal("7500.00"), breakdown.getBaseFare());
        assertEquals(new BigDecimal("8850.00"), breakdown.getFinalPrice());
    }

    @Test
    @DisplayName("3. Business Cabin Pricing Calculation")
    void testBusinessPricingCalculation() {
        FlightPricing bizPricing = FlightPricing.builder()
                .flight(testFlight)
                .seatClass(CabinClass.BUSINESS)
                .baseFare(new BigDecimal("15000.00"))
                .tax(new BigDecimal("1500.00"))
                .airportFee(new BigDecimal("500.00"))
                .convenienceFee(new BigDecimal("200.00"))
                .baggageFee(new BigDecimal("0.00"))
                .effectiveFrom(LocalDateTime.now().minusDays(1))
                .build();
        bizPricing.calculateFinalPrice();
        flightPricingRepository.save(bizPricing);

        FareCalculationRequestDTO request = FareCalculationRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.BUSINESS)
                .build();

        FareBreakdownDTO breakdown = fareCalculationService.calculateFare(request);
        assertEquals(new BigDecimal("15000.00"), breakdown.getBaseFare());
        assertEquals(new BigDecimal("17200.00"), breakdown.getFinalPrice());
    }

    @Test
    @DisplayName("4. First Class Cabin Pricing Calculation")
    void testFirstClassPricingCalculation() {
        FlightPricing fcPricing = FlightPricing.builder()
                .flight(testFlight)
                .seatClass(CabinClass.FIRST)
                .baseFare(new BigDecimal("30000.00"))
                .tax(new BigDecimal("3000.00"))
                .airportFee(new BigDecimal("1000.00"))
                .convenienceFee(new BigDecimal("300.00"))
                .baggageFee(new BigDecimal("0.00"))
                .effectiveFrom(LocalDateTime.now().minusDays(1))
                .build();
        fcPricing.calculateFinalPrice();
        flightPricingRepository.save(fcPricing);

        FareCalculationRequestDTO request = FareCalculationRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.FIRST)
                .build();

        FareBreakdownDTO breakdown = fareCalculationService.calculateFare(request);
        assertEquals(new BigDecimal("30000.00"), breakdown.getBaseFare());
        assertEquals(new BigDecimal("34300.00"), breakdown.getFinalPrice());
    }

    @Test
    @DisplayName("5. Weekend Pricing Adjustment")
    void testWeekendPricingAdjustment() {
        pricingRuleService.createRule(PricingRuleRequestDTO.builder()
                .name("Weekend Surge 10%")
                .ruleType(RuleType.WEEKEND)
                .adjustmentType(AdjustmentType.PERCENTAGE)
                .adjustmentValue(new BigDecimal("10.00"))
                .active(true)
                .priority(1)
                .build());

        // Next Saturday
        LocalDateTime Saturday = LocalDateTime.now().with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.SATURDAY));

        FareCalculationRequestDTO request = FareCalculationRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.ECONOMY)
                .travelDate(Saturday)
                .build();

        FareBreakdownDTO breakdown = fareCalculationService.calculateFare(request);
        assertEquals(new BigDecimal("500.00"), breakdown.getWeekendAdjustment());
        assertEquals(new BigDecimal("6600.00"), breakdown.getFinalPrice());
    }

    @Test
    @DisplayName("6. Holiday Pricing Adjustment")
    void testHolidayPricingAdjustment() {
        LocalDate holidayDate = LocalDate.now().plusDays(5);
        holidayService.createHoliday(HolidayRequestDTO.builder()
                .name("National Holiday")
                .holidayDate(holidayDate)
                .active(true)
                .build());

        pricingRuleService.createRule(PricingRuleRequestDTO.builder()
                .name("Holiday Surge Flat 1000")
                .ruleType(RuleType.HOLIDAY)
                .adjustmentType(AdjustmentType.FIXED)
                .adjustmentValue(new BigDecimal("1000.00"))
                .active(true)
                .priority(1)
                .build());

        FareCalculationRequestDTO request = FareCalculationRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.ECONOMY)
                .travelDate(holidayDate.atTime(10, 0))
                .build();

        FareBreakdownDTO breakdown = fareCalculationService.calculateFare(request);
        assertEquals(new BigDecimal("1000.00"), breakdown.getHolidayAdjustment());
        assertEquals(new BigDecimal("7100.00"), breakdown.getFinalPrice());
    }

    @Test
    @DisplayName("7. Seasonal Pricing Adjustment")
    void testSeasonalPricingAdjustment() {
        pricingRuleService.createRule(PricingRuleRequestDTO.builder()
                .name("Summer Peak 15%")
                .ruleType(RuleType.SEASONAL)
                .adjustmentType(AdjustmentType.PERCENTAGE)
                .adjustmentValue(new BigDecimal("15.00"))
                .active(true)
                .priority(1)
                .build());

        FareCalculationRequestDTO request = FareCalculationRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.ECONOMY)
                .build();

        FareBreakdownDTO breakdown = fareCalculationService.calculateFare(request);
        assertEquals(new BigDecimal("750.00"), breakdown.getSeasonalAdjustment());
        assertEquals(new BigDecimal("6850.00"), breakdown.getFinalPrice());
    }

    @Test
    @DisplayName("8. Promotional Discount Application")
    void testPromotionalDiscountApplication() {
        pricingRuleService.createRule(PricingRuleRequestDTO.builder()
                .name("Promo Flat 500 Off")
                .ruleType(RuleType.PROMOTION)
                .adjustmentType(AdjustmentType.FIXED)
                .adjustmentValue(new BigDecimal("500.00"))
                .active(true)
                .priority(1)
                .build());

        FareCalculationRequestDTO request = FareCalculationRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.ECONOMY)
                .build();

        FareBreakdownDTO breakdown = fareCalculationService.calculateFare(request);
        assertEquals(new BigDecimal("500.00"), breakdown.getPromotionalDiscount());
        assertEquals(new BigDecimal("5600.00"), breakdown.getFinalPrice());
    }

    @Test
    @DisplayName("9. Valid Coupon Code Application")
    void testValidCouponApplication() {
        couponService.createCoupon(CouponRequestDTO.builder()
                .couponCode("FLY20")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20.00"))
                .minimumBookingAmount(new BigDecimal("1000.00"))
                .active(true)
                .build());

        FareCalculationRequestDTO request = FareCalculationRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.ECONOMY)
                .couponCode("FLY20")
                .build();

        FareBreakdownDTO breakdown = fareCalculationService.calculateFare(request);
        assertEquals(new BigDecimal("1220.00"), breakdown.getCouponDiscount());
        assertEquals(new BigDecimal("4880.00"), breakdown.getFinalPrice());
    }

    @Test
    @DisplayName("10. Expired Coupon Rejection")
    void testExpiredCouponRejection() {
        couponService.createCoupon(CouponRequestDTO.builder()
                .couponCode("EXPIRED50")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("50.00"))
                .validFrom(LocalDateTime.now().minusDays(10))
                .validTo(LocalDateTime.now().minusDays(1))
                .active(true)
                .build());

        FareCalculationRequestDTO request = FareCalculationRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.ECONOMY)
                .couponCode("EXPIRED50")
                .build();

        assertThrows(CouponExpiredException.class, () -> fareCalculationService.calculateFare(request));
    }

    @Test
    @DisplayName("11. Coupon Usage Limit Exceeded Rejection")
    void testCouponUsageLimitExceededRejection() {
        couponService.createCoupon(CouponRequestDTO.builder()
                .couponCode("LIMIT1")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("100.00"))
                .usageLimit(1)
                .active(true)
                .build());

        couponService.incrementCouponUsage("LIMIT1");

        FareCalculationRequestDTO request = FareCalculationRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.ECONOMY)
                .couponCode("LIMIT1")
                .build();

        assertThrows(CouponUsageLimitExceededException.class, () -> fareCalculationService.calculateFare(request));
    }

    @Test
    @DisplayName("12. Tax Calculation Test")
    void testTaxCalculation() {
        FareCalculationRequestDTO request = FareCalculationRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.ECONOMY)
                .build();

        FareBreakdownDTO breakdown = fareCalculationService.calculateFare(request);
        assertEquals(new BigDecimal("500.00"), breakdown.getTax());
    }

    @Test
    @DisplayName("13. Airport Fee Test")
    void testAirportFee() {
        FareCalculationRequestDTO request = FareCalculationRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.ECONOMY)
                .build();

        FareBreakdownDTO breakdown = fareCalculationService.calculateFare(request);
        assertEquals(new BigDecimal("200.00"), breakdown.getAirportFee());
    }

    @Test
    @DisplayName("14. Convenience Fee Test")
    void testConvenienceFee() {
        FareCalculationRequestDTO request = FareCalculationRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.ECONOMY)
                .build();

        FareBreakdownDTO breakdown = fareCalculationService.calculateFare(request);
        assertEquals(new BigDecimal("100.00"), breakdown.getConvenienceFee());
    }

    @Test
    @DisplayName("15. Baggage Fee Test (Base + Additional)")
    void testBaggageFeeCalculation() {
        FareCalculationRequestDTO request = FareCalculationRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.ECONOMY)
                .baggageCount(2) // 2 * 500 = 1000
                .build();

        FareBreakdownDTO breakdown = fareCalculationService.calculateFare(request);
        assertEquals(new BigDecimal("1300.00"), breakdown.getBaggageFee()); // 300 base + 1000 count
    }

    @Test
    @DisplayName("16. Multiple Discounts Combination")
    void testMultipleDiscountsCombination() {
        pricingRuleService.createRule(PricingRuleRequestDTO.builder()
                .name("Promo 500")
                .ruleType(RuleType.PROMOTION)
                .adjustmentType(AdjustmentType.FIXED)
                .adjustmentValue(new BigDecimal("500.00"))
                .active(true)
                .priority(1)
                .build());

        couponService.createCoupon(CouponRequestDTO.builder()
                .couponCode("COMBO1000")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("1000.00"))
                .active(true)
                .build());

        FareCalculationRequestDTO request = FareCalculationRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.ECONOMY)
                .couponCode("COMBO1000")
                .build();

        FareBreakdownDTO breakdown = fareCalculationService.calculateFare(request);
        assertEquals(new BigDecimal("500.00"), breakdown.getPromotionalDiscount());
        assertEquals(new BigDecimal("1000.00"), breakdown.getCouponDiscount());
        assertEquals(new BigDecimal("1500.00"), breakdown.getTotalDiscount());
        assertEquals(new BigDecimal("4600.00"), breakdown.getFinalPrice());
    }

    @Test
    @DisplayName("17. Final Price Cannot Become Negative")
    void testFinalPriceCannotBecomeNegative() {
        couponService.createCoupon(CouponRequestDTO.builder()
                .couponCode("HUGE10000")
                .discountType(DiscountType.FIXED)
                .discountValue(new BigDecimal("100000.00"))
                .active(true)
                .build());

        FareCalculationRequestDTO request = FareCalculationRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.ECONOMY)
                .couponCode("HUGE10000")
                .build();

        FareBreakdownDTO breakdown = fareCalculationService.calculateFare(request);
        assertEquals(new BigDecimal("0.00"), breakdown.getFinalPrice());
    }

    @Test
    @DisplayName("18. Historical Pricing Period Tracking")
    void testHistoricalPricingPeriodTracking() {
        LocalDateTime newFrom = LocalDateTime.now();
        FlightPricingRequestDTO newRequest = FlightPricingRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.ECONOMY)
                .baseFare(new BigDecimal("6000.00"))
                .tax(new BigDecimal("600.00"))
                .effectiveFrom(newFrom)
                .build();

        flightPricingService.addPricing(newRequest);

        List<FlightPricingResponseDTO> history = flightPricingService.getPricingHistory(testFlight.getFlightId(), CabinClass.ECONOMY);
        assertEquals(2, history.size());
        assertEquals(new BigDecimal("6000.00"), history.get(0).getBaseFare());
        assertEquals(new BigDecimal("5000.00"), history.get(1).getBaseFare());
        assertNotNull(history.get(1).getEffectiveTo());
    }

    @Test
    @DisplayName("19. Effective Pricing Period Date Boundaries")
    void testEffectivePricingPeriodBoundaries() {
        LocalDateTime past = LocalDateTime.now().minusDays(10);
        LocalDateTime pastEnd = LocalDateTime.now().minusDays(2);

        FlightPricing pastPricing = FlightPricing.builder()
                .flight(testFlight)
                .seatClass(CabinClass.ECONOMY)
                .baseFare(new BigDecimal("2000.00"))
                .tax(new BigDecimal("200.00"))
                .effectiveFrom(past)
                .effectiveTo(pastEnd)
                .build();
        pastPricing.calculateFinalPrice();
        flightPricingRepository.save(pastPricing);

        Optional<FlightPricing> foundPast = flightPricingRepository.findActivePricing(testFlight.getFlightId(), CabinClass.ECONOMY, past.plusDays(1));
        assertTrue(foundPast.isPresent());
        assertEquals(new BigDecimal("2000.00"), foundPast.get().getBaseFare());
    }

    @Test
    @DisplayName("20. Invalid Pricing Period Rejection")
    void testInvalidPricingPeriodRejection() {
        FlightPricingRequestDTO invalidRequest = FlightPricingRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.ECONOMY)
                .baseFare(new BigDecimal("5000.00"))
                .effectiveFrom(LocalDateTime.now())
                .effectiveTo(LocalDateTime.now().minusDays(1))
                .build();

        assertThrows(InvalidPricingPeriodException.class, () -> flightPricingService.addPricing(invalidRequest));
    }

    @Test
    @DisplayName("21. USER Cannot Modify Pricing")
    void testUserCannotModifyPricing() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        FlightPricingRequestDTO dto = FlightPricingRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.ECONOMY)
                .baseFare(new BigDecimal("9000.00"))
                .build();

        assertThrows(AccessDeniedException.class, () -> flightPricingController.addPricing(dto));
    }

    @Test
    @DisplayName("22. ADMIN Can Modify Pricing")
    void testAdminCanModifyPricing() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        FlightPricingRequestDTO dto = FlightPricingRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.ECONOMY)
                .baseFare(new BigDecimal("8000.00"))
                .build();

        FlightPricingResponseDTO response = flightPricingController.addPricing(dto);
        assertNotNull(response.getPricingId());
        assertEquals(new BigDecimal("8000.00"), response.getBaseFare());
    }

    @Test
    @DisplayName("23. Booking Uses Server-Calculated Price")
    void testBookingUsesServerCalculatedPrice() {
        BookingRequestDTO bookingReq = BookingRequestDTO.builder()
                .flightIds(List.of(testFlight.getFlightId()))
                .cabinClass(CabinClass.ECONOMY)
                .build();

        BookingResponseDTO bookingResponse = bookingService.createBooking(bookingReq, testUser.getEmail());
        assertNotNull(bookingResponse.getBookingId());
        assertEquals(new BigDecimal("6100.00"), bookingResponse.getTotalAmount());
    }

    @Test
    @DisplayName("24. Existing Booking Price Snapshot Remains Unchanged")
    void testBookingPriceSnapshotUnchangedAfterUpdate() {
        BookingRequestDTO bookingReq = BookingRequestDTO.builder()
                .flightIds(List.of(testFlight.getFlightId()))
                .cabinClass(CabinClass.ECONOMY)
                .build();

        BookingResponseDTO bookingResponse = bookingService.createBooking(bookingReq, testUser.getEmail());
        assertEquals(new BigDecimal("6100.00"), bookingResponse.getTotalAmount());

        // Update pricing for flight
        FlightPricingRequestDTO updateReq = FlightPricingRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.ECONOMY)
                .baseFare(new BigDecimal("10000.00"))
                .build();
        flightPricingService.addPricing(updateReq);

        // Verify existing booking amount is still 6100.00
        Booking dbBooking = bookingRepository.findById(bookingResponse.getBookingId()).orElseThrow();
        assertEquals(new BigDecimal("6100.00"), dbBooking.getTotalAmount());
    }

    @Test
    @DisplayName("25. Repository Query Retrieves Only Relevant Flight Pricing")
    void testRepositoryQueryRetrieval() {
        List<FlightPricing> pricingList = flightPricingRepository.findByFlightFlightIdAndSeatClassOrderByEffectiveFromDesc(
                testFlight.getFlightId(), CabinClass.ECONOMY);

        assertFalse(pricingList.isEmpty());
        assertEquals(testFlight.getFlightId(), pricingList.get(0).getFlight().getFlightId());
        assertEquals(CabinClass.ECONOMY, pricingList.get(0).getSeatClass());
    }

    @Test
    @DisplayName("26. Cache Eviction After Pricing Update")
    void testCacheEvictionAfterUpdate() {
        FlightPricingResponseDTO initial = flightPricingService.getActivePricing(testFlight.getFlightId(), CabinClass.ECONOMY);
        assertEquals(new BigDecimal("5000.00"), initial.getBaseFare());

        FlightPricingRequestDTO newReq = FlightPricingRequestDTO.builder()
                .flightId(testFlight.getFlightId())
                .cabinClass(CabinClass.ECONOMY)
                .baseFare(new BigDecimal("9500.00"))
                .build();
        flightPricingService.addPricing(newReq);

        FlightPricingResponseDTO updated = flightPricingService.getActivePricing(testFlight.getFlightId(), CabinClass.ECONOMY);
        assertEquals(new BigDecimal("9500.00"), updated.getBaseFare());
    }
}
