import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { ApiResponse, AuthResponse, UserSummary } from '../models/banking.models';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = '/api/v1/auth';
  private currentUserSubject = new BehaviorSubject<UserSummary | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {
    const savedUser = localStorage.getItem('bankease_user');
    if (savedUser) {
      try {
        this.currentUserSubject.next(JSON.parse(savedUser));
      } catch (e) {
        localStorage.removeItem('bankease_user');
      }
    }
  }

  public get currentUserValue(): UserSummary | null {
    return this.currentUserSubject.value;
  }

  public get isAuthenticated(): boolean {
    return !!this.getToken();
  }

  public getToken(): string | null {
    return localStorage.getItem('bankease_token');
  }

  public getRefreshToken(): string | null {
    return localStorage.getItem('bankease_refresh_token');
  }

  login(credentials: { email: string; password: string }): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/login`, credentials).pipe(
      tap(res => {
        if (res.success && res.data) {
          localStorage.setItem('bankease_token', res.data.accessToken);
          localStorage.setItem('bankease_refresh_token', res.data.refreshToken);
          localStorage.setItem('bankease_user', JSON.stringify(res.data.user));
          this.currentUserSubject.next(res.data.user);
        }
      })
    );
  }

  register(userData: any): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/register`, userData).pipe(
      tap(res => {
        if (res.success && res.data) {
          localStorage.setItem('bankease_token', res.data.accessToken);
          localStorage.setItem('bankease_refresh_token', res.data.refreshToken);
          localStorage.setItem('bankease_user', JSON.stringify(res.data.user));
          this.currentUserSubject.next(res.data.user);
        }
      })
    );
  }

  logout(): void {
    const token = this.getRefreshToken();
    if (token) {
      this.http.post(`${this.apiUrl}/logout`, { refreshToken: token }).subscribe({
        error: () => {}
      });
    }
    localStorage.removeItem('bankease_token');
    localStorage.removeItem('bankease_refresh_token');
    localStorage.removeItem('bankease_user');
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }
}
