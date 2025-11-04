/**
 * 게시글 목록 페이지
 * T072: PostList + SearchBar 통합
 * T073: 페이지네이션 UI 구현
 * T075: 검색 결과에 유사도 점수 표시
 * T090: Error Boundary 적용
 * T095: 벡터 검색 코드 스플리팅
 *
 * Constitution Principle X: semantic HTML, Error Boundary 적용
 * Constitution Principle VII: TanStack Query (서버) + React 19 (클라이언트) 상태 분리
 */

'use client';

import { useState } from 'react';
import dynamic from 'next/dynamic';
import Link from 'next/link';
import { usePosts, useSearchPosts } from '@/lib/hooks/usePosts';
import { PostList } from '@/components/posts/PostList';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorBoundary } from '@/components/ErrorBoundary';
import { ChevronLeft, ChevronRight, AlertCircle, PenSquare } from 'lucide-react';

/**
 * T095: SearchBar 컴포넌트 지연 로딩
 * 벡터 검색 기능을 사용할 때만 로드
 */
const SearchBar = dynamic(
  () => import('@/components/posts/SearchBar').then(mod => ({ default: mod.SearchBar })),
  {
    loading: () => <Skeleton className="h-12 w-full" />,
    ssr: false,
  }
);

export default function PostsPage() {
  // 클라이언트 상태 (React 19)
  const [page, setPage] = useState(0);
  const [pageSize] = useState(20);
  const [hashtag, setHashtag] = useState<string | undefined>(undefined);
  const [searchMode, setSearchMode] = useState<'normal' | 'vector'>('normal');
  const [vectorQuery, setVectorQuery] = useState('');
  const [vectorThreshold, setVectorThreshold] = useState(0.6);

  // 서버 상태 (TanStack Query)
  const {
    data: postsData,
    isLoading: isLoadingPosts,
    error: postsError,
  } = usePosts(page, pageSize, hashtag);

  const {
    data: searchData,
    refetch: refetchSearch,
    isLoading: isLoadingSearch,
    error: searchError,
  } = useSearchPosts(
    { query: vectorQuery, threshold: vectorThreshold, limit: 20 },
    false // 수동 실행
  );

  const handleSearch = (query: string, vectorSearch: boolean, threshold: number) => {
    if (vectorSearch) {
      // 벡터 검색 모드
      setSearchMode('vector');
      setVectorQuery(query);
      setVectorThreshold(threshold);
      refetchSearch();
    } else {
      // 일반 검색 모드 (해시태그 필터링)
      setSearchMode('normal');
      setHashtag(query);
      setPage(0); // 첫 페이지로 리셋
    }
  };

  const handleResetSearch = () => {
    setSearchMode('normal');
    setHashtag(undefined);
    setVectorQuery('');
    setPage(0);
  };

  const isLoading = isLoadingPosts || isLoadingSearch;
  const error = postsError || searchError;

  // 에러 표시
  if (error) {
    return (
      <div className="container mx-auto px-4 py-8">
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>오류가 발생했습니다</AlertTitle>
          <AlertDescription>
            {error instanceof Error ? error.message : '게시글을 불러올 수 없습니다.'}
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  // 현재 표시할 게시글 데이터
  const currentPosts =
    searchMode === 'vector'
      ? searchData?.results.map((r) => r.post) || []
      : postsData?.posts || [];

  const pagination = postsData?.pagination;
  const vectorResults = searchData?.results || [];

  return (
    <ErrorBoundary>
      <div className="container mx-auto px-4 py-8">
        <header className="mb-8 flex items-start justify-between">
          <div>
            <h1 className="text-4xl font-bold mb-2">게시글 목록</h1>
            <p className="text-muted-foreground">
              {searchMode === 'vector'
                ? '벡터 검색 결과'
                : hashtag
                  ? `#${hashtag} 태그 게시글`
                  : '모든 게시글'}
            </p>
          </div>
          <Button asChild size="default" className="min-h-11">
            <Link href="/posts/new">
              <PenSquare className="w-4 h-4 mr-2" />
              새 게시글 작성
            </Link>
          </Button>
        </header>

        {/* 검색 바 */}
        <div className="mb-6">
          <SearchBar onSearch={handleSearch} isLoading={isLoading} />
        </div>

        {/* 검색 필터 표시 */}
        {(searchMode === 'vector' || hashtag) && (
          <div className="mb-4 flex items-center gap-2">
            <Badge variant="outline">
              {searchMode === 'vector'
                ? `벡터 검색: "${vectorQuery}" (임계값: ${vectorThreshold.toFixed(2)})`
                : `해시태그: #${hashtag}`}
            </Badge>
            <Button
              variant="ghost"
              size="sm"
              onClick={handleResetSearch}
              aria-label="검색 초기화"
            >
              초기화
            </Button>
          </div>
        )}

        {/* 게시글 목록 */}
        {searchMode === 'vector' ? (
          // 벡터 검색 결과 (유사도 점수 포함)
          <div className="space-y-4" role="list" aria-label="검색 결과">
            {isLoading ? (
              <PostList posts={[]} isLoading={true} />
            ) : vectorResults.length === 0 ? (
              <div className="text-center py-12 text-muted-foreground">
                <p className="text-lg">검색 결과가 없습니다.</p>
                <p className="text-sm mt-2">
                  다른 키워드로 검색하거나 임계값을 낮춰보세요.
                </p>
              </div>
            ) : (
              vectorResults.map((result) => (
                <div key={result.post.id} className="relative">
                  {/* 유사도 점수 표시 */}
                  <div className="absolute top-4 right-4 z-10">
                    <Badge variant="secondary" className="font-mono">
                      {(result.similarityScore * 100).toFixed(1)}%
                    </Badge>
                  </div>
                  <PostList posts={[result.post]} isLoading={false} />
                </div>
              ))
            )}
          </div>
        ) : (
          // 일반 목록 (페이지네이션)
          <>
            <PostList posts={currentPosts} isLoading={isLoading} />

            {/* 페이지네이션 */}
            {pagination && pagination.totalPages > 1 && (
              <nav
                className="mt-8 flex items-center justify-center gap-2"
                aria-label="페이지네이션"
              >
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(page - 1)}
                  disabled={page === 0 || isLoading}
                  aria-label="이전 페이지"
                >
                  <ChevronLeft className="w-4 h-4" />
                </Button>

                <span className="text-sm text-muted-foreground px-4">
                  {page + 1} / {pagination.totalPages}
                </span>

                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(page + 1)}
                  disabled={page >= pagination.totalPages - 1 || isLoading}
                  aria-label="다음 페이지"
                >
                  <ChevronRight className="w-4 h-4" />
                </Button>
              </nav>
            )}
          </>
        )}
      </div>
    </ErrorBoundary>
  );
}
