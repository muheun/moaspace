'use client';

import { useState } from 'react';
import { usePostSearch } from '@/lib/hooks/usePosts';
import { PostSearchFilter } from '@/components/posts/PostSearchFilter';
import { PostSearchResults } from '@/components/posts/PostSearchResults';
import { PostSearchSkeleton } from '@/components/posts/PostSearchSkeleton';
import { PostSearchErrorBoundary } from '@/components/posts/PostSearchErrorBoundary';
import type { PostSearchField } from '@/types/api/post';

export default function PostSearchPage() {
  const [searchParams, setSearchParams] = useState<{
    query: string;
    fields?: PostSearchField[];
  } | null>(null);

  const { data, isLoading, error, refetch } = usePostSearch(
    {
      query: searchParams?.query || '',
      fields: searchParams?.fields,
      limit: 20,
    },
    !!searchParams?.query
  );

  const handleSearch = (query: string, fields?: PostSearchField[]) => {
    setSearchParams({ query, fields });
  };

  return (
    <div className="container mx-auto px-4 py-8 max-w-6xl">
      <div className="space-y-8">
        {/* 페이지 헤더 */}
        <div>
          <h1 className="text-3xl font-bold mb-2">게시글 검색</h1>
          <p className="text-muted-foreground">
            필드별 가중치 기반 벡터 검색으로 관련성 높은 게시글을 찾아보세요.
          </p>
        </div>

        {/* 검색 필터 */}
        <PostSearchFilter onSearch={handleSearch} isLoading={isLoading} />

        {/* 검색 결과 */}
        <div>
          {isLoading && <PostSearchSkeleton />}

          {error && (
            <PostSearchErrorBoundary
              error={error}
              onRetry={() => refetch()}
            />
          )}

          {data && !isLoading && !error && (
            <PostSearchResults
              results={data.results}
              totalResults={data.totalResults}
            />
          )}

          {!searchParams && !isLoading && (
            <div className="text-center py-12 text-muted-foreground">
              <p>검색어를 입력하고 검색 버튼을 눌러주세요.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
