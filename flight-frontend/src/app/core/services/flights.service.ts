import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  FlightResponseDTO,
  FlightRequestDTO,
  FlightSearchRequestDTO,
  FlightStatusRequestDTO,
  Page
} from '../models/flight.model';

@Injectable({
  providedIn: 'root'
})
export class FlightsService {
  private apiUrl = `${environment.apiUrl}/api/flights`;

  constructor(private http: HttpClient) {}

  public searchFlights(request: FlightSearchRequestDTO): Observable<Page<FlightResponseDTO>> {
    let params = new HttpParams();
    Object.keys(request).forEach(key => {
      const val = (request as any)[key];
      if (val !== undefined && val !== null && val !== '') {
        params = params.set(key, val);
      }
    });

    return this.http.get<Page<FlightResponseDTO>>(`${this.apiUrl}/search`, { params });
  }

  public getAllFlights(): Observable<FlightResponseDTO[]> {
    return this.http.get<FlightResponseDTO[]>(this.apiUrl);
  }

  public getFlightById(flightId: number): Observable<FlightResponseDTO> {
    return this.http.get<FlightResponseDTO>(`${this.apiUrl}/${flightId}`);
  }

  public addFlight(dto: FlightRequestDTO): Observable<FlightResponseDTO> {
    return this.http.post<FlightResponseDTO>(this.apiUrl, dto);
  }

  public updateFlight(flightId: number, dto: FlightRequestDTO): Observable<FlightResponseDTO> {
    return this.http.put<FlightResponseDTO>(`${this.apiUrl}/${flightId}`, dto);
  }

  public deleteFlight(flightId: number): Observable<string> {
    return this.http.delete(`${this.apiUrl}/${flightId}`, { responseType: 'text' });
  }

  public updateFlightStatus(flightId: number, request: FlightStatusRequestDTO): Observable<FlightResponseDTO> {
    return this.http.patch<FlightResponseDTO>(`${this.apiUrl}/${flightId}/status`, request);
  }
}
