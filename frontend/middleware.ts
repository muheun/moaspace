import { NextRequest, NextResponse } from 'next/server';
import { isProtectedRoute, isPublicRoute } from '@/lib/constants/routes';

/**
 * Next.js Middleware - 인증 기반 경로 보호
 *
 * 역할:
 * 1. routes.ts의 protected 경로에 인증 없이 접근 시 /login으로 리다이렉트
 * 2. 로그인된 사용자가 /login 접근 시 /posts로 리다이렉트
 *
 * JWT 토큰은 Cookie에서 확인 (localStorage는 서버에서 접근 불가)
 * routes.ts에서 경로 정의 변경 시 자동 반영됨
 */

export function middleware(request: NextRequest) {
  const path = request.nextUrl.pathname;
  const token = request.cookies.get('access_token')?.value;

  // 보호된 경로에 인증 없이 접근 시 /login으로 리다이렉트
  if (isProtectedRoute(path) && !token) {
    return NextResponse.redirect(new URL('/login', request.url));
  }

  // 로그인된 사용자가 /login 접근 시 /posts로 리다이렉트
  if (isPublicRoute(path) && token && path === '/login') {
    return NextResponse.redirect(new URL('/posts', request.url));
  }

  return NextResponse.next();
}

// Middleware가 실행될 경로 지정
export const config = {
  matcher: [
    /*
     * 다음을 제외한 모든 경로에서 실행:
     * - api (API routes)
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     * - 이미지 파일 (png, jpg, jpeg, gif, svg, webp)
     */
    '/((?!api|_next/static|_next/image|favicon.ico|.*\\.(?:png|jpg|jpeg|gif|svg|webp)$).*)',
  ],
};
