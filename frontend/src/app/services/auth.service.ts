import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, interval } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface JwtResponse {
  token: string;
  type: string;
  id: number;
  username: string;
  email: string;
  roles: string[];
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';
  private currentUserSubject = new BehaviorSubject<JwtResponse | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();
  private tokenCheckInterval: any;
  private readonly TOKEN_CHECK_INTERVAL = 60000; // Check every minute
  private readonly SESSION_TIMEOUT_WARNING = 5 * 60 * 1000; // 5 minutes before expiration

  constructor(private http: HttpClient, private router: Router) {
    this.initializeSession();
  }

  private initializeSession(): void {
    const storedUser = localStorage.getItem('currentUser');
    const storedToken = localStorage.getItem('token');
    
    if (storedUser && storedToken) {
      // Validate token before restoring session
      if (this.isTokenValid(storedToken)) {
        this.currentUserSubject.next(JSON.parse(storedUser));
        this.startTokenValidation();
      } else {
        // Token expired, clear session
        this.clearSession();
      }
    }
  }

  private startTokenValidation(): void {
    // Clear any existing interval
    if (this.tokenCheckInterval) {
      clearInterval(this.tokenCheckInterval);
    }

    // Check token validity periodically
    this.tokenCheckInterval = setInterval(() => {
      const token = this.getToken();
      if (token && !this.isTokenValid(token)) {
        this.handleTokenExpiration();
      }
    }, this.TOKEN_CHECK_INTERVAL);
  }

  private stopTokenValidation(): void {
    if (this.tokenCheckInterval) {
      clearInterval(this.tokenCheckInterval);
      this.tokenCheckInterval = null;
    }
  }

  checkBackendHealth(): Observable<any> {
    return this.http.get(`${this.apiUrl}/health`);
  }

  register(credentials: RegisterRequest): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.apiUrl}/register`, credentials).pipe(
      tap(response => {
        this.setSession(response);
      })
    );
  }

  login(credentials: LoginRequest): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        this.setSession(response);
      })
    );
  }

  private setSession(response: JwtResponse): void {
    localStorage.setItem('token', response.token);
    localStorage.setItem('currentUser', JSON.stringify(response));
    localStorage.setItem('tokenExpiry', this.getTokenExpiry(response.token).toString());
    this.currentUserSubject.next(response);
    this.startTokenValidation();
  }

  logout(): void {
    this.clearSession();
    this.router.navigate(['/auth']);
  }

  private clearSession(): void {
    this.stopTokenValidation();
    localStorage.removeItem('token');
    localStorage.removeItem('currentUser');
    localStorage.removeItem('tokenExpiry');
    this.currentUserSubject.next(null);
  }

  private handleTokenExpiration(): void {
    this.clearSession();
    this.router.navigate(['/auth'], { 
      queryParams: { 
        expired: 'true',
        message: 'Your session has expired. Please login again.' 
      } 
    });
  }

  getToken(): string | null {
    const token = localStorage.getItem('token');
    if (token && this.isTokenValid(token)) {
      return token;
    }
    // Token expired or invalid
    if (token) {
      this.handleTokenExpiration();
    }
    return null;
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    return !!token && this.isTokenValid(token);
  }

  /**
   * Check if JWT token is valid and not expired
   */
  private isTokenValid(token: string): boolean {
    try {
      const payload = this.decodeToken(token);
      if (!payload || !payload.exp) {
        return false;
      }
      
      // Check if token is expired (with 30 second buffer)
      const expirationTime = payload.exp * 1000; // Convert to milliseconds
      const currentTime = Date.now();
      const bufferTime = 30 * 1000; // 30 seconds buffer
      
      return expirationTime > (currentTime + bufferTime);
    } catch (error) {
      return false;
    }
  }

  /**
   * Decode JWT token without verification (for expiration check only)
   */
  private decodeToken(token: string): any {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      return JSON.parse(jsonPayload);
    } catch (error) {
      return null;
    }
  }

  /**
   * Get token expiration timestamp
   */
  private getTokenExpiry(token: string): number {
    const payload = this.decodeToken(token);
    return payload?.exp ? payload.exp * 1000 : 0;
  }

  /**
   * Get time remaining until token expiration (in milliseconds)
   */
  getTokenTimeRemaining(): number {
    const token = this.getToken();
    if (!token) {
      return 0;
    }
    
    const expiry = this.getTokenExpiry(token);
    const remaining = expiry - Date.now();
    return remaining > 0 ? remaining : 0;
  }

  /**
   * Check if token is about to expire (within warning time)
   */
  isTokenExpiringSoon(): boolean {
    const timeRemaining = this.getTokenTimeRemaining();
    return timeRemaining > 0 && timeRemaining < this.SESSION_TIMEOUT_WARNING;
  }

  hasRole(role: string): boolean {
    const user = this.currentUserSubject.value;
    return user?.roles.includes(role) || false;
  }

  getCurrentUser(): JwtResponse | null {
    // Validate token before returning user
    if (this.currentUserSubject.value && !this.isAuthenticated()) {
      this.handleTokenExpiration();
      return null;
    }
    return this.currentUserSubject.value;
  }

  /**
   * Validate session by checking token with backend
   */
  validateSession(): Observable<boolean> {
    return new Observable(observer => {
      if (!this.isAuthenticated()) {
        observer.next(false);
        observer.complete();
        return;
      }

      // Make a lightweight request to validate token
      this.http.get(`${this.apiUrl}/me`).subscribe({
        next: () => {
          observer.next(true);
          observer.complete();
        },
        error: () => {
          this.handleTokenExpiration();
          observer.next(false);
          observer.complete();
        }
      });
    });
  }
}

