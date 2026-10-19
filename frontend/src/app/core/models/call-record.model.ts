import { PatientResponse } from './patient.model';

export type CallType = 'AI_INBOUND' | 'HUMAN_TRANSFER';

export interface CallRecordResponse {
  id: number;
  clinicId?: number;
  retellCallId?: string;
  patient?: PatientResponse;
  callType: string;
  callStatus?: string;
  callerPhone?: string;
  durationMs?: number;
  transcript?: string;
  recordingUrl?: string;
  sentiment?: string;
  sentimentScore?: number;
  callSummary?: string;
  transferredToHuman?: boolean;
  humanCallRecordingUrl?: string;
  humanCallTranscript?: string;
  callStartedAt?: string;
  callEndedAt?: string;
  createdOn?: string;
}
