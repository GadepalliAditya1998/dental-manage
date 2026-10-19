import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AlertService } from '../../../core/services/alert.service';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  loading = false;
  showPassword = false;
  errorMessage = '';
  reloginMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private alertService: AlertService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initForm();

    // Listen for relogin message (e.g. from 403 forbidden or expired token)
    this.authService.reloginMessage$.subscribe((msg) => {
      this.reloginMessage = msg;
    });
  }

  initForm(): void {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.reloginMessage = null;

    const { username, password } = this.loginForm.value;

    this.authService.login(username, password).subscribe({
      next: (res) => {
        this.loading = false;
        // Successful login -> navigate to dashboard
        // The dashboard and shell will render the role-specific view based on contextUser
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 403) {
          // 403: Patients / unauthorized roles
          const msg = err.error?.message || "You don't have access. Only Doctors and Receptionists are permitted portal access.";
          this.errorMessage = msg;
          this.alertService.showAccessDenied(msg);
        } else if (err.status === 401) {
          // 401: Invalid credentials
          this.errorMessage = err.error?.message || 'Invalid username or password. Please verify your credentials.';
        } else {
          // Server offline or connection error
          this.errorMessage = err.error?.message || 'Unable to connect to the authentication server.';
        }
      }
    });
  }
}
