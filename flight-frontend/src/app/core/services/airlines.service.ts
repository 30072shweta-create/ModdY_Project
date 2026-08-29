import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AirlineResponseDTO, AirlineRequestDTO } from '../models/flight.model';

@Injectable({
  providedIn: 'root'
})
export class AirlinesService {
  private apiUrl = `${environment.apiUrl}/api/airlines`;

  constructor(private http: HttpClient) {}

  public getAllAirlines(): Observable<AirlineResponseDTO[]> {
    return this.http.get<AirlineResponseDTO[]>(this.apiUrl);
  }

  public getAirlineByCode(code: string): Observable<AirlineResponseDTO> {
    return this.http.get<AirlineResponseDTO>(`${this.apiUrl}/${code}`);
  }

  public addAirline(dto: AirlineRequestDTO): Observable<AirlineResponseDTO> {
    return this.http.post<AirlineResponseDTO>(this.apiUrl, dto);
  }

  public updateAirline(code: string, dto: AirlineRequestDTO): Observable<AirlineResponseDTO> {
    return this.http.put<AirlineResponseDTO>(`${this.apiUrl}/${code}`, dto);
  }

  public deleteAirline(code: string): Observable<string> {
    return this.http.delete(`${this.apiUrl}/${code}`, { responseType: 'text' });
  }
}
