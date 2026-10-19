import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { AlertService } from '../services/alert.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(
    private authService: AuthService,
    private alertService: AlertService
  ) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.authService.getToken();

    // Attach Authorization Bearer token if available
    let authReq = request;
    if (token && !request.headers.has('Authorization')) {
      authReq = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    return next.handle(authReq).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 403) {
          // Requirement: "If any API call responds with 403, we should show the user with a message saying you don't have access"
          const serverMessage = error.error?.message;
          const displayMessage = serverMessage || "You don't have access to this resource or action.";
          this.alertService.showAccessDenied(displayMessage);

          // If the validate endpoint or an auth verification returned 403: ask user to relogin
          if (request.url.includes('/auth/validate') || request.url.includes('/validate')) {
            this.authService.logout("Access Forbidden: Your permissions have changed or token is invalid. Please log in again.");
          }
        } else if (error.status === 401) {
          // If 401 Unauthorized on non-login endpoints
          if (!request.url.includes('/auth/login') && !request.url.includes('/login')) {
            this.authService.logout("Your session has expired. Please log in again.");
          }
        }

        return throwError(() => error);
      })
    );
  }
}
