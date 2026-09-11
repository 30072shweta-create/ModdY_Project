export interface AirportWeather {
  airportCode: string;
  cityName: string;
  temperature: number;
  condition: string;
  icon: string;
  windSpeed: number;
  humidity?: number;
  flightSuitability: 'OPTIMAL' | 'GOOD' | 'MODERATE' | 'CAUTION';
  flightSuitabilityText: string;
}

export interface RouteWeather {
  departure: AirportWeather;
  destination: AirportWeather;
  overallSuitability: 'OPTIMAL' | 'GOOD' | 'MODERATE' | 'CAUTION';
  summaryText: string;
}
