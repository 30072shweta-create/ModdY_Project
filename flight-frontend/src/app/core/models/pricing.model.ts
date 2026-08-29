import { CabinClass } from './booking.model';

export type DiscountType = 'PERCENTAGE' | 'FIXED';
export type RuleType = 'WEEKEND' | 'HOLIDAY' | 'SEASONAL' | 'PROMOTION';
export type AdjustmentType = 'PERCENTAGE' | 'FIXED';

export interface FareCalculationRequestDTO {
  flightId: number;
  bookingId?: number;
  cabinClass: CabinClass;
  passengerCount: number;
  travelDate: string;
  couponCode?: string;
  isHoliday?: boolean;
  extraBaggage?: boolean;
  gourmetMeal?: boolean;
  travelInsurance?: boolean;
}

export interface FareBreakdownDTO {
  flightId: number;
  cabinClass: CabinClass;
  baseFare: number;
  tax?: number;
  airportFee?: number;
  convenienceFee?: number;
  taxAmount: number;
  fuelSurcharge?: number;
  surgeMultiplier?: number;
  extraBaggageFee?: number;
  gourmetMealFee?: number;
  travelInsuranceFee?: number;
  discountAmount?: number;
  couponDiscount?: number;
  totalDiscount?: number;
  couponApplied?: string;
  totalFare: number;
  finalPrice?: number;
  currency: string;
}

export interface FlightPricingRequestDTO {
  flightId: number;
  cabinClass: CabinClass;
  baseFare: number;
  tax?: number;
  taxes?: number;
  airportFee?: number;
  convenienceFee?: number;
  baggageFee?: number;
  discount?: number;
  currency?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
}

export interface FlightPricingResponseDTO {
  pricingId: number;
  flightId: number;
  cabinClass: CabinClass;
  baseFare: number;
  tax?: number;
  taxes?: number;
  airportFee?: number;
  convenienceFee?: number;
  baggageFee?: number;
  discount?: number;
  finalPrice?: number;
  currency?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CouponRequestDTO {
  couponCode: string;
  discountType: DiscountType;
  discountValue: number;
  minimumBookingAmount?: number;
  maximumDiscount?: number;
  validFrom?: string;
  validTo?: string;
  usageLimit?: number;
  active?: boolean;
}

export interface CouponResponseDTO {
  couponId: number;
  couponCode: string;
  discountType: DiscountType;
  discountValue: number;
  minimumBookingAmount?: number;
  maximumDiscount?: number;
  validFrom?: string;
  validTo?: string;
  usageLimit?: number;
  usedCount?: number;
  active?: boolean;
}

export interface PricingRuleRequestDTO {
  name: string;
  ruleType: RuleType;
  adjustmentType: AdjustmentType;
  adjustmentValue: number;
  effectiveFrom?: string;
  effectiveTo?: string;
  active?: boolean;
  priority?: number;
}

export interface PricingRuleResponseDTO {
  ruleId: number;
  name: string;
  ruleType: RuleType;
  adjustmentType: AdjustmentType;
  adjustmentValue: number;
  effectiveFrom?: string;
  effectiveTo?: string;
  active?: boolean;
  priority?: number;
}

export interface HolidayRequestDTO {
  name: string;
  holidayDate: string;
  active?: boolean;
}

export interface HolidayResponseDTO {
  holidayId: number;
  name: string;
  holidayDate: string;
  active?: boolean;
}
