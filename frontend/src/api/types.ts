export interface UserResponse {
  id: string;
  email: string;
  displayName: string | null;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: UserResponse;
}

export type Platform = "YOUTUBE" | "INSTAGRAM" | "MOCK";
export type SocialAccountStatus = "CONNECTED" | "TOKEN_EXPIRED" | "ERROR" | "DISCONNECTED";
export type ContentType = "VIDEO" | "SHORT" | "REEL" | "POST" | "STORY";
export type ViralConfidence = "LOW" | "MEDIUM" | "HIGH";

export interface SocialAccountResponse {
  id: string;
  platform: Platform;
  username: string | null;
  status: SocialAccountStatus;
  connectedAt: string;
  lastSyncedAt: string | null;
}

export interface ContentResponse {
  id: string;
  platform: Platform;
  title: string | null;
  description: string | null;
  contentType: ContentType;
  publishedAt: string | null;
  durationSeconds: number | null;
  language: string | null;
  country: string | null;
  category: string | null;
  thumbnailUrl: string | null;
  demo: boolean;
  topics: string[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ContentSummary {
  id: string;
  title: string | null;
  platform: Platform;
  views: number;
  engagementRate: number;
}

export interface ViralEventResponse {
  id: string;
  contentId: string;
  contentTitle: string | null;
  startTime: string;
  endTime: string | null;
  peakGrowth: number;
  baselineGrowth: number;
  multiplier: number;
  confidence: ViralConfidence;
  explanation: string;
}

export interface OverviewResponse {
  totalContent: number;
  totalViews: number;
  totalLikes: number;
  totalComments: number;
  totalShares: number;
  avgEngagementRate: number;
  topContent: ContentSummary[];
  platformBreakdown: Record<string, number>;
  recentViralEvents: ViralEventResponse[];
}

export interface ContentAnalyticsResponse {
  contentId: string;
  title: string | null;
  platform: Platform;
  totalViews: number;
  totalLikes: number;
  totalComments: number;
  totalShares: number;
  engagementRate: number;
  latestGrowthPerHour: number;
  latestAccelerationPerHour: number;
  topics: string[];
}

export interface GrowthPoint {
  capturedAt: string;
  views: number;
  likes: number;
  comments: number;
  shares: number;
  growthViewsPerHour: number;
  accelerationViewsPerHour: number;
  engagementRate: number;
}

export interface ExplanationResponse {
  contentId: string;
  turningPoint: string | null;
  viralEventDetected: boolean;
  evidence: string[];
  possibleFactors: string[];
  comparisons: string[];
  limitations: string[];
}

export interface TopicPerformanceResponse {
  topic: string;
  contentCount: number;
  totalViews: number;
  avgEngagementRate: number;
  viewsByPlatform: Record<string, number>;
  topRegion: string | null;
}

export interface RegionPerformanceResponse {
  country: string;
  totalViews: number;
  totalEngagement: number;
  topTopic: string | null;
}

export interface PlatformPerformanceResponse {
  platform: Platform;
  contentCount: number;
  totalViews: number;
  avgEngagementRate: number;
}

export interface AiQueryResponse {
  question: string;
  aiGenerated: boolean;
  provider: string;
  answer: string;
  fallbackOverview: OverviewResponse | null;
  limitations: string | null;
}

export interface ApiError {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  fieldErrors?: { field: string; message: string }[];
}
