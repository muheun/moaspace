'use client';

import Link from 'next/link';
import { Button } from '@/components/ui/button';

/**
 * Header 컴포넌트 (로그인/로그아웃 버튼, 사용자 정보 표시)
 *
 * Constitution Principle X: semantic HTML, ARIA 속성 사용
 * Constitution Principle VI: shadcn/ui 컴포넌트 활용
 *
 * User Story 1에서 로그인 상태 관리 로직 추가 예정
 */
export function Header() {
  // TODO: User Story 1에서 useAuth 훅으로 로그인 상태 확인
  const isLoggedIn = false; // 임시 (User Story 1에서 구현)

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

          {isLoggedIn ? (
            <>
              {/* User Story 1에서 구현: 사용자 이름, 프로필 이미지 */}
              <span className="text-sm text-gray-600">사용자</span>
              <Button variant="outline" size="sm">
                로그아웃
              </Button>
            </>
          ) : (
            <Button asChild size="sm">
              <Link href="/login">로그인</Link>
            </Button>
          )}
        </nav>
      </div>
    </header>
  );
}
