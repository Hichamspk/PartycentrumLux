import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.accessToken;
  const authenticatedRequest = token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;

  return next(authenticatedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      const isAuthCall = request.url.includes('/auth/login') || request.url.includes('/auth/refresh');
      if (error.status !== 401 || !auth.refreshToken || isAuthCall) {
        return throwError(() => error);
      }

      return auth.refresh().pipe(
        switchMap(() => next(request.clone({ setHeaders: { Authorization: `Bearer ${auth.accessToken}` } }))),
        catchError((refreshError) => {
          auth.logout();
          void router.navigate(['/login']);
          return throwError(() => refreshError);
        })
      );
    })
  );
};
