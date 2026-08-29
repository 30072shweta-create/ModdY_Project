import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { HolidayRequestDTO, HolidayResponseDTO } from '../models/pricing.model';

@Injectable({
  providedIn: 'root'
})
export class HolidaysService {
  private apiUrl = `${environment.apiUrl}/api/holidays`;

  constructor(private http: HttpClient) {}

  public getAllHolidays(): Observable<HolidayResponseDTO[]> {
    return this.http.get<HolidayResponseDTO[]>(this.apiUrl);
  }

  public createHoliday(dto: HolidayRequestDTO): Observable<HolidayResponseDTO> {
    return this.http.post<HolidayResponseDTO>(this.apiUrl, dto);
  }

  public updateHoliday(holidayId: number, dto: HolidayRequestDTO): Observable<HolidayResponseDTO> {
    return this.http.put<HolidayResponseDTO>(`${this.apiUrl}/${holidayId}`, dto);
  }

  public deleteHoliday(holidayId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${holidayId}`);
  }
}
