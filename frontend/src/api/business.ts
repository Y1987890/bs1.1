import { request } from './request';

export type PositionStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
export type TrainingPlanStatus = 'ACTIVE' | 'COMPLETED';
export type GrowthRecordStatus = 'OPEN' | 'DONE';

export interface JobRequirement {
  dimension: string;
  weight: number;
  description: string;
}

export interface PositionView {
  id: number;
  title: string;
  company: string;
  description: string;
  requirements: JobRequirement[];
  status: PositionStatus;
  ownerId: number;
  createdAt: string;
}

export interface CreateJobRequest {
  title: string;
  company: string;
  description: string;
  requirements: JobRequirement[];
}

export type UpdateJobRequest = CreateJobRequest;

export interface TrainingPlanView {
  id: number;
  ownerUserId: number;
  positionId: number | null;
  title: string;
  goal: string;
  status: TrainingPlanStatus;
  createdAt: string;
  completedAt: string | null;
}

export interface CreateTrainingPlanRequest {
  title: string;
  goal: string;
  positionId?: number | null;
}

export interface GrowthRecordView {
  id: number;
  ownerUserId: number;
  dimension: string;
  issue: string;
  action: string;
  status: GrowthRecordStatus;
  sourceSessionId: string | null;
  createdAt: string;
  doneAt: string | null;
}

export interface CreateGrowthRecordRequest {
  dimension: string;
  issue: string;
  action: string;
  sourceSessionId?: string | null;
}

export interface GrowthTrendPoint {
  sessionId: string;
  overallScore: number;
  createdAt: string;
}

export interface AdminUserView {
  id: number;
  username: string;
  role: string;
  displayName: string | null;
  enabled: boolean;
  createdAt: string;
}

export interface AuditLogView {
  id: number;
  actorUsername: string;
  action: string;
  targetType: string;
  targetId: string | null;
  detail: string;
  createdAt: string;
}

export const jobsApi = {
  list: () => request.get<PositionView[]>('/api/v1/jobs'),
  create: (data: CreateJobRequest) => request.post<PositionView>('/api/v1/jobs', data),
  update: (id: number, data: UpdateJobRequest) => request.put<PositionView>(`/api/v1/jobs/${id}`, data),
  publish: (id: number) => request.post<PositionView>(`/api/v1/jobs/${id}/publish`),
  archive: (id: number) => request.post<PositionView>(`/api/v1/jobs/${id}/archive`),
  remove: (id: number) => request.delete<void>(`/api/v1/jobs/${id}`),
};

export const trainingApi = {
  list: () => request.get<TrainingPlanView[]>('/api/v1/training-plans'),
  create: (data: CreateTrainingPlanRequest) => request.post<TrainingPlanView>('/api/v1/training-plans', data),
  complete: (id: number) => request.post<TrainingPlanView>(`/api/v1/training-plans/${id}/complete`),
};

export const growthApi = {
  list: () => request.get<GrowthRecordView[]>('/api/v1/growth/records'),
  create: (data: CreateGrowthRecordRequest) => request.post<GrowthRecordView>('/api/v1/growth/records', data),
  done: (id: number) => request.post<GrowthRecordView>(`/api/v1/growth/records/${id}/done`),
  trend: () => request.get<GrowthTrendPoint[]>('/api/v1/growth/trend'),
};

export const adminApi = {
  listUsers: () => request.get<AdminUserView[]>('/api/v1/admin/users'),
  updateUserRole: (id: number, role: string) => request.patch<void>(`/api/v1/admin/users/${id}/role`, { role }),
  listAuditLogs: () => request.get<AuditLogView[]>('/api/v1/admin/audit-logs'),
};