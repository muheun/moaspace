'use client';

import { useState } from 'react';
import dynamic from 'next/dynamic';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';

/**
 * 테스트 전용 페이지 - 로그인 없이 에디터 테스트 가능
 *
 * 목적:
 * - Lexical 에디터 UI/UX 검증
 * - 에디터 기능 테스트 (포맷팅, 마크다운, 링크 등)
 * - HTML 및 Plain Text 추출 확인
 * - 개발 환경에서 빠른 디버깅
 */

const LexicalEditor = dynamic(
  () => import('@/components/editor/LexicalEditor').then(mod => ({ default: mod.LexicalEditor })),
  { loading: () => <Skeleton className="h-[400px] w-full" />, ssr: false }
);

export default function TestEditorPage() {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [plainContent, setPlainContent] = useState('');
  const [hashtags, setHashtags] = useState('');

  const handleEditorChange = (html: string, plainText: string) => {
    setContent(html);
    setPlainContent(plainText);
  };

  const handleReset = () => {
    setTitle('');
    setContent('');
    setPlainContent('');
    setHashtags('');
  };

  const hashtagArray = hashtags
    .split(/[\s,]+/)
    .filter((tag) => tag.trim().length > 0)
    .slice(0, 10);

  return (
    <div className="container mx-auto px-4 py-8 max-w-6xl">
      <header className="mb-8">
        <Badge variant="outline" className="mb-4">테스트 전용 페이지</Badge>
        <h1 className="text-4xl font-bold mb-2">Lexical 에디터 테스트</h1>
        <p className="text-muted-foreground">
          로그인 없이 에디터 기능을 테스트할 수 있습니다. 백엔드 API는 호출되지 않습니다.
        </p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* 왼쪽: 에디터 입력 */}
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>게시글 작성</CardTitle>
              <CardDescription>에디터 기능을 자유롭게 테스트하세요</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label htmlFor="title" className="block text-sm font-medium mb-2">
                  제목
                </label>
                <Input
                  id="title"
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="게시글 제목을 입력하세요"
                  maxLength={200}
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-2">내용</label>
                <LexicalEditor
                  onChange={handleEditorChange}
                  placeholder="에디터 기능을 테스트하세요. 마크다운 문법을 지원합니다."
                />
              </div>

              <div>
                <label htmlFor="hashtags" className="block text-sm font-medium mb-2">
                  해시태그
                </label>
                <Input
                  id="hashtags"
                  type="text"
                  value={hashtags}
                  onChange={(e) => setHashtags(e.target.value)}
                  placeholder="태그를 입력하세요 (공백 또는 쉼표로 구분)"
                />
                <div className="flex flex-wrap gap-2 mt-2">
                  {hashtagArray.map((tag, idx) => (
                    <Badge key={idx} variant="secondary">#{tag}</Badge>
                  ))}
                </div>
              </div>

              <Button onClick={handleReset} variant="outline" className="w-full">
                초기화
              </Button>
            </CardContent>
          </Card>
        </div>

        {/* 오른쪽: 실시간 미리보기 */}
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>실시간 미리보기</CardTitle>
              <CardDescription>입력된 데이터를 확인할 수 있습니다</CardDescription>
            </CardHeader>
            <CardContent>
              <Tabs defaultValue="preview" className="w-full">
                <TabsList className="grid w-full grid-cols-3">
                  <TabsTrigger value="preview">미리보기</TabsTrigger>
                  <TabsTrigger value="html">HTML</TabsTrigger>
                  <TabsTrigger value="plain">Plain Text</TabsTrigger>
                </TabsList>

                <TabsContent value="preview" className="space-y-4">
                  <div>
                    <h3 className="font-semibold mb-2">제목</h3>
                    <p className="text-2xl font-bold">{title || '(제목 없음)'}</p>
                  </div>
                  <div>
                    <h3 className="font-semibold mb-2">내용 (HTML 렌더링)</h3>
                    <div
                      className="prose dark:prose-invert max-w-none p-4 border rounded-md min-h-[200px]"
                      dangerouslySetInnerHTML={{ __html: content || '<p class="text-muted-foreground">(내용 없음)</p>' }}
                    />
                  </div>
                  <div>
                    <h3 className="font-semibold mb-2">해시태그</h3>
                    <div className="flex flex-wrap gap-2">
                      {hashtagArray.length > 0 ? (
                        hashtagArray.map((tag, idx) => (
                          <Badge key={idx} variant="secondary">#{tag}</Badge>
                        ))
                      ) : (
                        <span className="text-muted-foreground">(해시태그 없음)</span>
                      )}
                    </div>
                  </div>
                </TabsContent>

                <TabsContent value="html">
                  <div className="bg-muted p-4 rounded-md">
                    <pre className="text-xs overflow-auto max-h-[500px] whitespace-pre-wrap break-words">
                      <code>{content || '(HTML 없음)'}</code>
                    </pre>
                  </div>
                </TabsContent>

                <TabsContent value="plain">
                  <div className="bg-muted p-4 rounded-md">
                    <pre className="text-sm overflow-auto max-h-[500px] whitespace-pre-wrap break-words">
                      {plainContent || '(Plain Text 없음)'}
                    </pre>
                  </div>
                  <div className="mt-4 text-sm text-muted-foreground">
                    <p>글자 수: {plainContent.length}</p>
                    <p>단어 수: {plainContent.trim() ? plainContent.trim().split(/\s+/).length : 0}</p>
                  </div>
                </TabsContent>
              </Tabs>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>API Request 미리보기</CardTitle>
              <CardDescription>실제 API 요청 시 전송될 데이터</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="bg-muted p-4 rounded-md">
                <pre className="text-xs overflow-auto max-h-[300px] whitespace-pre-wrap break-words">
                  <code>
                    {JSON.stringify({
                      title: title.trim() || null,
                      contentMarkdown: content || null,
                      contentHtml: content || null,
                      contentText: plainContent.trim() || null,
                      hashtags: hashtagArray,
                    }, null, 2)}
                  </code>
                </pre>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
