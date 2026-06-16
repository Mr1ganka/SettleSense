import { apiRequest } from './client';

export type SystemStatus = {
  service: string;
  status: string;
  timestamp: string;
};

export function getSystemStatus() {
  return apiRequest<SystemStatus>('/api/system/status');
}
