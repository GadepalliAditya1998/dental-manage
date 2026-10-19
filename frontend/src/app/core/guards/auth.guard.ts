import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    // 1. If no token exists in localStorage, redirect directly to /login
    if (!this.authService.hasToken()) {
      return this.router.createUrlTree(['/login']);
    }

    // 2. If token exists, make a call to backend to validate whether token is valid or not
    return this.authService.validateToken().pipe(
      map((res) => {
        if (res && res.data && res.data.valid) {
          return true; // Token is valid, proceed to destination
        }
        // If response indicated invalid
        this.authService.logout('Your session is no longer active. Please log in again.');
        return this.router.createUrlTree(['/login']);
      }),
      catchError((error) => {
        // If we get 403 (or 401) from the backend, ask the user to relogin
        if (error.status === 403) {
          const reason = error.error?.message || 'Access Forbidden: You do not have permissions to access the clinic portal. Please log in again.';
          this.authService.logout(reason);
        } else if (error.status === 401) {
          this.authService.logout('Session expired. Please log in again.');
        } else {
          // If backend server is not running or other error
          console.warn('Backend validation check failed:', error);
          this.authService.logout('Unable to verify credentials with server. Please log in again.');
        }
        return of(this.router.createUrlTree(['/login']));
      })
    );
  }
}
