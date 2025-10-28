import api from './axios';
import type { UserResponse } from '@/types/api/user';

/**
 * 현재 로그인한 사용자 정보 조회
 *
 * JWT 토큰이 localStorage에 저장되어 있으면 Axios 인터셉터가 자동으로 주입
 *
 * @returns UserResponse
 * @throws 401 Unauthorized if JWT token is invalid or expired
 */
export async function getCurrentUser(): Promise<UserResponse> {
  const response = await api.get<UserResponse>('/api/auth/me');
  return response.data;
}

/**
 * 로그아웃 처리
 *
 * 클라이언트에서 JWT 토큰 삭제 (localStorage.removeItem)
 * 백엔드 /api/auth/logout 호출 (향후 JWT 블랙리스트 확장용)
 *
 * @returns { message: string }
 */
export async function logout(): Promise<{ message: string }> {
  const response = await api.post<{ message: string }>('/api/auth/logout');

  // 로컬 스토리지에서 JWT 토큰 제거
  if (typeof window !== 'undefined') {
    localStorage.removeItem('access_token');
  }

  return response.data;
}
