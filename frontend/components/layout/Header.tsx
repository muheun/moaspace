'use client';

import Link from 'next/link';
import { useRouter, usePathname } from 'next/navigation';
import { useState } from 'react';
import { Menu, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { useAuth, useLogout } from '@/lib/hooks/useAuth';
import { NavigationMenu, defaultNavigationItems } from './NavigationMenu';

/**
 * Header 컴포넌트 (로그인/로그아웃 버튼, 사용자 정보 표시, 모바일 메뉴)
 *
 * Constitution Principle X: semantic HTML, ARIA 속성 사용
 * Constitution Principle VI: shadcn/ui 컴포넌트 활용
 * Constitution Principle VII: TanStack Query (서버 상태) 사용
 * Web UI Design Guide: Mobile-first responsive design, hamburger menu < 768px
 *
 * 기능:
 * - 로그인 상태에 따라 사용자 정보 또는 로그인 버튼 표시
 * - 로그아웃 버튼 클릭 시 로그아웃 처리 및 로그인 페이지로 리다이렉트
 * - 모바일 환경에서 햄버거 메뉴 표시 (< 768px)
 * - 인증이 필요한 경로에서만 useAuth 훅 실행 (무한 API 호출 방지)
 */
export function Header() {
  const router = useRouter();
  const pathname = usePathname();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  // 공개 페이지 목록 (인증 체크 안 함)
  const publicPaths = ['/login', '/callback', '/test-editor'];
  const isPublicPage = publicPaths.includes(pathname);

  // 공개 페이지에서는 useAuth 호출 안 함 (401 에러 방지)
  const { data: user, isLoading } = useAuth(!isPublicPage);
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

        {/* Desktop Navigation (>= 768px) */}
        <div className="hidden md:flex items-center gap-4">
          <NavigationMenu items={defaultNavigationItems} />

          {isLoading ? (
            <Skeleton className="h-11 w-24" />
          ) : user ? (
            <>
              <div className="flex items-center gap-2">
                {user.profileImageUrl && (
                  <img
                    src={user.profileImageUrl}
                    alt={`${user.name}의 프로필 이미지`}
                    className="w-11 h-11 rounded-full min-w-11 min-h-11"
                  />
                )}
                <span className="text-base font-medium">
                  {user.name}
                </span>
              </div>
              <Button
                variant="outline"
                size="default"
                onClick={handleLogout}
                disabled={isLoggingOut}
                aria-label="로그아웃"
                className="min-h-11 min-w-20"
              >
                {isLoggingOut ? '로그아웃 중...' : '로그아웃'}
              </Button>
            </>
          ) : (
            <Button asChild size="default" className="min-h-11 min-w-20">
              <Link href="/login">로그인</Link>
            </Button>
          )}
        </div>

        {/* Mobile Hamburger Menu Button (< 768px) */}
        <Button
          variant="ghost"
          size="icon"
          className="md:hidden min-h-11 min-w-11"
          onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
          aria-label={isMobileMenuOpen ? '메뉴 닫기' : '메뉴 열기'}
          aria-expanded={isMobileMenuOpen}
        >
          {isMobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
        </Button>
      </div>

      {/* Mobile Menu (< 768px) */}
      {isMobileMenuOpen && (
        <div className="md:hidden border-t">
          <div className="container mx-auto px-4 py-4 flex flex-col gap-4">
            <NavigationMenu items={defaultNavigationItems} className="flex-col items-start" />

            <div className="border-t pt-4">
              {isLoading ? (
                <Skeleton className="h-11 w-24" />
              ) : user ? (
                <div className="flex flex-col gap-3">
                  <div className="flex items-center gap-2">
                    {user.profileImageUrl && (
                      <img
                        src={user.profileImageUrl}
                        alt={`${user.name}의 프로필 이미지`}
                        className="w-11 h-11 rounded-full min-w-11 min-h-11"
                      />
                    )}
                    <span className="text-base font-medium">
                      {user.name}
                    </span>
                  </div>
                  <Button
                    variant="outline"
                    size="default"
                    onClick={handleLogout}
                    disabled={isLoggingOut}
                    aria-label="로그아웃"
                    className="min-h-11 w-full"
                  >
                    {isLoggingOut ? '로그아웃 중...' : '로그아웃'}
                  </Button>
                </div>
              ) : (
                <Button asChild size="default" className="min-h-11 w-full">
                  <Link href="/login">로그인</Link>
                </Button>
              )}
            </div>
          </div>
        </div>
      )}
    </header>
  );
}
