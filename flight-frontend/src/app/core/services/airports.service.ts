import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AirportResponseDTO, AirportRequestDTO } from '../models/flight.model';

@Injectable({
  providedIn: 'root'
})
export class AirportsService {
  private apiUrl = `${environment.apiUrl}/api/airports`;

  constructor(private http: HttpClient) {}

  public getAllAirports(): Observable<AirportResponseDTO[]> {
    return this.http.get<AirportResponseDTO[]>(this.apiUrl);
  }

  public getAirportByCode(code: string): Observable<AirportResponseDTO> {
    return this.http.get<AirportResponseDTO>(`${this.apiUrl}/${code}`);
  }

  public addAirport(dto: AirportRequestDTO): Observable<AirportResponseDTO> {
    return this.http.post<AirportResponseDTO>(this.apiUrl, dto);
  }

  public updateAirport(code: string, dto: AirportRequestDTO): Observable<AirportResponseDTO> {
    return this.http.put<AirportResponseDTO>(`${this.apiUrl}/${code}`, dto);
  }

  public deleteAirport(code: string): Observable<string> {
    return this.http.delete(`${this.apiUrl}/${code}`, { responseType: 'text' });
  }
}
