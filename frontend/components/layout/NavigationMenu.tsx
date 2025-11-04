'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Home, FileText } from 'lucide-react';

/**
 * NavigationMenu 컴포넌트
 *
 * Constitution Principle VII: Props Interface 명시, shadcn/ui 활용
 * Constitution Principle XI: Keyboard navigation support
 * Web UI Design Guide: 16px minimum text, 8px grid spacing, neutral colors
 *
 * 기능:
 * - 키보드 네비게이션 지원 (Tab 키로 이동)
 * - 현재 경로 강조 표시
 * - 아이콘 + 텍스트 조합
 */

export interface NavigationItem {
  label: string;
  href: string;
  icon?: React.ReactNode;
}

export interface NavigationMenuProps {
  items: NavigationItem[];
  className?: string;
}

export function NavigationMenu({ items, className = '' }: NavigationMenuProps) {
  const pathname = usePathname();

  return (
    <nav
      className={`flex items-center gap-4 ${className}`}
      role="navigation"
      aria-label="Site navigation"
    >
      {items.map((item) => {
        const isActive = pathname === item.href;

        return (
          <Link
            key={item.href}
            href={item.href}
            className={`
              flex items-center gap-2 text-base font-medium
              transition-colors duration-200
              focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2
              rounded-md px-3 py-2
              ${
                isActive
                  ? 'text-primary bg-primary/10'
                  : 'text-foreground hover:text-primary hover:bg-accent/50'
              }
            `}
            aria-current={isActive ? 'page' : undefined}
          >
            {item.icon && <span aria-hidden="true">{item.icon}</span>}
            <span>{item.label}</span>
          </Link>
        );
      })}
    </nav>
  );
}

/**
 * 기본 네비게이션 아이템 (홈, 게시판)
 *
 * 참고: 검색 기능은 게시판 페이지에 통합되어 있음
 */
export const defaultNavigationItems: NavigationItem[] = [
  {
    label: '홈',
    href: '/',
    icon: <Home size={20} />,
  },
  {
    label: '게시판',
    href: '/posts',
    icon: <FileText size={20} />,
  },
];
