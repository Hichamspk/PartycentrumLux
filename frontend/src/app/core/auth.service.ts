import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse, Role } from './models';

const STORAGE_KEY = 'lux_auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = environment.apiUrl;
  readonly session = signal<AuthResponse | null>(this.readSession());

  constructor(private readonly http: HttpClient, private readonly router: Router) {}

  get accessToken(): string | null {
    return this.session()?.accessToken ?? null;
  }

  get refreshToken(): string | null {
    return this.session()?.refreshToken ?? null;
  }

  get role(): Role | null {
    return this.session()?.role ?? null;
  }

  get isOwner(): boolean {
    return this.role === 'OWNER';
  }

  isLoggedIn(): boolean {
    return Boolean(this.accessToken);
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, { email, password }).pipe(
      tap((session) => this.saveSession(session))
    );
  }

  refresh(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/refresh`, { refreshToken: this.refreshToken }).pipe(
      tap((session) => this.saveSession(session))
    );
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.session.set(null);
    void this.router.navigate(['/login']);
  }

  private saveSession(session: AuthResponse): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    this.session.set(session);
  }

  private readSession(): AuthResponse | null {
    try {
      var raw = localStorage.getItem(STORAGE_KEY);
      return raw ? JSON.parse(raw) as AuthResponse : null;
    } catch {
      return null;
    }
  }
}
