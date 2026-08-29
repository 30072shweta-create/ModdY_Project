import { inject } from '@angular/core';
import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, switchMap, throwError, BehaviorSubject, filter, take } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

const isPublicEndpoint = (url: string): boolean => {
  return url.includes('/auth/') ||
         url.includes('/api/airports') ||
         url.includes('/api/airlines') ||
         url.includes('/api/flights');
};

export const jwtInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const authService = inject(AuthService);
  const token = authService.getAccessToken();

  let modifiedReq = req;

  // Add Authorization header and withCredentials for API calls
  if (req.url.startsWith(environment.apiUrl)) {
    const headers: Record<string, string> = {};
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    modifiedReq = req.clone({
      setHeaders: headers,
      withCredentials: true
    });
  }

  return next(modifiedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Avoid refresh loop for public endpoints, /auth/* endpoints, or when user has no active token
      if (error.status === 401 && !isPublicEndpoint(req.url) && token) {
        if (!isRefreshing) {
          isRefreshing = true;
          refreshTokenSubject.next(null);

          return authService.refreshToken().pipe(
            switchMap(res => {
              isRefreshing = false;
              refreshTokenSubject.next(res.accessToken);

              return next(req.clone({
                setHeaders: { Authorization: `Bearer ${res.accessToken}` },
                withCredentials: true
              }));
            }),
            catchError(refreshError => {
              isRefreshing = false;
              authService.clearAuthStateAndRedirect();
              return throwError(() => refreshError);
            })
          );
        } else {
          return refreshTokenSubject.pipe(
            filter(newToken => newToken !== null),
            take(1),
            switchMap(newToken => {
              return next(req.clone({
                setHeaders: { Authorization: `Bearer ${newToken}` },
                withCredentials: true
              }));
            })
          );
        }
      }

      return throwError(() => error);
    })
  );
};
