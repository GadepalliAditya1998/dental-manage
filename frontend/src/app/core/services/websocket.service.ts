import { Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService implements OnDestroy {
  private client: Client | null = null;
  private callsSubject = new Subject<any>();
  private appointmentsSubject = new Subject<any>();
  private metricsSubject = new Subject<any>();
  private isConnected = false;

  constructor() {
    this.initStomp();
  }

  private initStomp(): void {
    try {
      this.client = new Client({
        webSocketFactory: () => new SockJS(environment.wsUrl),
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          this.isConnected = true;
          this.subscribeToTopics();
        },
        onDisconnect: () => {
          this.isConnected = false;
        },
        onStompError: (frame) => {
          console.warn('STOMP protocol error:', frame);
        },
        onWebSocketError: (err) => {
          // Graceful warning when backend is offline
          console.warn('WebSocket connection attempt failed. Backend may be offline.', err);
        }
      });

      this.client.activate();
    } catch (e) {
      console.warn('WebSocket initialization skipped or failed:', e);
    }
  }

  private subscribeToTopics(): void {
    if (!this.client || !this.isConnected) return;

    this.client.subscribe('/topic/calls', (message: IMessage) => {
      try {
        const payload = JSON.parse(message.body);
        this.callsSubject.next(payload);
      } catch (err) {
        console.error('Failed to parse call websocket payload', err);
      }
    });

    this.client.subscribe('/topic/appointments', (message: IMessage) => {
      try {
        const payload = JSON.parse(message.body);
        this.appointmentsSubject.next(payload);
      } catch (err) {
        console.error('Failed to parse appointment websocket payload', err);
      }
    });

    this.client.subscribe('/topic/metrics', (message: IMessage) => {
      try {
        const payload = JSON.parse(message.body);
        this.metricsSubject.next(payload);
      } catch (err) {
        console.error('Failed to parse metrics websocket payload', err);
      }
    });
  }

  onCallUpdate(): Observable<any> {
    return this.callsSubject.asObservable();
  }

  onAppointmentUpdate(): Observable<any> {
    return this.appointmentsSubject.asObservable();
  }

  onMetricsUpdate(): Observable<any> {
    return this.metricsSubject.asObservable();
  }

  get connected(): boolean {
    return this.isConnected;
  }

  ngOnDestroy(): void {
    if (this.client) {
      this.client.deactivate();
    }
  }
}
