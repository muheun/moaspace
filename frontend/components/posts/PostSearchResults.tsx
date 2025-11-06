'use client';

import { PostSearchResultItem as SearchResultItem } from './PostSearchResultItem';
import type { PostSearchResultItem } from '@/types/api/post';

interface PostSearchResultsProps {
  results: PostSearchResultItem[];
  totalResults: number;
}

export function PostSearchResults({ results, totalResults }: PostSearchResultsProps) {
  if (totalResults === 0) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">검색 결과가 없습니다.</p>
        <p className="text-sm text-muted-foreground mt-2">
          다른 검색어나 필터를 시도해보세요.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">
          검색 결과 ({totalResults}건)
        </h2>
      </div>

      <div className="space-y-3">
        {results.map((item) => (
          <SearchResultItem key={item.post.id} item={item} />
        ))}
      </div>
    </div>
  );
}
