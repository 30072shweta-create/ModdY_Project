import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AircraftResponseDTO, AircraftRequestDTO } from '../models/flight.model';

@Injectable({
  providedIn: 'root'
})
export class AircraftService {
  private apiUrl = `${environment.apiUrl}/api/aircraft`;

  constructor(private http: HttpClient) {}

  public getAllAircraft(): Observable<AircraftResponseDTO[]> {
    return this.http.get<AircraftResponseDTO[]>(this.apiUrl);
  }

  public getAircraftById(aircraftId: number): Observable<AircraftResponseDTO> {
    return this.http.get<AircraftResponseDTO>(`${this.apiUrl}/${aircraftId}`);
  }

  public getAircraftByCode(code: string): Observable<AircraftResponseDTO> {
    return this.http.get<AircraftResponseDTO>(`${this.apiUrl}/code/${code}`);
  }

  public createAircraft(dto: AircraftRequestDTO): Observable<AircraftResponseDTO> {
    return this.http.post<AircraftResponseDTO>(this.apiUrl, dto);
  }

  public updateAircraft(aircraftId: number, dto: AircraftRequestDTO): Observable<AircraftResponseDTO> {
    return this.http.put<AircraftResponseDTO>(`${this.apiUrl}/${aircraftId}`, dto);
  }

  public deleteAircraft(aircraftId: number): Observable<string> {
    return this.http.delete(`${this.apiUrl}/${aircraftId}`, { responseType: 'text' });
  }
}
