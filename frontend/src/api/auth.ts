import { apiClient } from "./client";
import type { AuthResponse, UserResponse } from "./types";

export function register(email: string, password: string, displayName: string) {
  return apiClient
    .post<AuthResponse>("/api/auth/register", { email, password, displayName: displayName || null })
    .then((r) => r.data);
}

export function login(email: string, password: string) {
  return apiClient.post<AuthResponse>("/api/auth/login", { email, password }).then((r) => r.data);
}

export function me() {
  return apiClient.get<UserResponse>("/api/auth/me").then((r) => r.data);
}
