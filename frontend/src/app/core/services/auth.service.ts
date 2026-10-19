import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { LoginRequest, LoginResponse, TokenValidationResponse, ContextUser } from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly baseUrl = `${environment.apiUrl}/auth`;
  private readonly TOKEN_KEY = 'auth_token';
  private readonly CONTEXT_USER_KEY = 'context_user';

  private contextUserSubject = new BehaviorSubject<ContextUser | null>(this.getStoredContextUser());
  public contextUser$ = this.contextUserSubject.asObservable();

  private reloginMessageSubject = new BehaviorSubject<string | null>(null);
  public reloginMessage$ = this.reloginMessageSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  /**
   * Helper to safely base64 encode strings (UTF-8 safe)
   */
  private encodeBase64(str: string): string {
    return btoa(unescape(encodeURIComponent(str)));
  }

  /**
   * Unified login for both Doctor and Receptionist.
   * Encodes credentials in base64 as required by backend.
   */
  login(username: string, password: string): Observable<ApiResponse<LoginResponse>> {
    const payload: LoginRequest = {
      username: this.encodeBase64(username),
      password: this.encodeBase64(password)
    };

    return this.http.post<ApiResponse<LoginResponse>>(`${this.baseUrl}/login`, payload).pipe(
      tap((res) => {
        if (res?.data?.token) {
          // Construct ContextUser object containing user data excluding password
          const contextUser: ContextUser = {
            name: res.data.fullName || res.data.username,
            role: res.data.role,
            email: res.data.email,
            phoneNumber: res.data.phoneNumber,
            token: res.data.token,
            username: res.data.username,
            clinicId: res.data.clinicId,
            doctorId: res.data.doctorId
          };

          this.setContextUser(contextUser);
          this.clearReloginMessage();
        }
      })
    );
  }

  /**
   * Validates the currently held token against the backend endpoint GET /api/auth/validate
   */
  validateToken(): Observable<ApiResponse<TokenValidationResponse>> {
    return this.http.get<ApiResponse<TokenValidationResponse>>(`${this.baseUrl}/validate`).pipe(
      tap((res) => {
        if (res?.data && res.data.valid) {
          const current = this.getContextUser();
          if (current) {
            // Update role/metadata if returned by token claims
            if (res.data.role && res.data.role !== current.role) {
              current.role = res.data.role;
            }
            if (res.data.fullName) {
              current.name = res.data.fullName;
            }
            this.setContextUser(current);
          }
        }
      })
    );
  }

  /**
   * Store token and contextUser in localStorage (excludes password)
   */
  private setContextUser(user: ContextUser): void {
    localStorage.setItem(this.TOKEN_KEY, user.token);
    localStorage.setItem(this.CONTEXT_USER_KEY, JSON.stringify(user));
    this.contextUserSubject.next(user);
  }

  /**
   * Retrieve ContextUser from localStorage
   */
  private getStoredContextUser(): ContextUser | null {
    const stored = localStorage.getItem(this.CONTEXT_USER_KEY);
    if (!stored) return null;
    try {
      return JSON.parse(stored);
    } catch {
      return null;
    }
  }

  /**
   * Retrieve current ContextUser snapshot
   */
  getContextUser(): ContextUser | null {
    return this.contextUserSubject.value;
  }

  /**
   * Get raw token from localStorage or contextUser
   */
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY) || this.getContextUser()?.token || null;
  }

  hasToken(): boolean {
    return !!this.getToken();
  }

  isDoctor(): boolean {
    return this.getContextUser()?.role?.toUpperCase() === 'DOCTOR';
  }

  isReceptionist(): boolean {
    return this.getContextUser()?.role?.toUpperCase() === 'RECEPTIONIST';
  }

  setReloginMessage(message: string): void {
    this.reloginMessageSubject.next(message);
  }

  clearReloginMessage(): void {
    this.reloginMessageSubject.next(null);
  }

  /**
   * Clear session and redirect to /login
   */
  logout(reason?: string): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.CONTEXT_USER_KEY);
    localStorage.removeItem('auth_user'); // Clean up any previous keys
    this.contextUserSubject.next(null);
    if (reason) {
      this.setReloginMessage(reason);
    }
    this.router.navigate(['/login']);
  }
}
