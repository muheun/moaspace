'use client';

import { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Skeleton } from '@/components/ui/skeleton';
import { Alert } from '@/components/ui/alert';

/**
 * OAuth 콜백 핸들러 페이지
 *
 * Google OAuth 인증 성공 후 Backend OAuth2SuccessHandler가 리다이렉트하는 페이지
 *
 * 처리 과정:
 * 1. URL 쿼리 파라미터에서 JWT 토큰 추출 (?token=xxx)
 * 2. localStorage에 access_token 저장
 * 3. 홈 페이지(/)로 리다이렉트
 *
 * Constitution Principle X: Skeleton UI로 로딩 상태 표시
 */
export default function CallbackPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const token = searchParams.get('token');

    if (!token) {
      setError('인증 토큰을 찾을 수 없습니다. 다시 로그인해주세요.');
      setTimeout(() => {
        router.push('/login');
      }, 2000);
      return;
    }

    // JWT 토큰 저장
    localStorage.setItem('access_token', token);

    // 홈 페이지로 리다이렉트
    router.push('/');
  }, [searchParams, router]);

  if (error) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-gray-50">
        <Alert variant="destructive" className="max-w-md">
          <p>{error}</p>
        </Alert>
      </main>
    );
  }

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
