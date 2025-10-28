'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { LexicalEditor } from '@/components/editor/LexicalEditor';
import { useCreatePost } from '@/lib/hooks/usePosts';
import { useAuth } from '@/lib/hooks/useAuth';

/**
 * 게시글 작성 페이지
 *
 * Constitution Principle VI: shadcn/ui 기반 컴포넌트 우선 아키텍처
 * Constitution Principle VIII: content (HTML) + plainContent (Plain Text) 분리 저장
 * Constitution Principle X: Semantic HTML, ARIA, Error Boundary
 */
export default function NewPostPage() {
  const router = useRouter();
  const { data: user, isLoading: isAuthLoading } = useAuth();
  const { mutate: createPost, isPending, error } = useCreatePost();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [plainContent, setPlainContent] = useState('');
  const [hashtags, setHashtags] = useState('');

  /**
   * Lexical 에디터 내용 변경 시 호출
   * HTML과 Plain Text를 동시에 받아 상태 업데이트
   */
  const handleEditorChange = (html: string, plainText: string) => {
    setContent(html);
    setPlainContent(plainText);
  };

  /**
   * 게시글 작성 제출
   */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!user) {
      alert('로그인이 필요합니다');
      router.push('/login');
      return;
    }

    if (!title.trim()) {
      alert('제목을 입력하세요');
      return;
    }

    if (!plainContent.trim()) {
      alert('내용을 입력하세요');
      return;
    }

    // 해시태그 파싱 (공백 또는 쉼표로 구분)
    const hashtagArray = hashtags
      .split(/[\s,]+/)
      .filter((tag) => tag.trim().length > 0)
      .slice(0, 10);

    createPost(
      {
        title: title.trim(),
        content,
        plainContent: plainContent.trim(),
        hashtags: hashtagArray,
      },
      {
        onSuccess: (data) => {
          router.push(`/posts/${data.id}`);
        },
        onError: (err) => {
          console.error('게시글 작성 실패:', err);
          alert('게시글 작성에 실패했습니다. 다시 시도해주세요.');
        },
      }
    );
  };

  /**
   * 인증 로딩 중
   */
  if (isAuthLoading) {
    return (
      <main className="container mx-auto px-4 py-8 max-w-4xl">
        <div className="animate-pulse">
          <div className="h-8 bg-gray-200 rounded w-1/4 mb-6"></div>
          <div className="space-y-4">
            <div className="h-10 bg-gray-200 rounded"></div>
            <div className="h-64 bg-gray-200 rounded"></div>
          </div>
        </div>
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
            게시글을 작성하려면 먼저 로그인해주세요
          </p>
          <Button onClick={() => router.push('/login')}>로그인하기</Button>
        </section>
      </main>
    );
  }

  return (
    <main className="container mx-auto px-4 py-8 max-w-4xl">
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
          <LexicalEditor
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
            disabled={isPending || !title.trim() || !plainContent.trim()}
            aria-label="게시글 작성"
          >
            {isPending ? '작성 중...' : '작성하기'}
          </Button>
        </div>
      </form>
    </main>
  );
}
