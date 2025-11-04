'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { CheckCircle, XCircle } from 'lucide-react';

/**
 * 인증 정보 완전 삭제 페이지
 *
 * 목적:
 * - Cookie의 access_token 삭제
 * - localStorage의 access_token 삭제
 * - 테스트 및 디버깅용
 */
export default function ClearAuthPage() {
  const router = useRouter();
  const [cleared, setCleared] = useState(false);
  const [details, setDetails] = useState<string[]>([]);

  useEffect(() => {
    const clearAll = async () => {
      const results: string[] = [];

      // 1. localStorage 확인 및 삭제
      try {
        const localToken = localStorage.getItem('access_token');
        if (localToken) {
          localStorage.removeItem('access_token');
          results.push('✅ localStorage의 access_token 삭제됨');
        } else {
          results.push('⚪ localStorage에 access_token 없음');
        }
      } catch (error) {
        results.push('❌ localStorage 삭제 실패: ' + error);
      }

      // 2. Cookie 삭제 (서버 액션 호출)
      try {
        const response = await fetch('/api/auth/clear-cookie', {
          method: 'POST',
        });
        if (response.ok) {
          results.push('✅ Cookie의 access_token 삭제됨');
        } else {
          results.push('❌ Cookie 삭제 실패');
        }
      } catch (error) {
        // API가 없어도 클라이언트에서 직접 삭제
        document.cookie = 'access_token=; Max-Age=0; path=/;';
        results.push('✅ Cookie 직접 삭제 시도');
      }

      // 3. 모든 쿠키 확인
      const allCookies = document.cookie;
      if (allCookies.includes('access_token')) {
        results.push('⚠️ 경고: Cookie에 아직 access_token이 남아있을 수 있음');
      }

      setDetails(results);
      setCleared(true);
    };

    clearAll();
  }, []);

  return (
    <div className="container mx-auto px-4 py-8 max-w-2xl">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            {cleared ? (
              <>
                <CheckCircle className="w-6 h-6 text-green-500" />
                인증 정보 삭제 완료
              </>
            ) : (
              <>
                <XCircle className="w-6 h-6 text-yellow-500" />
                인증 정보 삭제 중...
              </>
            )}
          </CardTitle>
          <CardDescription>
            모든 인증 토큰이 삭제되었습니다. 이제 보호된 페이지에 접근할 수 없습니다.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="bg-muted p-4 rounded-md">
            <h3 className="font-semibold mb-2">삭제 내역:</h3>
            <ul className="space-y-1 text-sm">
              {details.map((detail, idx) => (
                <li key={idx} className="font-mono">{detail}</li>
              ))}
            </ul>
          </div>

          <div className="space-y-2">
            <Button
              onClick={() => router.push('/posts')}
              className="w-full"
            >
              게시판 접근 테스트 (리다이렉트되어야 함)
            </Button>
            <Button
              onClick={() => router.push('/')}
              variant="outline"
              className="w-full"
            >
              메인 페이지로 이동
            </Button>
          </div>

          <div className="text-xs text-muted-foreground bg-yellow-50 dark:bg-yellow-900/20 p-3 rounded-md">
            <p className="font-semibold mb-1">⚠️ 참고:</p>
            <ul className="list-disc list-inside space-y-1">
              <li>브라우저를 완전히 닫고 다시 열면 더 확실합니다</li>
              <li>개발자 도구(F12) → Application → Storage에서 직접 확인 가능</li>
              <li>이 페이지는 테스트/디버깅 전용입니다</li>
            </ul>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
