'use client';

import { use, useEffect, useTransition } from 'react';
import { redirect, useRouter } from 'next/navigation';
import { Skeleton } from '@/components/ui/skeleton';
import { setAuthCookie } from './actions';

/**
 * OAuth 콜백 핸들러 페이지 (React 19 + React Compiler)
 *
 * Google OAuth 인증 성공/실패 후 Backend가 리다이렉트하는 페이지
 *
 * 성공 처리 과정:
 * 1. URL 쿼리 파라미터에서 JWT 토큰 추출 (?token=xxx)
 * 2. localStorage + Cookie에 access_token 저장 (Middleware 인증용)
 * 3. useTransition으로 Server Action 호출 (React 19)
 * 4. 홈 페이지(/)로 리다이렉트
 *
 * 실패 처리 과정:
 * 1. URL 쿼리 파라미터에서 에러 정보 추출 (?error=xxx&message=yyy)
 * 2. 사용자 친화적 에러 메시지 표시
 * 3. 3초 후 /login 페이지로 자동 리다이렉트
 *
 * React Compiler: 자동 메모이제이션, useMemo/useCallback 불필요
 * React 19: use() API for suspense-aware data fetching
 */
export default function CallbackPage({
  searchParams
}: {
  searchParams: Promise<{ token?: string; error?: string; message?: string }>
}) {
  const router = useRouter();
  // React 19: use() API로 searchParams 읽기
  const params = use(searchParams);
  const [, startTransition] = useTransition();

  useEffect(() => {
    const { token, error, message } = params;

    // 에러가 있으면 3초 후 /login으로 리다이렉트
    if (error) {
      const timer = setTimeout(() => {
        router.push('/login');
      }, 3000);
      return () => clearTimeout(timer);
    }

    // 토큰 없으면 /login으로 리다이렉트
    if (!token) {
      redirect('/login');
      return;
    }

    // React 19: useTransition으로 Server Action 호출
    startTransition(async () => {
      // JWT 토큰 저장 (localStorage + Cookie)
      localStorage.setItem('access_token', token);
      await setAuthCookie(token);

      // 홈 페이지로 리다이렉트
      router.push('/');
    });
  }, [params, router]);

  // 에러 메시지가 있으면 에러 화면 표시
  if (params.error) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-gray-50">
        <div className="w-full max-w-md p-8 bg-white rounded-lg shadow-md space-y-4">
          <div className="flex items-center justify-center w-16 h-16 mx-auto bg-red-100 rounded-full">
            <svg
              className="w-8 h-8 text-red-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-center text-gray-900">로그인 실패</h1>
          <p className="text-center text-gray-600">
            {params.message || '로그인 중 오류가 발생했습니다.'}
          </p>
          <p className="text-sm text-center text-gray-500">
            3초 후 로그인 페이지로 이동합니다...
          </p>
        </div>
      </main>
    );
  }

  // 정상 처리 중 화면
  return (
    <main className="flex min-h-screen items-center justify-center bg-gray-50">
      <div className="w-full max-w-md p-8 bg-white rounded-lg shadow-md space-y-4">
        <h1 className="text-2xl font-bold text-center">로그인 처리 중...</h1>
        <Skeleton className="h-12 w-full" />
        <Skeleton className="h-12 w-3/4" />
        <p className="text-center text-gray-600">잠시만 기다려주세요</p>
      </div>
    </main>
  );
}
