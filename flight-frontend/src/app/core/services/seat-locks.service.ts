import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  SeatLockRequestDTO,
  SeatLockResponseDTO,
  SeatCheckResponse,
  SeatReleaseResponse
} from '../models/seat.model';

@Injectable({
  providedIn: 'root'
})
export class SeatLocksService {
  private apiUrl = `${environment.apiUrl}/api/seat-locks`;

  constructor(private http: HttpClient) {}

  public allocateAndLockSeat(dto: SeatLockRequestDTO): Observable<SeatLockResponseDTO> {
    return this.http.post<SeatLockResponseDTO>(this.apiUrl, dto);
  }

  public lockSpecificSeat(
    flightId: number,
    segmentId: number,
    seatNumber: string,
    passengerId: number
  ): Observable<SeatLockResponseDTO> {
    let params = new HttpParams()
      .set('flightId', flightId)
      .set('segmentId', segmentId)
      .set('seatNumber', seatNumber)
      .set('passengerId', passengerId);

    return this.http.post<SeatLockResponseDTO>(`${this.apiUrl}/lock-specific`, null, { params });
  }

  public releaseSeatLock(flightId: number, seatNumber: string): Observable<SeatReleaseResponse> {
    let params = new HttpParams()
      .set('flightId', flightId)
      .set('seatNumber', seatNumber);

    return this.http.delete<SeatReleaseResponse>(`${this.apiUrl}/release`, { params });
  }

  public checkSeatLock(flightId: number, seatNumber: string): Observable<SeatCheckResponse> {
    let params = new HttpParams()
      .set('flightId', flightId)
      .set('seatNumber', seatNumber);

    return this.http.get<SeatCheckResponse>(`${this.apiUrl}/check`, { params });
  }

  public getBookingSeatLocks(bookingId: number): Observable<SeatLockResponseDTO[]> {
    return this.http.get<SeatLockResponseDTO[]>(`${this.apiUrl}/booking/${bookingId}`);
  }

  public getOccupiedSeatsForFlight(flightId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/flight/${flightId}/occupied`);
  }
}
