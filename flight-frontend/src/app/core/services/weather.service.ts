import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, forkJoin, map, catchError, shareReplay } from 'rxjs';
import { AirportWeather, RouteWeather } from '../models/weather.model';

interface AirportCoord {
  city: string;
  lat: number;
  lon: number;
}

@Injectable({
  providedIn: 'root'
})
export class WeatherService {
  private http = inject(HttpClient);
  private cache = new Map<string, Observable<AirportWeather>>();

  private airportCoordinates: Record<string, AirportCoord> = {
    BLR: { city: 'Bengaluru', lat: 12.9716, lon: 77.5946 },
    DEL: { city: 'New Delhi', lat: 28.6139, lon: 77.2090 },
    BOM: { city: 'Mumbai', lat: 19.0760, lon: 72.8777 },
    HYD: { city: 'Hyderabad', lat: 17.3850, lon: 78.4867 },
    MAA: { city: 'Chennai', lat: 13.0827, lon: 80.2707 },
    CCU: { city: 'Kolkata', lat: 22.5726, lon: 88.3639 },
    GOI: { city: 'Goa (Dabolim)', lat: 15.3803, lon: 73.8314 },
    GOX: { city: 'Goa (Mopa)', lat: 15.7533, lon: 73.8647 },
    COK: { city: 'Kochi', lat: 9.9312, lon: 76.2673 },
    AMD: { city: 'Ahmedabad', lat: 23.0225, lon: 72.5714 },
    PNQ: { city: 'Pune', lat: 18.5204, lon: 73.8567 },
    JAI: { city: 'Jaipur', lat: 26.9124, lon: 75.7873 },
    LKO: { city: 'Lucknow', lat: 26.8467, lon: 80.9462 },
    GAU: { city: 'Guwahati', lat: 26.1445, lon: 91.7362 },
    DXB: { city: 'Dubai', lat: 25.2048, lon: 55.2708 },
    LHR: { city: 'London', lat: 51.5074, lon: -0.1278 },
    SIN: { city: 'Singapore', lat: 1.3521, lon: 103.8198 },
    JFK: { city: 'New York', lat: 40.7128, lon: -74.0060 },
    LAX: { city: 'Los Angeles', lat: 34.0522, lon: -118.2437 },
    SFO: { city: 'San Francisco', lat: 37.7749, lon: -122.4194 },
    CDG: { city: 'Paris', lat: 48.8566, lon: 2.3522 },
    FRA: { city: 'Frankfurt', lat: 50.1109, lon: 8.6821 },
    NRT: { city: 'Tokyo', lat: 35.6762, lon: 139.6503 }
  };

  public getAirportWeather(code: string): Observable<AirportWeather> {
    const cleanCode = (code || 'BLR').trim().toUpperCase();
    if (this.cache.has(cleanCode)) {
      return this.cache.get(cleanCode)!;
    }

    const coord = this.airportCoordinates[cleanCode] || {
      city: cleanCode,
      lat: 12.9716,
      lon: 77.5946
    };

    const url = `https://api.open-meteo.com/v1/forecast?latitude=${coord.lat}&longitude=${coord.lon}&current_weather=true&hourly=relative_humidity_2m`;

    const weather$ = this.http.get<any>(url).pipe(
      map(res => this.mapOpenMeteoResponse(cleanCode, coord.city, res)),
      catchError(() => of(this.getFallbackWeather(cleanCode, coord.city))),
      shareReplay(1)
    );

    this.cache.set(cleanCode, weather$);
    return weather$;
  }

  public getRouteWeather(fromCode: string, toCode: string): Observable<RouteWeather> {
    return forkJoin({
      dep: this.getAirportWeather(fromCode),
      arr: this.getAirportWeather(toCode)
    }).pipe(
      map(({ dep, arr }) => {
        let overallSuitability: 'OPTIMAL' | 'GOOD' | 'MODERATE' | 'CAUTION' = 'OPTIMAL';
        if (dep.flightSuitability === 'CAUTION' || arr.flightSuitability === 'CAUTION') {
          overallSuitability = 'CAUTION';
        } else if (dep.flightSuitability === 'MODERATE' || arr.flightSuitability === 'MODERATE') {
          overallSuitability = 'MODERATE';
        } else if (dep.flightSuitability === 'GOOD' || arr.flightSuitability === 'GOOD') {
          overallSuitability = 'GOOD';
        }

        let summaryText = 'Clear skies on departure and arrival. Smooth cruising expected.';
        if (overallSuitability === 'GOOD') {
          summaryText = 'Favorable cruising weather with light breezes along the route.';
        } else if (overallSuitability === 'MODERATE') {
          summaryText = 'Minor precipitation or cloud cover expected near destination.';
        } else if (overallSuitability === 'CAUTION') {
          summaryText = 'Adverse weather reported. Flight schedules operating under standard safety protocol.';
        }

        return {
          departure: dep,
          destination: arr,
          overallSuitability,
          summaryText
        };
      })
    );
  }

  private mapOpenMeteoResponse(code: string, city: string, data: any): AirportWeather {
    const current = data?.current_weather;
    const temp = current?.temperature !== undefined ? Math.round(current.temperature) : 25;
    const wind = current?.windspeed !== undefined ? Math.round(current.windspeed) : 10;
    const weatherCode = current?.weathercode !== undefined ? current.weathercode : 0;
    const humidity = data?.hourly?.relative_humidity_2m?.[0] || 60;

    const { condition, icon, suitability, suitabilityText } = this.interpretWmoCode(weatherCode, wind);

    return {
      airportCode: code,
      cityName: city,
      temperature: temp,
      condition,
      icon,
      windSpeed: wind,
      humidity,
      flightSuitability: suitability,
      flightSuitabilityText: suitabilityText
    };
  }

  private interpretWmoCode(wmoCode: number, windSpeed: number): {
    condition: string;
    icon: string;
    suitability: 'OPTIMAL' | 'GOOD' | 'MODERATE' | 'CAUTION';
    suitabilityText: string;
  } {
    if (wmoCode === 0) {
      return {
        condition: 'Clear Sky',
        icon: 'wb_sunny',
        suitability: 'OPTIMAL',
        suitabilityText: 'Clear Skies & Optimal Cruising'
      };
    }
    if (wmoCode >= 1 && wmoCode <= 3) {
      return {
        condition: 'Partly Cloudy',
        icon: 'partly_cloudy_day',
        suitability: 'OPTIMAL',
        suitabilityText: 'Smooth Cruising'
      };
    }
    if (wmoCode >= 45 && wmoCode <= 48) {
      return {
        condition: 'Foggy / Hazy',
        icon: 'foggy',
        suitability: 'MODERATE',
        suitabilityText: 'Low Visibility - CAT III ILS Active'
      };
    }
    if (wmoCode >= 51 && wmoCode <= 65) {
      return {
        condition: 'Light Rain / Drizzle',
        icon: 'rainy',
        suitability: 'GOOD',
        suitabilityText: 'Light Rain - Normal Operations'
      };
    }
    if (wmoCode >= 80 && wmoCode <= 82) {
      return {
        condition: 'Rain Showers',
        icon: 'rainy_heavy',
        suitability: 'MODERATE',
        suitabilityText: 'Rain Showers - Minor Turbulence'
      };
    }
    if (wmoCode >= 95) {
      return {
        condition: 'Thunderstorm',
        icon: 'thunderstorm',
        suitability: 'CAUTION',
        suitabilityText: 'Thunderstorm - Air Route Managed'
      };
    }

    return {
      condition: 'Fair Weather',
      icon: 'cloud_queue',
      suitability: 'GOOD',
      suitabilityText: 'Good Flying Conditions'
    };
  }

  private getFallbackWeather(code: string, city: string): AirportWeather {
    return {
      airportCode: code,
      cityName: city,
      temperature: 26,
      condition: 'Pleasant & Clear',
      icon: 'wb_sunny',
      windSpeed: 12,
      humidity: 55,
      flightSuitability: 'OPTIMAL',
      flightSuitabilityText: 'Clear Skies - Great Flying Weather'
    };
  }
}
