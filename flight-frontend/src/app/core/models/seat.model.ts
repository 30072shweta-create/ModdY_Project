export type SeatLockStatus = 'LOCKED' | 'CONFIRMED' | 'RELEASED' | 'EXPIRED';

export interface SeatLockRequestDTO {
  segmentId: number;
  passengerId: number;
}

export interface SeatLockResponseDTO {
  seatLockId: number;
  bookingId: number;
  segmentId: number;
  flightId: number;
  passengerId: number;
  seatNumber: string;
  status: SeatLockStatus;
  lockedAt: string;
  lockedUntil: string;
}

export interface SeatCheckResponse {
  flightId: number;
  seatNumber: string;
  isLocked: boolean;
  owner: string;
}

export interface SeatReleaseResponse {
  released: boolean;
  message: string;
}

export interface UISeat {
  seatNumber: string;
  rowNumber: number;
  columnLetter: string;
  cabinClass: 'ECONOMY' | 'BUSINESS' | 'FIRST';
  price: number;
  isLocked: boolean;
  isOccupied: boolean;
  isSelected: boolean;
  isMine: boolean;
  isPending?: boolean;
  lockedByPassengerId?: number;
}
