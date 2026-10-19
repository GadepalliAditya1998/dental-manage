import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'statusBadge',
  standalone: false
})
export class StatusBadgePipe implements PipeTransform {
  transform(status: string | undefined): { text: string; cssClass: string } {
    switch (status) {
      case 'SCHEDULED':
        return { text: 'Scheduled', cssClass: 'badge badge-scheduled' };
      case 'CONFIRMED':
        return { text: 'Confirmed', cssClass: 'badge badge-confirmed' };
      case 'REMINDER_SENT':
        return { text: 'Reminder Sent', cssClass: 'badge bg-info-subtle text-info-emphasis' };
      case 'RESCHEDULED':
        return { text: 'Rescheduled', cssClass: 'badge bg-warning-subtle text-warning-emphasis' };
      case 'CANCELLED':
        return { text: 'Cancelled', cssClass: 'badge badge-cancelled' };
      case 'NO_SHOW':
        return { text: 'No Show', cssClass: 'badge badge-no-show' };
      case 'COMPLETED':
        return { text: 'Completed', cssClass: 'badge badge-completed' };
      case 'AI_INBOUND':
        return { text: 'AI Inbound', cssClass: 'badge bg-primary-subtle text-primary' };
      case 'HUMAN_TRANSFER':
        return { text: 'Transferred', cssClass: 'badge bg-danger-subtle text-danger' };
      default:
        return { text: status || 'Unknown', cssClass: 'badge bg-secondary' };
    }
  }
}
