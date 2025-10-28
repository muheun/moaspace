'use client';

import { use } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { usePost } from '@/lib/hooks/usePosts';
import { useAuth } from '@/lib/hooks/useAuth';
import { format } from 'date-fns';

/**
 * 게시글 상세 페이지
 *
 * Constitution Principle VI: shadcn/ui 기반 컴포넌트 우선 아키텍처
 * Constitution Principle X: Semantic HTML, ARIA, Skeleton UI
 */
export default function PostDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const router = useRouter();
  const postId = parseInt(id, 10);

  const { data: post, isLoading, error } = usePost(postId);
  const { data: user } = useAuth();

  const isAuthor = user && post && user.id === post.author.id;

  /**
   * 게시글 수정 페이지로 이동
   */
  const handleEdit = () => {
    router.push(`/posts/${postId}/edit`);
  };

  /**
   * 로딩 상태 (Skeleton UI)
   */
  if (isLoading) {
    return (
      <main className="container mx-auto px-4 py-8 max-w-4xl">
        <div className="animate-pulse">
          <div className="h-10 bg-gray-200 rounded w-3/4 mb-4"></div>
          <div className="flex items-center gap-4 mb-6">
            <div className="w-12 h-12 bg-gray-200 rounded-full"></div>
            <div className="space-y-2">
              <div className="h-4 bg-gray-200 rounded w-24"></div>
              <div className="h-3 bg-gray-200 rounded w-32"></div>
            </div>
          </div>
          <div className="space-y-3">
            <div className="h-4 bg-gray-200 rounded"></div>
            <div className="h-4 bg-gray-200 rounded"></div>
            <div className="h-4 bg-gray-200 rounded w-5/6"></div>
          </div>
        </div>
      </main>
    );
  }

  /**
   * 에러 상태
   */
  if (error || !post) {
    return (
      <main className="container mx-auto px-4 py-8 max-w-4xl">
        <section className="text-center py-16">
          <h1 className="text-2xl font-bold text-red-600 mb-4">
            게시글을 찾을 수 없습니다
          </h1>
          <p className="text-gray-600 mb-6">
            요청하신 게시글이 존재하지 않거나 삭제되었습니다.
          </p>
          <Button onClick={() => router.push('/')}>홈으로 돌아가기</Button>
        </section>
      </main>
    );
  }

  return (
    <main className="container mx-auto px-4 py-8 max-w-4xl">
      <article>
        <header className="mb-8">
          <h1 className="text-4xl font-bold mb-4">{post.title}</h1>

          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-4">
              {post.author.profileImageUrl && (
                <img
                  src={post.author.profileImageUrl}
                  alt={`${post.author.name} 프로필`}
                  className="w-12 h-12 rounded-full"
                />
              )}
              <div>
                <p className="font-semibold">{post.author.name}</p>
                <p className="text-sm text-gray-500">
                  {format(new Date(post.createdAt), 'yyyy년 MM월 dd일 HH:mm')}
                  {post.createdAt !== post.updatedAt && ' (수정됨)'}
                </p>
              </div>
            </div>

            {isAuthor && (
              <Button
                onClick={handleEdit}
                variant="outline"
                aria-label="게시글 수정"
              >
                수정하기
              </Button>
            )}
          </div>

          {post.hashtags.length > 0 && (
            <div className="flex flex-wrap gap-2" role="list" aria-label="해시태그 목록">
              {post.hashtags.map((tag, index) => (
                <span
                  key={index}
                  className="px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-sm"
                  role="listitem"
                >
                  #{tag}
                </span>
              ))}
            </div>
          )}
        </header>

        <section
          className="prose prose-lg max-w-none"
          dangerouslySetInnerHTML={{ __html: post.content }}
          aria-label="게시글 내용"
        />
      </article>

      <footer className="mt-12 pt-6 border-t">
        <Button variant="outline" onClick={() => router.back()} aria-label="뒤로 가기">
          목록으로
        </Button>
      </footer>
    </main>
  );
}
