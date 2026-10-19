import { Component, OnInit } from '@angular/core';
import { CallRecordService } from '../../../core/services/call-record.service';
import { CallRecordResponse } from '../../../core/models/call-record.model';

@Component({
  selector: 'app-call-list',
  standalone: false,
  templateUrl: './call-list.component.html',
  styleUrls: ['./call-list.component.scss']
})
export class CallListComponent implements OnInit {
  calls: CallRecordResponse[] = [];
  filteredCalls: CallRecordResponse[] = [];
  selectedCall: CallRecordResponse | null = null;
  loading = false;
  errorMessage = '';

  filterType = '';
  searchQuery = '';

  constructor(private callRecordService: CallRecordService) {}

  ngOnInit(): void {
    this.loadCalls();
  }

  loadCalls(): void {
    this.loading = true;
    this.errorMessage = '';

    const filters: any = {};
    if (this.filterType) filters.callType = this.filterType;

    this.callRecordService.getCalls(filters).subscribe({
      next: (res) => {
        this.calls = res?.data || [];
        this.applyFilter();
        this.loading = false;
      },
      error: (err) => {
        console.warn('Calls API error:', err);
        this.errorMessage = 'Backend endpoint GET /api/calls is pending or unreachable.';
        this.loading = false;
      }
    });
  }

  applyFilter(): void {
    let result = this.calls;

    if (this.filterType) {
      result = result.filter(c => c.callType === this.filterType);
    }

    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      result = result.filter(c =>
        (c.callerPhone?.toLowerCase().includes(q) ||
        c.callSummary?.toLowerCase().includes(q) ||
        c.retellCallId?.toLowerCase().includes(q) ||
        c.sentiment?.toLowerCase().includes(q))
      );
    }

    this.filteredCalls = result;
  }

  openCallModal(call: CallRecordResponse): void {
    this.selectedCall = call;
  }

  closeModal(): void {
    this.selectedCall = null;
  }
}
