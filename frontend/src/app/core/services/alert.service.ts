import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface AlertMessage {
  type: 'danger' | 'warning' | 'info' | 'success';
  title: string;
  message: string;
  id: number;
}

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  private alertsSubject = new BehaviorSubject<AlertMessage[]>([]);
  alerts$: Observable<AlertMessage[]> = this.alertsSubject.asObservable();
  private nextId = 1;

  showAlert(type: 'danger' | 'warning' | 'info' | 'success', message: string, title: string = ''): void {
    const id = this.nextId++;
    const newAlert: AlertMessage = { type, title, message, id };
    const current = this.alertsSubject.value;
    this.alertsSubject.next([...current, newAlert]);

    // Auto-dismiss after 6 seconds
    setTimeout(() => {
      this.dismiss(id);
    }, 6000);
  }

  showAccessDenied(customMessage?: string): void {
    this.showAlert(
      'danger',
      customMessage || "You don't have access to this resource or action.",
      "Access Denied (403)"
    );
  }

  dismiss(id: number): void {
    const current = this.alertsSubject.value.filter(a => a.id !== id);
    this.alertsSubject.next(current);
  }
}
