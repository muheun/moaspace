'use client';

import { use, useState } from 'react';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorBoundary } from '@/components/ErrorBoundary';
import { usePost, useDeletePost } from '@/lib/hooks/usePosts';
import { useAuth } from '@/lib/hooks/useAuth';
import { DeleteConfirmDialog } from '@/components/posts/DeleteConfirmDialog';
import { format } from 'date-fns';

/**
 * 게시글 상세 페이지
 * T090: Error Boundary 적용
 *
 * Constitution Principle VI: shadcn/ui 기반 컴포넌트 우선 아키텍처
 * Constitution Principle X: Semantic HTML, ARIA, Skeleton UI, Error Boundary
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
  const deletePostMutation = useDeletePost();

  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);

  const isAuthor = user && post && user.id === post.author.id;

  /**
   * 게시글 수정 페이지로 이동
   */
  const handleEdit = () => {
    router.push(`/posts/${postId}/edit`);
  };

  /**
   * 게시글 삭제 확인 다이얼로그 열기
   */
  const handleDeleteClick = () => {
    setDeleteDialogOpen(true);
  };

  /**
   * 게시글 삭제 확인 (T086-T087)
   * Optimistic update로 삭제 후 목록으로 리다이렉트
   */
  const handleDeleteConfirm = () => {
    deletePostMutation.mutate(postId, {
      onSuccess: () => {
        // T087: 삭제 성공 후 게시글 목록으로 리다이렉트
        toast.success('게시글이 삭제되었습니다');
        router.push('/posts');
      },
      onError: (error: any) => {
        // 에러 처리 (권한 없음, 네트워크 오류 등)
        toast.error(error instanceof Error ? error.message : '게시글 삭제에 실패했습니다');
        setDeleteDialogOpen(false);
      },
    });
  };

  /**
   * 로딩 상태 (Skeleton UI)
   * T089: Skeleton 컴포넌트로 로딩 상태 개선
   */
  if (isLoading) {
    return (
      <main className="container mx-auto px-4 py-8 max-w-4xl" role="status" aria-label="게시글 로딩 중">
        <article>
          <header className="mb-8">
            <Skeleton className="h-10 w-3/4 mb-4" />
            <div className="flex items-center gap-4 mb-4">
              <Skeleton className="w-12 h-12 rounded-full" />
              <div className="space-y-2">
                <Skeleton className="h-4 w-24" />
                <Skeleton className="h-3 w-32" />
              </div>
            </div>
            <div className="flex gap-2">
              <Skeleton className="h-6 w-16" />
              <Skeleton className="h-6 w-16" />
            </div>
          </header>
          <section className="space-y-3">
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-5/6" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-4/5" />
          </section>
        </article>
      </main>
    );
  }

  /**
   * 에러 상태 (T088)
   * 404 또는 삭제된 게시글 접근 시 메시지 표시
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
          <Button onClick={() => router.push('/posts')}>목록으로 돌아가기</Button>
        </section>
      </main>
    );
  }

  return (
    <ErrorBoundary>
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

              {/* T084: 작성자에게만 수정/삭제 버튼 표시 */}
              {isAuthor && (
                <div className="flex gap-2">
                  <Button
                    onClick={handleEdit}
                    variant="outline"
                    aria-label="게시글 수정"
                  >
                    수정하기
                  </Button>
                  <Button
                    onClick={handleDeleteClick}
                    variant="outline"
                    className="text-red-600 border-red-600 hover:bg-red-50"
                    aria-label="게시글 삭제"
                  >
                    삭제하기
                  </Button>
                </div>
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

        {/* T085: 삭제 확인 다이얼로그 */}
        <DeleteConfirmDialog
          open={deleteDialogOpen}
          onOpenChange={setDeleteDialogOpen}
          onConfirm={handleDeleteConfirm}
          isDeleting={deletePostMutation.isPending}
          postTitle={post.title}
        />
      </main>
    </ErrorBoundary>
  );
}
