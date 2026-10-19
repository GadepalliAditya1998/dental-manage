import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { WebSocketService } from './core/services/websocket.service';
import { AuthService } from './core/services/auth.service';
import { AlertService, AlertMessage } from './core/services/alert.service';
import { ContextUser } from './core/models/auth.model';

@Component({
  selector: 'app-root',
  standalone: false,
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class App implements OnInit {
  sidebarOpen = false;
  wsConnected = false;
  isLoginPage = false;
  contextUser: ContextUser | null = null;
  alerts: AlertMessage[] = [];

  constructor(
    private wsService: WebSocketService,
    private authService: AuthService,
    private alertService: AlertService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Track current route to hide shell on login page
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.isLoginPage = event.urlAfterRedirects.includes('/login');
      });

    // Track authenticated context user
    this.authService.contextUser$.subscribe((user) => {
      this.contextUser = user;
    });

    // Track global 403 access denied alerts
    this.alertService.alerts$.subscribe((alerts) => {
      this.alerts = alerts;
    });

    // Check WebSocket state
    this.wsConnected = this.wsService.connected;
    this.wsService.onCallUpdate().subscribe(() => {
      this.wsConnected = true;
    });
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  dismissAlert(id: number): void {
    this.alertService.dismiss(id);
  }
}
