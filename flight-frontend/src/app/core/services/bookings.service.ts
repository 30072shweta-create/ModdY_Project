import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  BookingRequestDTO,
  BookingResponseDTO,
  BookingCancellationRequestDTO,
  BookingCancellationResponseDTO
} from '../models/booking.model';

@Injectable({
  providedIn: 'root'
})
export class BookingsService {
  private apiUrl = `${environment.apiUrl}/api/bookings`;

  constructor(private http: HttpClient) {}

  public createBooking(dto: BookingRequestDTO): Observable<BookingResponseDTO> {
    return this.http.post<BookingResponseDTO>(this.apiUrl, dto);
  }

  public getBookingById(bookingId: number): Observable<BookingResponseDTO> {
    return this.http.get<BookingResponseDTO>(`${this.apiUrl}/${bookingId}`);
  }

  public getBookingByCode(bookingCode: string): Observable<BookingResponseDTO> {
    return this.http.get<BookingResponseDTO>(`${this.apiUrl}/code/${bookingCode}`);
  }

  public getUserBookings(userId: number): Observable<BookingResponseDTO[]> {
    return this.http.get<BookingResponseDTO[]>(`${this.apiUrl}/user/${userId}`);
  }

  public getAllBookings(): Observable<BookingResponseDTO[]> {
    return this.http.get<BookingResponseDTO[]>(this.apiUrl);
  }

  public confirmBooking(bookingId: number): Observable<BookingResponseDTO> {
    return this.http.post<BookingResponseDTO>(`${this.apiUrl}/${bookingId}/confirm`, {});
  }

  public cancelBooking(bookingId: number, dto?: BookingCancellationRequestDTO): Observable<BookingCancellationResponseDTO> {
    return this.http.post<BookingCancellationResponseDTO>(`${this.apiUrl}/${bookingId}/cancel`, dto || { bookingId });
  }
}
