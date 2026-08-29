export type FlightStatus = 'SCHEDULED' | 'BOARDING' | 'DEPARTED' | 'ARRIVED' | 'DELAYED' | 'CANCELLED';

export interface FlightResponseDTO {
  flightId: number;
  flightNumber: string;
  airlineCode: string;
  airlineName: string;
  fromAirport: string;
  toAirport: string;
  aircraftId: number;
  aircraftCode: string;
  aircraftModel: string;
  totalSeatCapacity: number;
  departureTs: string;
  arrivalTs: string;
  stops: number;
  basePrice: number;
  availableSeats: number;
  durationMins: number;
  status: FlightStatus;
}

export interface FlightRequestDTO {
  flightNumber: string;
  airlineCode: string;
  fromAirport: string;
  toAirport: string;
  aircraftId?: number | null;
  departureTs: string;
  arrivalTs: string;
  stops: number;
  basePrice: number;
  availableSeats?: number;
  durationMins?: number;
  status: FlightStatus;
}

export interface FlightSearchRequestDTO {
  source?: string;
  destination?: string;
  date?: string;
  airline?: string;
  flightNumber?: string;
  stops?: number;
  minPrice?: number;
  maxPrice?: number;
  maxDuration?: number;
  sortBy?: string;
  sortDirection?: string;
  page?: number;
  size?: number;
}

export interface FlightStatusRequestDTO {
  status: FlightStatus;
}

export interface AirportResponseDTO {
  airportCode: string;
  name: string;
  airportName?: string;
  city: string;
  country: string;
}

export interface AirportRequestDTO {
  airportCode: string;
  name: string;
  city: string;
  country: string;
}

export interface AirlineResponseDTO {
  airlineCode: string;
  airlineName: string;
}

export interface AirlineRequestDTO {
  airlineCode: string;
  airlineName: string;
}

export interface AircraftResponseDTO {
  aircraftId: number;
  aircraftCode: string;
  model: string;
  manufacturer?: string;
  totalSeatCapacity?: number;
  active?: boolean;
  totalSeats?: number;
  economySeats?: number;
  businessSeats?: number;
  firstClassSeats?: number;
}

export interface AircraftRequestDTO {
  aircraftCode: string;
  model: string;
  manufacturer?: string;
  totalSeatCapacity: number;
  active?: boolean;
  totalSeats?: number;
  economySeats?: number;
  businessSeats?: number;
  firstClassSeats?: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
