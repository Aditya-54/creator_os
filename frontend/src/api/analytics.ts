import { apiClient } from "./client";
import type {
  AiQueryResponse,
  ContentAnalyticsResponse,
  ContentResponse,
  ExplanationResponse,
  GrowthPoint,
  OverviewResponse,
  PageResponse,
  PlatformPerformanceResponse,
  RegionPerformanceResponse,
  SocialAccountResponse,
  TopicPerformanceResponse,
  ViralEventResponse,
} from "./types";

export const getOverview = () => apiClient.get<OverviewResponse>("/api/analytics/overview").then((r) => r.data);

export const getTopics = () =>
  apiClient.get<TopicPerformanceResponse[]>("/api/analytics/topics").then((r) => r.data);

export const getRegions = () =>
  apiClient.get<RegionPerformanceResponse[]>("/api/analytics/regions").then((r) => r.data);

export const getPlatforms = () =>
  apiClient.get<PlatformPerformanceResponse[]>("/api/analytics/platforms").then((r) => r.data);

export const getContentAnalytics = (id: string) =>
  apiClient.get<ContentAnalyticsResponse>(`/api/analytics/content/${id}`).then((r) => r.data);

export const getTimeline = (id: string) =>
  apiClient.get<GrowthPoint[]>(`/api/analytics/content/${id}/timeline`).then((r) => r.data);

export const getViralEvents = (id: string) =>
  apiClient.get<ViralEventResponse[]>(`/api/analytics/content/${id}/viral-events`).then((r) => r.data);

export const getExplanation = (id: string) =>
  apiClient.get<ExplanationResponse>(`/api/analytics/content/${id}/explanation`).then((r) => r.data);

export const listContent = (page = 0, size = 20) =>
  apiClient
    .get<PageResponse<ContentResponse>>("/api/content", { params: { page, size } })
    .then((r) => r.data);

export const getContent = (id: string) =>
  apiClient.get<ContentResponse>(`/api/content/${id}`).then((r) => r.data);

export const listSocialAccounts = () =>
  apiClient.get<SocialAccountResponse[]>("/api/social-accounts").then((r) => r.data);

export const connectSocialAccount = (platform: string, handle: string) =>
  apiClient.post<SocialAccountResponse>("/api/social-accounts", { platform, handle }).then((r) => r.data);

export const disconnectSocialAccount = (id: string) => apiClient.delete(`/api/social-accounts/${id}`);

export const triggerSync = (id: string) => apiClient.post(`/api/social-accounts/${id}/sync`);

export const seedDemoData = () => apiClient.post("/api/demo/seed").then((r) => r.data);

export const resetDemoData = () => apiClient.delete("/api/demo/reset");

export const askAi = (question: string) =>
  apiClient.post<AiQueryResponse>("/api/ai/query", { question }).then((r) => r.data);
