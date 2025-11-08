'use client';

import { use, useState } from 'react';
import { useRouter } from 'next/navigation';
import Image from 'next/image';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorBoundary } from '@/components/ErrorBoundary';
import { usePost, useDeletePost } from '@/lib/hooks/usePosts';
import { useAuth } from '@/lib/hooks/useAuth';
import { DeleteConfirmDialog } from '@/components/posts/DeleteConfirmDialog';
import { format } from 'date-fns';
import MarkdownViewer from '@/components/ui/MarkdownViewer';

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

  const handleEdit = () => {
    router.push(`/posts/${postId}/edit`);
  };

  const handleDeleteClick = () => {
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = () => {
    deletePostMutation.mutate(postId, {
      onSuccess: () => {
        toast.success('게시글이 삭제되었습니다');
        router.push('/posts');
      },
      onError: (error) => {
        toast.error(error instanceof Error ? error.message : '게시글 삭제에 실패했습니다');
        setDeleteDialogOpen(false);
      },
    });
  };

  if (isLoading) {
    return (
      <main className="container mx-auto px-4 py-8 max-w-6xl" role="status" aria-label="게시글 로딩 중">
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

  if (error || !post) {
    return (
      <main className="container mx-auto px-4 py-8 max-w-6xl">
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
      <main className="container mx-auto px-4 py-8 max-w-6xl">
        <article>
          <header className="mb-8">
            <h1 className="text-4xl font-bold mb-4">{post.title}</h1>

            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-4">
                {post.author.profileImageUrl && (
                  <Image
                    src={post.author.profileImageUrl}
                    alt={`${post.author.name} 프로필`}
                    width={48}
                    height={48}
                    className="rounded-full"
                  />
                )}
                <div>
                  <p className="font-semibold">{post.author.name}</p>
                  <p className="text-sm text-gray-500">
                    {format(new Date(post.createdAt), 'yyyy년 MM월 dd일 HH:mm')}
                    {post.updatedAt && ' (수정됨)'}
                  </p>
                </div>
              </div>

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

          <section className="mt-8">
            {post.contentMarkdown ? (
              <MarkdownViewer content={post.contentMarkdown} />
            ) : post.contentHtml ? (
              <div className="prose prose-slate dark:prose-invert max-w-none">
                <div dangerouslySetInnerHTML={{ __html: post.contentHtml }} />
              </div>
            ) : (
              <div className="whitespace-pre-wrap text-gray-500">
                (내용 없음)
              </div>
            )}
          </section>
        </article>

        <footer className="mt-12 pt-6 border-t">
          <Button variant="outline" onClick={() => router.back()} aria-label="뒤로 가기">
            목록으로
          </Button>
        </footer>

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
