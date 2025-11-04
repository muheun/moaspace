import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as authApi from '@/lib/api/auth';
import type { UserResponse } from '@/types/api/user';
import { clearAuthCookie } from '@/lib/actions/auth';

/**
 * 현재 로그인한 사용자 정보를 가져오는 커스텀 훅
 *
 * TanStack Query를 사용하여 서버 상태 관리 (Constitution Principle VII)
 *
 * @param enabled - 쿼리 실행 여부 (기본값: true). false일 경우 API 호출하지 않음
 * @returns { data: UserResponse | undefined, isLoading, error, refetch }
 */
export function useAuth(enabled: boolean = true) {
  return useQuery<UserResponse>({
    queryKey: ['auth', 'me'],
    queryFn: authApi.getCurrentUser,
    retry: false, // JWT 토큰이 없거나 만료된 경우 재시도하지 않음
    staleTime: 1000 * 60 * 5, // 5분간 stale 상태 유지 (캐시 유지)
    enabled, // 조건부 실행: enabled가 false면 API 호출하지 않음
    refetchOnWindowFocus: false, // 윈도우 포커스 시 refetch 방지
    refetchOnReconnect: false, // 네트워크 재연결 시 refetch 방지
    // refetchOnMount는 true (기본값) - 처음 마운트 시 API 호출 필요
  });
}

/**
 * 로그아웃 기능을 제공하는 커스텀 훅
 *
 * 로그아웃 시:
 * 1. 백엔드 /api/auth/logout 호출
 * 2. localStorage에서 access_token 제거
 * 3. Cookie에서 access_token 제거 (Middleware 인증용)
 * 4. React Query 캐시 초기화
 *
 * @returns { mutate: (void) => void, isLoading, error }
 */
export function useLogout() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      await authApi.logout();
      await clearAuthCookie();
    },
    onSuccess: () => {
      // 모든 쿼리 캐시 무효화 (사용자 정보, 게시글 등)
      queryClient.clear();
    },
  });
}
