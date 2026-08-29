import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, BehaviorSubject, tap, catchError, of, throwError, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthResponseDTO,
  LoginRequestDTO,
  RegisterRequestDTO,
  UserResponseDTO,
  VerifyEmailRequestDTO,
  ForgotPasswordRequestDTO,
  ResetPasswordRequestDTO,
  UpdateProfileRequestDTO,
  ChangePasswordRequestDTO,
  GoogleLoginRequestDTO
} from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;
  
  // Access Token Storage Key
  private readonly TOKEN_KEY = 'sky_flight_access_token';
  private readonly USER_KEY = 'sky_flight_user_info';

  // Signals for reactive state
  private currentUserSignal = signal<UserResponseDTO | null>(this.getStoredUser());
  private accessTokenSignal = signal<string | null>(this.getStoredToken());

  public currentUser = computed(() => this.currentUserSignal());
  public isAuthenticated = computed(() => !!this.accessTokenSignal());
  public isAdmin = computed(() => {
    const role = this.currentUserSignal()?.role;
    return role === 'ADMIN' || role === 'ROLE_ADMIN' 
  });

  constructor(private http: HttpClient, private router: Router) {
    // If token exists, verify current user
    if (this.getStoredToken()) {
      this.fetchCurrentUser().subscribe({
        error: () => this.handleSessionExpired()
      });
    }
  }

  public getAccessToken(): string | null {
    return this.accessTokenSignal();
  }

  public login(credentials: LoginRequestDTO): Observable<AuthResponseDTO> {
    return this.http.post<AuthResponseDTO>(`${this.apiUrl}/login`, credentials, { withCredentials: true }).pipe(
      tap(response => this.handleAuthSuccess(response))
    );
  }

  public register(data: RegisterRequestDTO): Observable<AuthResponseDTO> {
    return this.http.post<AuthResponseDTO>(`${this.apiUrl}/register`, data, { withCredentials: true }).pipe(
      tap(response => this.handleAuthSuccess(response))
    );
  }

  public googleLogin(data: GoogleLoginRequestDTO): Observable<AuthResponseDTO> {
    return this.http.post<AuthResponseDTO>(`${this.apiUrl}/google`, data, { withCredentials: true }).pipe(
      tap(response => this.handleAuthSuccess(response))
    );
  }

  public refreshToken(): Observable<AuthResponseDTO> {
    return this.http.post<AuthResponseDTO>(`${this.apiUrl}/refresh-token`, {}, { withCredentials: true }).pipe(
      tap(response => {
        if (response && response.accessToken) {
          this.setAccessToken(response.accessToken);
          if (response.user) {
            this.setCurrentUser(response.user);
          }
        }
      }),
      catchError(err => {
        this.clearAuthState();
        return throwError(() => err);
      })
    );
  }

  public fetchCurrentUser(): Observable<UserResponseDTO> {
    return this.http.get<UserResponseDTO>(`${this.apiUrl}/me`, { withCredentials: true }).pipe(
      tap(user => this.setCurrentUser(user))
    );
  }

  public logout(): void {
    this.http.post(`${this.apiUrl}/logout`, {}, { withCredentials: true, responseType: 'text' }).subscribe({
      next: () => this.clearAuthStateAndRedirect(),
      error: () => this.clearAuthStateAndRedirect()
    });
  }

  public verifyEmail(dto: VerifyEmailRequestDTO): Observable<string> {
    return this.http.post(`${this.apiUrl}/verify-email`, dto, { responseType: 'text' });
  }

  public resendVerification(email: string): Observable<string> {
    return this.http.post(`${this.apiUrl}/resend-verification?email=${encodeURIComponent(email)}`, {}, { responseType: 'text' });
  }

  public forgotPassword(dto: ForgotPasswordRequestDTO): Observable<string> {
    return this.http.post(`${this.apiUrl}/forgot-password`, dto, { responseType: 'text' });
  }

  public resetPassword(dto: ResetPasswordRequestDTO): Observable<string> {
    return this.http.post(`${this.apiUrl}/reset-password`, dto, { responseType: 'text' });
  }

  public handleAuthSuccess(authResponse: AuthResponseDTO): void {
    if (authResponse.accessToken) {
      this.setAccessToken(authResponse.accessToken);
    }
    if (authResponse.user) {
      this.setCurrentUser(authResponse.user);
    }
  }

  public setAccessToken(token: string): void {
    sessionStorage.setItem(this.TOKEN_KEY, token);
    this.accessTokenSignal.set(token);
  }

  public setCurrentUser(user: UserResponseDTO): void {
    sessionStorage.setItem(this.USER_KEY, JSON.stringify(user));
    this.currentUserSignal.set(user);
  }

  private getStoredToken(): string | null {
    return sessionStorage.getItem(this.TOKEN_KEY);
  }

  private getStoredUser(): UserResponseDTO | null {
    const data = sessionStorage.getItem(this.USER_KEY);
    if (!data) return null;
    try {
      return JSON.parse(data);
    } catch {
      return null;
    }
  }

  private handleSessionExpired(): void {
    this.clearAuthState();
  }

  public clearAuthState(): void {
    sessionStorage.removeItem(this.TOKEN_KEY);
    sessionStorage.removeItem(this.USER_KEY);
    this.accessTokenSignal.set(null);
    this.currentUserSignal.set(null);
  }

  public clearAuthStateAndRedirect(): void {
    this.clearAuthState();
    this.router.navigate(['/login']);
  }
}
