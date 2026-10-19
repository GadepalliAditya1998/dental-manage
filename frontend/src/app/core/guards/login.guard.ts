import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class LoginGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    // If no token exists, allow access to login page
    if (!this.authService.hasToken()) {
      return true;
    }

    // If token exists, validate with backend
    return this.authService.validateToken().pipe(
      map((res) => {
        if (res && res.data && res.data.valid) {
          // Token is valid: navigate to dashboard
          return this.router.createUrlTree(['/dashboard']);
        }
        return true;
      }),
      catchError((error) => {
        // If 403 or invalid: clear token and stay on login page
        this.authService.logout(
          error.status === 403 ? "You don't have access. Please log in with valid credentials." : undefined
        );
        return of(true);
      })
    );
  }
}
