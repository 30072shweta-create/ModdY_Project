import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NotificationResponseDTO, NotificationRequestDTO } from '../models/notification.model';

@Injectable({
  providedIn: 'root'
})
export class NotificationsService {
  private apiUrl = `${environment.apiUrl}/api/notifications`;

  constructor(private http: HttpClient) {}

  public getAllNotifications(): Observable<NotificationResponseDTO[]> {
    return this.http.get<NotificationResponseDTO[]>(this.apiUrl);
  }

  public getNotificationById(notificationId: number): Observable<NotificationResponseDTO> {
    return this.http.get<NotificationResponseDTO>(`${this.apiUrl}/${notificationId}`);
  }

  public getNotificationsByUser(userId: number): Observable<NotificationResponseDTO[]> {
    return this.http.get<NotificationResponseDTO[]>(`${this.apiUrl}/user/${userId}`);
  }

  public getNotificationsByBooking(bookingId: number): Observable<NotificationResponseDTO[]> {
    return this.http.get<NotificationResponseDTO[]>(`${this.apiUrl}/booking/${bookingId}`);
  }

  public createNotification(dto: NotificationRequestDTO): Observable<NotificationResponseDTO> {
    return this.http.post<NotificationResponseDTO>(this.apiUrl, dto);
  }

  public updateNotification(notificationId: number, dto: NotificationRequestDTO): Observable<NotificationResponseDTO> {
    return this.http.put<NotificationResponseDTO>(`${this.apiUrl}/${notificationId}`, dto);
  }

  public deleteNotification(notificationId: number): Observable<string> {
    return this.http.delete(`${this.apiUrl}/${notificationId}`, { responseType: 'text' });
  }
}
