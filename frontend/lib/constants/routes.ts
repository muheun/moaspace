/**
 * 중앙 집중식 인증 경로 관리
 *
 * 역할:
 * - 공개/보호 경로를 한 곳에서 정의 (Single Source of Truth)
 * - 패턴 매칭으로 동적 경로 지원 (/posts/[id])
 * - Middleware, axios, Header에서 공통 사용
 * - 경로 추가 시 이 파일만 수정하면 모든 곳에 반영
 */

/**
 * 인증 설정
 */
export const AUTH_CONFIG = {
  /**
   * 완전 공개 경로 (인증 불필요, Header도 useAuth 호출 안함)
   * - 401 에러 발생하지 않음
   * - 로그인 버튼만 표시
   */
  public: [
    '/',              // 메인 페이지
    '/login',         // 로그인 페이지
    '/callback',      // OAuth 콜백
    '/test-editor',   // 에디터 테스트
    '/clear-auth',    // 인증 정보 초기화 (디버깅용)
  ],

  /**
   * 인증 필수 경로 (Middleware + Page + Header에서 인증 체크)
   * - 미인증 시 /login으로 리다이렉트
   * - Header에서 useAuth() 호출하여 사용자 정보 표시
   */
  protected: [
    '/posts',           // 게시판 목록 (인증 필요)
    '/posts/[id]',      // 게시글 상세 (인증 필요)
    '/posts/new',       // 게시글 작성
    '/posts/[id]/edit', // 게시글 수정
  ],
};

/**
 * 패턴 매칭 함수
 *
 * 동적 경로를 지원하는 패턴 매칭
 * 예: '/posts/[id]' 패턴 → '/posts/123' 경로와 매칭
 *
 * @param pathname - 현재 경로 (예: '/posts/123')
 * @param pattern - 패턴 (예: '/posts/[id]')
 * @returns 매칭 여부
 */
export function matchRoute(pathname: string, pattern: string): boolean {
  // [id], [slug] 같은 동적 세그먼트를 정규식으로 변환
  // '/posts/[id]' → '^/posts/[^/]+$'
  const regex = new RegExp('^' + pattern.replace(/\[.*?\]/g, '[^/]+') + '$');
  return regex.test(pathname);
}

/**
 * 보호된 경로 여부 확인
 *
 * @param pathname - 현재 경로
 * @returns 인증이 필요한 경로면 true
 */
export function isProtectedRoute(pathname: string): boolean {
  return AUTH_CONFIG.protected.some((pattern) => matchRoute(pathname, pattern));
}

/**
 * 공개 경로 여부 확인
 *
 * @param pathname - 현재 경로
 * @returns 공개 경로면 true
 */
export function isPublicRoute(pathname: string): boolean {
  return AUTH_CONFIG.public.some((pattern) => matchRoute(pathname, pattern));
}
