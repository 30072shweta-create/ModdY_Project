import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  BookingCancellationRequestDTO,
  BookingCancellationResponseDTO
} from '../models/booking.model';

@Injectable({
  providedIn: 'root'
})
export class CancellationsService {
  private apiUrl = `${environment.apiUrl}/api/cancellations`;

  constructor(private http: HttpClient) {}

  public cancelBooking(dto: BookingCancellationRequestDTO): Observable<BookingCancellationResponseDTO> {
    return this.http.post<BookingCancellationResponseDTO>(this.apiUrl, dto);
  }

  public getCancellationByBookingId(bookingId: number): Observable<BookingCancellationResponseDTO> {
    return this.http.get<BookingCancellationResponseDTO>(`${this.apiUrl}/booking/${bookingId}`);
  }
}
