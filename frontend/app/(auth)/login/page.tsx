'use client';

import { Button } from '@/components/ui/button';
import { useRouter } from 'next/navigation';

/**
 * 로그인 페이지
 *
 * Google OAuth 로그인 버튼을 제공하여 사용자 인증을 시작합니다.
 *
 * Constitution Principle X:
 * - Semantic HTML: <main>, <section>, <h1> 태그 사용
 * - ARIA: aria-label로 버튼 설명 추가
 */
export default function LoginPage() {
  const router = useRouter();

  /**
   * Google OAuth 로그인 시작
   *
   * Backend의 /oauth2/authorization/google 엔드포인트로 리다이렉트
   * Spring Security OAuth2 클라이언트가 Google 로그인 화면으로 리다이렉트
   */
  const handleGoogleLogin = () => {
    const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
    window.location.href = `${apiUrl}/oauth2/authorization/google`;
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-gray-50">
      <section className="w-full max-w-md p-8 bg-white rounded-lg shadow-md">
        <h1 className="text-3xl font-bold text-center mb-8">
          게시판 로그인
        </h1>

        <div className="space-y-4">
          <p className="text-center text-gray-600 mb-6">
            Google 계정으로 로그인하여 게시판을 이용하세요
          </p>

          <Button
            onClick={handleGoogleLogin}
            className="w-full flex items-center justify-center gap-3"
            size="lg"
            aria-label="Google 계정으로 로그인"
          >
            <svg
              className="w-5 h-5"
              viewBox="0 0 24 24"
              fill="currentColor"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
              <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
              <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
              <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
            </svg>
            Google로 로그인
          </Button>
        </div>

        <p className="mt-6 text-center text-sm text-gray-500">
          로그인 시 서비스 이용약관 및 개인정보처리방침에 동의하게 됩니다
        </p>
      </section>
    </main>
  );
}
