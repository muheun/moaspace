'use client';

import { use, useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import dynamic from 'next/dynamic';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorBoundary } from '@/components/ErrorBoundary';
import { usePost, useUpdatePost } from '@/lib/hooks/usePosts';
import { useAuth } from '@/lib/hooks/useAuth';

/**
 * T094: Lexical 에디터 지연 로딩 최적화
 * next/dynamic을 사용하여 에디터를 사용할 때만 로드
 */
const LexicalEditor = dynamic(
  () => import('@/components/editor/LexicalEditor').then(mod => ({ default: mod.LexicalEditor })),
  {
    loading: () => <Skeleton className="h-64 w-full" />,
    ssr: false, // 클라이언트 전용 컴포넌트
  }
);

/**
 * 게시글 수정 페이지
 * T090: Error Boundary 적용
 *
 * Constitution Principle VI: shadcn/ui 기반 컴포넌트 우선 아키텍처
 * Constitution Principle VIII: content (HTML) + plainContent (Plain Text) 분리 저장
 * Constitution Principle X: Semantic HTML, ARIA, Error Boundary
 */
export default function EditPostPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const router = useRouter();
  const postId = parseInt(id, 10);

  const { data: post, isLoading: isPostLoading } = usePost(postId);
  const { data: user, isLoading: isAuthLoading } = useAuth();
  const { mutate: updatePost, isPending, error } = useUpdatePost();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [plainContent, setPlainContent] = useState('');
  const [hashtags, setHashtags] = useState('');
  const [isInitialized, setIsInitialized] = useState(false);

  /**
   * 게시글 데이터 로드 후 폼 초기화
   */
  useEffect(() => {
    if (post && !isInitialized) {
      setTitle(post.title);
      setContent(post.contentHtml);
      setHashtags(post.hashtags.join(', '));
      setIsInitialized(true);
    }
  }, [post, isInitialized]);

  /**
   * Lexical 에디터 내용 변경 시 호출
   */
  const handleEditorChange = (html: string, plainText: string) => {
    setContent(html);
    setPlainContent(plainText);
  };

  /**
   * 게시글 수정 제출
   */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Layout이 이미 로그인 체크 완료, 권한만 확인
    if (!post || !user) {
      return;
    }

    if (user.id !== post.author.id) {
      toast.error('본인이 작성한 게시글만 수정할 수 있습니다');
      router.push(`/posts/${postId}`);
      return;
    }

    if (!title.trim()) {
      toast.error('제목을 입력하세요');
      return;
    }

    if (!plainContent.trim()) {
      toast.error('내용을 입력하세요');
      return;
    }

    const hashtagArray = hashtags
      .split(/[\s,]+/)
      .filter((tag) => tag.trim().length > 0)
      .slice(0, 10);

    updatePost(
      {
        id: postId,
        request: {
          title: title.trim(),
          contentMarkdown: content, // TODO: Lexical에서 마크다운 직접 생성 필요
          contentHtml: content,
          contentText: plainContent.trim(),
          hashtags: hashtagArray,
        },
      },
      {
        onSuccess: () => {
          toast.success('게시글이 수정되었습니다!');
          router.push(`/posts/${postId}`);
        },
        onError: (err) => {
          console.error('게시글 수정 실패:', err);
          toast.error(err instanceof Error ? err.message : '게시글 수정에 실패했습니다.');
        },
      }
    );
  };

  /**
   * 로딩 상태
   * T089: Skeleton 컴포넌트로 로딩 상태 개선
   */
  if (isPostLoading || isAuthLoading) {
    return (
      <main className="container mx-auto px-4 py-8 max-w-4xl" role="status" aria-label="페이지 로딩 중">
        <Skeleton className="h-8 w-1/4 mb-8" />
        <div className="space-y-6">
          <div>
            <Skeleton className="h-5 w-20 mb-2" />
            <Skeleton className="h-10 w-full" />
          </div>
          <div>
            <Skeleton className="h-5 w-20 mb-2" />
            <Skeleton className="h-64 w-full" />
          </div>
          <div>
            <Skeleton className="h-5 w-28 mb-2" />
            <Skeleton className="h-10 w-full" />
          </div>
        </div>
      </main>
    );
  }

  /**
   * 게시글을 찾을 수 없음
   */
  if (!post) {
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

  /**
   * 권한 없음 (작성자가 아님)
   */
  if (user && user.id !== post.author.id) {
    return (
      <main className="container mx-auto px-4 py-8 max-w-4xl">
        <section className="text-center py-16">
          <h1 className="text-2xl font-bold text-red-600 mb-4">
            수정 권한이 없습니다
          </h1>
          <p className="text-gray-600 mb-6">
            본인이 작성한 게시글만 수정할 수 있습니다.
          </p>
          <Button onClick={() => router.push(`/posts/${postId}`)}>
            게시글로 돌아가기
          </Button>
        </section>
      </main>
    );
  }

  /**
   * 비로그인 상태
   */
  if (!user) {
    return (
      <main className="container mx-auto px-4 py-8 max-w-4xl">
        <section className="text-center py-16">
          <h1 className="text-2xl font-bold mb-4">로그인이 필요합니다</h1>
          <p className="text-gray-600 mb-6">
            게시글을 수정하려면 먼저 로그인해주세요
          </p>
          <Button onClick={() => router.push('/login')}>로그인하기</Button>
        </section>
      </main>
    );
  }

  return (
    <ErrorBoundary>
      <main className="container mx-auto px-4 py-8 max-w-4xl">
        <h1 className="text-3xl font-bold mb-8">게시글 수정</h1>

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
          <LexicalEditor
            initialContent={content}
            onChange={handleEditorChange}
            placeholder="게시글 내용을 작성하세요. 마크다운 문법을 지원합니다."
            disabled={isPending}
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
              게시글 수정에 실패했습니다. 다시 시도해주세요.
            </p>
          </div>
        )}

        <div className="flex gap-3 justify-end">
          <Button
            type="button"
            variant="outline"
            onClick={() => router.push(`/posts/${postId}`)}
            disabled={isPending}
            aria-label="취소"
          >
            취소
          </Button>
          <Button
            type="submit"
            disabled={isPending || !title.trim() || !plainContent.trim()}
            aria-label="게시글 수정"
          >
            {isPending ? '수정 중...' : '수정하기'}
          </Button>
        </div>
      </form>
      </main>
    </ErrorBoundary>
  );
}
