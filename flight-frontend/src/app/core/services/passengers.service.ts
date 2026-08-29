import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PassengerRequestDTO, PassengerResponseDTO } from '../models/booking.model';

@Injectable({
  providedIn: 'root'
})
export class PassengersService {
  private apiUrl = `${environment.apiUrl}/api/bookings`;

  constructor(private http: HttpClient) {}

  public addPassenger(bookingId: number, dto: PassengerRequestDTO): Observable<PassengerResponseDTO> {
    return this.http.post<PassengerResponseDTO>(`${this.apiUrl}/${bookingId}/passengers`, dto);
  }

  public getPassengers(bookingId: number): Observable<PassengerResponseDTO[]> {
    return this.http.get<PassengerResponseDTO[]>(`${this.apiUrl}/${bookingId}/passengers`);
  }
}
