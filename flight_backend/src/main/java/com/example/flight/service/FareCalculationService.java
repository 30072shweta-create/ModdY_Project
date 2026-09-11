package com.example.flight.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight.dto.FareBreakdownDTO;
import com.example.flight.dto.FareCalculationRequestDTO;
import com.example.flight.entity.AdjustmentType;
import com.example.flight.entity.CabinClass;
import com.example.flight.entity.Flight;
import com.example.flight.entity.FlightPricing;
import com.example.flight.entity.PricingRule;
import com.example.flight.entity.RuleType;
import com.example.flight.exception.FlightNotFoundException;
import com.example.flight.repository.BookingRepository;
import com.example.flight.repository.FlightPricingRepository;
import com.example.flight.repository.FlightRepository;
import com.example.flight.repository.HolidayRepository;
import com.example.flight.repository.PricingRuleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FareCalculationService {

    private final FlightPricingRepository flightPricingRepository;
    private final FlightRepository flightRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final HolidayRepository holidayRepository;
    private final CouponService couponService;
    private final BookingRepository bookingRepository;

    @Transactional
    public FareBreakdownDTO calculateFare(FareCalculationRequestDTO request) {
        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new FlightNotFoundException("Flight not found with ID: " + request.getFlightId()));

        LocalDateTime travelTs = request.getTravelDate() != null ? request.getTravelDate() :
                (flight.getDepartureTs() != null ? flight.getDepartureTs() : LocalDateTime.now());

        CabinClass seatClass = request.getCabinClass();

        FlightPricing pricing = flightPricingRepository.findActivePricing(flight.getFlightId(), seatClass, travelTs)
                .orElseGet(() -> flightPricingRepository.findByFlightFlightIdAndSeatClass(flight.getFlightId(), seatClass)
                        .orElseGet(() -> createDefaultPricingFromFlight(flight, seatClass, travelTs)));

        int passengerCount = (request.getPassengerCount() != null && request.getPassengerCount() > 0) ? request.getPassengerCount() : 1;

        BigDecimal baseFare = (pricing.getBaseFare() != null ? pricing.getBaseFare() : BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(passengerCount));
        BigDecimal tax = (pricing.getTax() != null ? pricing.getTax() : BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(passengerCount));
        BigDecimal airportFee = (pricing.getAirportFee() != null ? pricing.getAirportFee() : BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(passengerCount));
        BigDecimal convenienceFee = (pricing.getConvenienceFee() != null ? pricing.getConvenienceFee() : BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(passengerCount));
        BigDecimal baseBaggageFee = (pricing.getBaggageFee() != null ? pricing.getBaggageFee() : BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(passengerCount));

        // Selected Add-ons
        BigDecimal extraBaggageFee = Boolean.TRUE.equals(request.getExtraBaggage()) ? new BigDecimal("1200.00") : BigDecimal.ZERO;
        BigDecimal gourmetMealFee = Boolean.TRUE.equals(request.getGourmetMeal()) ? new BigDecimal("450.00") : BigDecimal.ZERO;
        BigDecimal travelInsuranceFee = Boolean.TRUE.equals(request.getTravelInsurance()) ? new BigDecimal("299.00") : BigDecimal.ZERO;

        BigDecimal totalAddons = extraBaggageFee.add(gourmetMealFee).add(travelInsuranceFee);

        // Additional baggage calculation
        BigDecimal additionalBaggage = BigDecimal.ZERO;
        if (request.getBaggageCount() != null && request.getBaggageCount() > 0) {
            additionalBaggage = additionalBaggage.add(new BigDecimal("500.00").multiply(new BigDecimal(request.getBaggageCount())));
        }
        if (request.getBaggageKg() != null && request.getBaggageKg() > 0) {
            additionalBaggage = additionalBaggage.add(new BigDecimal("100.00").multiply(new BigDecimal(request.getBaggageKg())));
        }
        BigDecimal baggageFee = baseBaggageFee.add(additionalBaggage);

        // Dynamic Pricing Adjustments
        BigDecimal weekendAdj = calculateWeekendAdjustment(travelTs, baseFare);
        BigDecimal holidayAdj = calculateHolidayAdjustment(travelTs.toLocalDate(), travelTs, baseFare);
        BigDecimal seasonalAdj = calculateSeasonalAdjustment(travelTs, baseFare);
        BigDecimal promotionalDiscount = calculatePromotionalDiscount(travelTs, baseFare);

        BigDecimal taxAmount = tax.add(airportFee).add(convenienceFee);

        BigDecimal subtotal = baseFare.add(weekendAdj).add(holidayAdj).add(seasonalAdj).add(taxAmount).add(baggageFee).add(totalAddons);

        // Coupon Discount
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            couponDiscount = couponService.calculateCouponDiscount(request.getCouponCode(), subtotal);
        }

        BigDecimal totalDiscount = promotionalDiscount.add(couponDiscount);
        BigDecimal grossPrice = subtotal.subtract(totalDiscount);
        BigDecimal finalPrice = grossPrice.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : grossPrice;

        // Sync final price with Booking entity if bookingId is provided
        if (request.getBookingId() != null) {
            bookingRepository.findById(request.getBookingId()).ifPresent(b -> {
                b.setTotalAmount(finalPrice.setScale(2, RoundingMode.HALF_UP));
                bookingRepository.save(b);
            });
        }

        return FareBreakdownDTO.builder()
                .flightId(flight.getFlightId())
                .cabinClass(seatClass)
                .currency(pricing.getCurrency() != null ? pricing.getCurrency() : "INR")
                .baseFare(baseFare.setScale(2, RoundingMode.HALF_UP))
                .weekendAdjustment(weekendAdj.setScale(2, RoundingMode.HALF_UP))
                .holidayAdjustment(holidayAdj.setScale(2, RoundingMode.HALF_UP))
                .seasonalAdjustment(seasonalAdj.setScale(2, RoundingMode.HALF_UP))
                .promotionalDiscount(promotionalDiscount.setScale(2, RoundingMode.HALF_UP))
                .couponDiscount(couponDiscount.setScale(2, RoundingMode.HALF_UP))
                .tax(tax.setScale(2, RoundingMode.HALF_UP))
                .airportFee(airportFee.setScale(2, RoundingMode.HALF_UP))
                .convenienceFee(convenienceFee.setScale(2, RoundingMode.HALF_UP))
                .baggageFee(baggageFee.setScale(2, RoundingMode.HALF_UP))
                .extraBaggageFee(extraBaggageFee.setScale(2, RoundingMode.HALF_UP))
                .gourmetMealFee(gourmetMealFee.setScale(2, RoundingMode.HALF_UP))
                .travelInsuranceFee(travelInsuranceFee.setScale(2, RoundingMode.HALF_UP))
                .taxAmount(taxAmount.setScale(2, RoundingMode.HALF_UP))
                .discountAmount(totalDiscount.setScale(2, RoundingMode.HALF_UP))
                .totalDiscount(totalDiscount.setScale(2, RoundingMode.HALF_UP))
                .finalPrice(finalPrice.setScale(2, RoundingMode.HALF_UP))
                .totalFare(finalPrice.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    private FlightPricing createDefaultPricingFromFlight(Flight flight, CabinClass seatClass, LocalDateTime travelTs) {
        FlightPricing pricing = FlightPricing.builder()
                .flight(flight)
                .seatClass(seatClass)
                .baseFare(flight.getBasePrice() != null ? flight.getBasePrice() : BigDecimal.ZERO)
                .tax(BigDecimal.ZERO)
                .taxes(BigDecimal.ZERO)
                .airportFee(BigDecimal.ZERO)
                .convenienceFee(BigDecimal.ZERO)
                .baggageFee(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .currency("INR")
                .effectiveFrom(travelTs != null ? travelTs.minusDays(1) : LocalDateTime.now())
                .effectiveTo(null)
                .build();

        pricing.calculateFinalPrice();
        return flightPricingRepository.save(pricing);
    }

    private BigDecimal calculateWeekendAdjustment(LocalDateTime travelTs, BigDecimal baseFare) {
        DayOfWeek dow = travelTs.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            List<PricingRule> rules = pricingRuleRepository.findActiveRulesByTypeAndTimestamp(RuleType.WEEKEND, travelTs);
            if (!rules.isEmpty()) {
                return applyRule(rules.get(0), baseFare);
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateHolidayAdjustment(LocalDate date, LocalDateTime travelTs, BigDecimal baseFare) {
        if (holidayRepository.findByHolidayDateAndActiveTrue(date).isPresent()) {
            List<PricingRule> rules = pricingRuleRepository.findActiveRulesByTypeAndTimestamp(RuleType.HOLIDAY, travelTs);
            if (!rules.isEmpty()) {
                return applyRule(rules.get(0), baseFare);
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateSeasonalAdjustment(LocalDateTime travelTs, BigDecimal baseFare) {
        List<PricingRule> rules = pricingRuleRepository.findActiveRulesByTypeAndTimestamp(RuleType.SEASONAL, travelTs);
        if (!rules.isEmpty()) {
            return applyRule(rules.get(0), baseFare);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculatePromotionalDiscount(LocalDateTime travelTs, BigDecimal baseFare) {
        List<PricingRule> rules = pricingRuleRepository.findActiveRulesByTypeAndTimestamp(RuleType.PROMOTION, travelTs);
        if (!rules.isEmpty()) {
            return applyRule(rules.get(0), baseFare);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal applyRule(PricingRule rule, BigDecimal baseFare) {
        if (rule.getAdjustmentType() == AdjustmentType.PERCENTAGE) {
            return baseFare.multiply(rule.getAdjustmentValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            return rule.getAdjustmentValue();
        }
    }
}
