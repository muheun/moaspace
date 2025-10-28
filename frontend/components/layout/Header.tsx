'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { useAuth, useLogout } from '@/lib/hooks/useAuth';

/**
 * Header 컴포넌트 (로그인/로그아웃 버튼, 사용자 정보 표시)
 *
 * Constitution Principle X: semantic HTML, ARIA 속성 사용
 * Constitution Principle VI: shadcn/ui 컴포넌트 활용
 * Constitution Principle VII: TanStack Query (서버 상태) 사용
 *
 * 기능:
 * - 로그인 상태에 따라 사용자 정보 또는 로그인 버튼 표시
 * - 로그아웃 버튼 클릭 시 로그아웃 처리 및 로그인 페이지로 리다이렉트
 */
export function Header() {
  const router = useRouter();
  const { data: user, isLoading } = useAuth();
  const { mutate: logout, isPending: isLoggingOut } = useLogout();

  /**
   * 로그아웃 처리
   *
   * 1. useLogout 훅으로 백엔드 /api/auth/logout 호출
   * 2. localStorage에서 access_token 제거
   * 3. React Query 캐시 무효화
   * 4. 로그인 페이지로 리다이렉트
   */
  const handleLogout = () => {
    logout(undefined, {
      onSuccess: () => {
        router.push('/login');
      },
    });
  };

  return (
    <header className="border-b">
      <div className="container mx-auto px-4 py-4 flex justify-between items-center">
        <Link href="/" className="text-2xl font-bold">
          Vector AI Board
        </Link>

        <nav className="flex items-center gap-4">
          <Link href="/posts" className="hover:underline">
            게시판
          </Link>

          {isLoading ? (
            // 로딩 중: Skeleton UI (Constitution Principle X)
            <Skeleton className="h-8 w-24" />
          ) : user ? (
            // 로그인 상태: 사용자 정보 + 로그아웃 버튼
            <>
              <div className="flex items-center gap-2">
                {user.profileImageUrl && (
                  <img
                    src={user.profileImageUrl}
                    alt={`${user.name}의 프로필 이미지`}
                    className="w-8 h-8 rounded-full"
                  />
                )}
                <span className="text-sm text-gray-700 font-medium">
                  {user.name}
                </span>
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={handleLogout}
                disabled={isLoggingOut}
                aria-label="로그아웃"
              >
                {isLoggingOut ? '로그아웃 중...' : '로그아웃'}
              </Button>
            </>
          ) : (
            // 비로그인 상태: 로그인 버튼
            <Button asChild size="sm">
              <Link href="/login">로그인</Link>
            </Button>
          )}
        </nav>
      </div>
    </header>
  );
}
