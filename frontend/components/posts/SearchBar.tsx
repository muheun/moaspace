/**
 * SearchBar 컴포넌트
 * T071: 키워드 입력 및 벡터 검색 토글
 * T074: 임계값 슬라이더 추가
 *
 * Constitution Principle X: semantic HTML, ARIA, accessible form controls
 * Constitution Principle VI: shadcn/ui 기반 컴포넌트, 단일 책임
 */

'use client';

import { useState } from 'react';
import { Search } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { Slider } from '@/components/ui/slider';

interface SearchBarProps {
  onSearch: (query: string, vectorSearch: boolean, threshold: number) => void;
  isLoading?: boolean;
}

/**
 * 게시글 검색 바 컴포넌트
 *
 * @param onSearch 검색 실행 콜백 (query, vectorSearch, threshold)
 * @param isLoading 검색 중 상태
 */
export function SearchBar({ onSearch, isLoading = false }: SearchBarProps) {
  const [query, setQuery] = useState('');
  const [vectorSearch, setVectorSearch] = useState(false);
  const [threshold, setThreshold] = useState(0.6);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (query.trim()) {
      onSearch(query.trim(), vectorSearch, threshold);
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="space-y-4 p-4 bg-card rounded-lg border"
      role="search"
      aria-label="게시글 검색"
    >
      {/* 검색 입력 필드 */}
      <div className="flex gap-2">
        <div className="flex-1">
          <Input
            type="search"
            placeholder="게시글 검색..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            disabled={isLoading}
            aria-label="검색 키워드 입력"
            className="w-full"
          />
        </div>
        <Button type="submit" disabled={isLoading || !query.trim()}>
          <Search className="w-4 h-4 mr-2" aria-hidden="true" />
          검색
        </Button>
      </div>

      {/* 벡터 검색 토글 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <Switch
            id="vector-search"
            checked={vectorSearch}
            onCheckedChange={setVectorSearch}
            disabled={isLoading}
            aria-label="벡터 검색 활성화"
          />
          <Label
            htmlFor="vector-search"
            className="text-sm font-medium cursor-pointer"
          >
            벡터 검색 (의미적 유사도)
          </Label>
        </div>
      </div>

      {/* 임계값 슬라이더 (벡터 검색 활성화 시에만 표시) */}
      {vectorSearch && (
        <div className="space-y-2 pt-2 border-t">
          <div className="flex items-center justify-between">
            <Label htmlFor="threshold-slider" className="text-sm">
              유사도 임계값
            </Label>
            <span
              className="text-sm font-mono text-muted-foreground"
              aria-live="polite"
            >
              {threshold.toFixed(2)}
            </span>
          </div>
          <Slider
            id="threshold-slider"
            min={0}
            max={1}
            step={0.05}
            value={[threshold]}
            onValueChange={([value]) => setThreshold(value)}
            disabled={isLoading}
            aria-label="유사도 임계값 설정"
            aria-valuemin={0}
            aria-valuemax={1}
            aria-valuenow={threshold}
            aria-valuetext={`${(threshold * 100).toFixed(0)}%`}
          />
          <p className="text-xs text-muted-foreground">
            임계값이 높을수록 더 유사한 게시글만 표시됩니다. (0.0 ~ 1.0)
          </p>
        </div>
      )}
    </form>
  );
}
