/**
 * PostList 컴포넌트
 * T070: 게시글 목록 렌더링
 * T099: 게시글 상세 페이지 프리페칭
 *
 * Constitution Principle X: semantic HTML, ARIA, Skeleton UI 구현
 * Constitution Principle VI: shadcn/ui 기반 컴포넌트, 단일 책임
 */

'use client';

import Link from 'next/link';
import { useQueryClient } from '@tanstack/react-query';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import type { PostSummary } from '@/types/api/post';
import { getPostById } from '@/lib/api/posts';

interface PostListProps {
  posts: PostSummary[];
  isLoading?: boolean;
}

/**
 * 게시글 목록 컴포넌트
 *
 * @param posts 게시글 요약 배열
 * @param isLoading 로딩 상태
 */
export function PostList({ posts, isLoading = false }: PostListProps) {
  const queryClient = useQueryClient();

  /**
   * T099: 게시글 상세 페이지 프리페칭
   * 마우스 오버 시 해당 게시글 데이터를 미리 로드
   */
  const handlePrefetch = (postId: number) => {
    queryClient.prefetchQuery({
      queryKey: ['post', postId],
      queryFn: () => getPostById(postId),
      staleTime: 5 * 60 * 1000, // 5분간 캐시 유지
    });
  };

  if (isLoading) {
    return (
      <div className="space-y-4" role="status" aria-label="게시글 로딩 중">
        {Array.from({ length: 5 }).map((_, index) => (
          <Card key={index} className="hover:shadow-md transition-shadow">
            <CardHeader>
              <Skeleton className="h-6 w-3/4" />
              <Skeleton className="h-4 w-1/4 mt-2" />
            </CardHeader>
            <CardContent>
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-5/6 mt-2" />
              <div className="flex gap-2 mt-4">
                <Skeleton className="h-6 w-16" />
                <Skeleton className="h-6 w-16" />
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  if (posts.length === 0) {
    return (
      <div
        className="text-center py-12 text-muted-foreground"
        role="status"
        aria-label="게시글 없음"
      >
        <p className="text-lg">게시글이 없습니다.</p>
        <p className="text-sm mt-2">새로운 게시글을 작성해보세요!</p>
      </div>
    );
  }

  return (
    <div className="space-y-4" role="list" aria-label="게시글 목록">
      {posts.map((post) => (
        <article key={post.id} role="listitem">
          <Link
            href={`/posts/${post.id}`}
            className="block"
            onMouseEnter={() => handlePrefetch(post.id)}
            onFocus={() => handlePrefetch(post.id)}
          >
            <Card className="hover:shadow-md transition-shadow cursor-pointer">
              <CardHeader>
                <CardTitle className="text-xl">{post.title}</CardTitle>
                <CardDescription className="flex items-center gap-2">
                  <span>{post.author.name}</span>
                  <span>•</span>
                  <time dateTime={post.createdAt}>
                    {new Date(post.createdAt).toLocaleDateString('ko-KR', {
                      year: 'numeric',
                      month: 'long',
                      day: 'numeric',
                    })}
                  </time>
                </CardDescription>
              </CardHeader>
              <CardContent>
                <p className="text-muted-foreground line-clamp-2">{post.excerpt}</p>
                {post.hashtags.length > 0 && (
                  <div className="flex gap-2 mt-4 flex-wrap" aria-label="해시태그">
                    {post.hashtags.map((hashtag) => (
                      <Badge key={hashtag} variant="secondary">
                        #{hashtag}
                      </Badge>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </Link>
        </article>
      ))}
    </div>
  );
}
