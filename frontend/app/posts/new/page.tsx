'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import dynamic from 'next/dynamic';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorBoundary } from '@/components/ErrorBoundary';
import { useCreatePost } from '@/lib/hooks/usePosts';

/**
 * T094: Tiptap 에디터 지연 로딩 최적화
 * next/dynamic을 사용하여 에디터를 사용할 때만 로드
 */
const RichTextEditor = dynamic(
  () => import('@/components/RichTextEditor'),
  {
    loading: () => <Skeleton className="h-64 w-full" />,
    ssr: false,
  }
);

/**
 * 게시글 작성 페이지
 * T090: Error Boundary 적용
 *
 * Constitution Principle VI: shadcn/ui 기반 컴포넌트 우선 아키텍처
 * Constitution Principle VIII: content (HTML) + plainContent (Plain Text) 분리 저장
 * Constitution Principle X: Semantic HTML, ARIA, Error Boundary
 */
export default function NewPostPage() {
  const router = useRouter();
  const { mutate: createPost, isPending, error } = useCreatePost();

  const [title, setTitle] = useState('');
  const [contentHtml, setContentHtml] = useState('');
  const [hashtags, setHashtags] = useState('');

  const handleEditorChange = (html: string) => {
    setContentHtml(html);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!title.trim()) {
      toast.error('제목을 입력하세요');
      return;
    }

    if (!contentHtml.trim()) {
      toast.error('내용을 입력하세요');
      return;
    }

    const hashtagArray = hashtags
      .split(/[\s,]+/)
      .filter((tag) => tag.trim().length > 0)
      .slice(0, 10);

    createPost(
      {
        title: title.trim(),
        contentHtml: contentHtml,
        hashtags: hashtagArray,
      },
      {
        onSuccess: (data) => {
          toast.success('게시글이 작성되었습니다!');
          router.push(`/posts/${data.id}`);
        },
        onError: (err) => {
          console.error('게시글 작성 실패:', err);
          toast.error(err instanceof Error ? err.message : '게시글 작성에 실패했습니다.');
        },
      }
    );
  };

  return (
    <ErrorBoundary>
      <main className="container mx-auto px-4 py-8 max-w-6xl">
        <h1 className="text-3xl font-bold mb-8">새 게시글 작성</h1>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label
              htmlFor="title"
              className="block text-sm font-medium mb-2"
              aria-label="게시글 제목"
            >
              제목 <span className="text-red-500">*</span>
            </label>
            <Input
              id="title"
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="게시글 제목을 입력하세요 (최대 200자)"
              maxLength={200}
              required
              aria-required="true"
              disabled={isPending}
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-2" aria-label="게시글 내용">
              내용 <span className="text-red-500">*</span>
            </label>
            <RichTextEditor
              content=""
              onChange={handleEditorChange}
              placeholder="게시글 내용을 작성하세요. 마크다운 문법을 지원합니다."
            />
          </div>

          <div>
            <label
              htmlFor="hashtags"
              className="block text-sm font-medium mb-2"
              aria-label="해시태그"
            >
              해시태그 (선택)
            </label>
            <Input
              id="hashtags"
              type="text"
              value={hashtags}
              onChange={(e) => setHashtags(e.target.value)}
              placeholder="태그를 입력하세요 (공백 또는 쉼표로 구분, 최대 10개)"
              disabled={isPending}
            />
            <p className="text-sm text-gray-500 mt-1">
              예: Next.js React TypeScript 또는 Next.js, React, TypeScript
            </p>
          </div>

          {error && (
            <div
              className="p-4 bg-red-50 border border-red-200 rounded-md"
              role="alert"
              aria-live="polite"
            >
              <p className="text-red-800 text-sm">
                게시글 작성에 실패했습니다. 다시 시도해주세요.
              </p>
            </div>
          )}

          <div className="flex gap-3 justify-end">
            <Button
              type="button"
              variant="outline"
              onClick={() => router.back()}
              disabled={isPending}
              aria-label="취소"
            >
              취소
            </Button>
            <Button
              type="submit"
              disabled={isPending || !title.trim() || !contentHtml.trim()}
              aria-label="게시글 작성"
            >
              {isPending ? '작성 중...' : '작성하기'}
            </Button>
          </div>
        </form>
      </main>
    </ErrorBoundary>
  );
}
