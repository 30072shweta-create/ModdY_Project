export type CabinClass = 'ECONOMY' | 'PREMIUM_ECONOMY' | 'BUSINESS' | 'FIRST';
export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED';
export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';

export interface BookingRequestDTO {
  flightIds: number[];
  cabinClass: CabinClass;
  couponCode?: string;
  baggageCount?: number;
  baggageKg?: number;
  travelDate?: string;
}

export interface BookingSegmentResponseDTO {
  segmentId: number;
  bookingId: number;
  flightId: number;
  flightNumber: string;
  airlineCode: string;
  airlineName: string;
  fromAirport: string;
  toAirport: string;
  departureTs: string;
  arrivalTs: string;
  cabinClass: CabinClass;
  price: number;
  passengers: PassengerResponseDTO[];
}

export interface BookingSegmentRequestDTO {
  flightId: number;
  cabinClass: CabinClass;
}

export interface BookingResponseDTO {
  bookingId: number;
  userId: number;
  bookingCode: string;
  status: BookingStatus;
  paymentStatus: PaymentStatus;
  totalAmount: number;
  bookingTs: string;
  segments: BookingSegmentResponseDTO[];
}

export interface PassengerRequestDTO {
  firstName: string;
  lastName: string;
  passportNumber?: string;
  age?: number;
  gender?: string;
  dateOfBirth?: string;
  seatNumber?: string;
}

export interface PassengerResponseDTO {
  passengerId: number;
  bookingId: number;
  firstName: string;
  lastName: string;
  passportNumber?: string;
  age?: number;
  gender?: string;
  dateOfBirth?: string;
  seatNumber?: string;
}

export interface BookingAddOnRequestDTO {
  addOnType: string;
  description: string;
  price: number;
}

export interface BookingAddOnResponseDTO {
  addonId: number;
  bookingId: number;
  addOnType: string;
  description: string;
  price: number;
}

export interface BookingCancellationRequestDTO {
  bookingId?: number;
  reason?: string;
}

export interface BookingCancellationResponseDTO {
  cancellationId: number;
  bookingId: number;
  cancellationCode: string;
  reason: string;
  cancelledAt: string;
  refundAmount: number;
  cancellationFee: number;
  status: string;
}
