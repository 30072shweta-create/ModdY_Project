import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  UserResponseDTO,
  UpdateProfileRequestDTO,
  ChangePasswordRequestDTO
} from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class UsersService {
  private apiUrl = `${environment.apiUrl}/api/users`;

  constructor(private http: HttpClient) {}

  public getProfile(): Observable<UserResponseDTO> {
    return this.http.get<UserResponseDTO>(`${this.apiUrl}/profile`);
  }

  public updateProfile(dto: UpdateProfileRequestDTO): Observable<UserResponseDTO> {
    return this.http.put<UserResponseDTO>(`${this.apiUrl}/profile`, dto);
  }

  public changePassword(dto: ChangePasswordRequestDTO): Observable<string> {
    return this.http.put(`${this.apiUrl}/change-password`, dto, { responseType: 'text' });
  }

  // Admin User Management
  public getAllUsers(): Observable<UserResponseDTO[]> {
    return this.http.get<UserResponseDTO[]>(this.apiUrl);
  }

  public getUserById(userId: number): Observable<UserResponseDTO> {
    return this.http.get<UserResponseDTO>(`${this.apiUrl}/${userId}`);
  }

  public updateUserRole(userId: number, role: string): Observable<UserResponseDTO> {
    return this.http.put<UserResponseDTO>(`${this.apiUrl}/${userId}/role`, null, { params: { role } });
  }

  public updateUserStatus(userId: number, emailVerified: boolean): Observable<UserResponseDTO> {
    return this.http.put<UserResponseDTO>(`${this.apiUrl}/${userId}/status`, null, { params: { emailVerified: String(emailVerified) } });
  }
}
