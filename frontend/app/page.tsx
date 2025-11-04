import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { FileText, Search, Zap } from 'lucide-react';

/**
 * 메인 페이지 (랜딩 페이지)
 *
 * Constitution Principle VII: shadcn/ui 활용, 간단한 정적 콘텐츠
 * Web UI Design Guide: 16px minimum text, 8px grid spacing
 *
 * 기능:
 * - 프로젝트 소개
 * - 주요 기능 안내
 * - 게시판으로 이동 CTA
 */
export default function Home() {
  return (
    <div className="max-w-4xl mx-auto space-y-8">
      {/* 환영 메시지 */}
      <div className="text-center space-y-4 py-12">
        <h1 className="text-4xl font-bold tracking-tight">
          벡터 검색 게시판에 오신 것을 환영합니다
        </h1>
        <p className="text-xl text-muted-foreground max-w-2xl mx-auto">
          PostgreSQL + pgvector를 활용한 의미 기반 검색 게시판입니다.
          일반 키워드 검색을 넘어 문맥과 의미를 이해하는 스마트 검색을 경험해보세요.
        </p>
      </div>

      {/* 주요 기능 소개 */}
      <div className="grid md:grid-cols-3 gap-6">
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <FileText className="w-5 h-5 text-primary" />
              <CardTitle>게시글 관리</CardTitle>
            </div>
          </CardHeader>
          <CardContent>
            <CardDescription className="text-base">
              Lexical 에디터를 활용한 마크다운 기반 게시글 작성 및 관리
            </CardDescription>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Search className="w-5 h-5 text-primary" />
              <CardTitle>의미 기반 검색</CardTitle>
            </div>
          </CardHeader>
          <CardContent>
            <CardDescription className="text-base">
              pgvector를 활용한 벡터 유사도 검색으로 관련 콘텐츠 발견
            </CardDescription>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Zap className="w-5 h-5 text-primary" />
              <CardTitle>실시간 처리</CardTitle>
            </div>
          </CardHeader>
          <CardContent>
            <CardDescription className="text-base">
              ONNX Runtime 기반 임베딩 생성으로 빠른 벡터화
            </CardDescription>
          </CardContent>
        </Card>
      </div>

      {/* CTA 버튼 */}
      <div className="flex flex-col items-center gap-4 py-8">
        <div className="flex flex-col sm:flex-row items-center gap-4">
          <Button asChild size="lg" className="text-lg px-8 py-6">
            <Link href="/posts">
              게시판으로 이동
            </Link>
          </Button>
          <Button asChild size="lg" variant="outline" className="text-lg px-8 py-6">
            <Link href="/test-editor">
              에디터 테스트
            </Link>
          </Button>
        </div>
        <p className="text-sm text-muted-foreground text-center">
          게시글을 작성하고 벡터 검색을 경험해보세요 · 로그인 없이 에디터를 테스트할 수 있습니다
        </p>
      </div>

      {/* 기술 스택 정보 */}
      <Card className="bg-muted/50">
        <CardHeader>
          <CardTitle className="text-base">기술 스택</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid md:grid-cols-2 gap-4 text-sm text-muted-foreground">
            <div>
              <p className="font-semibold text-foreground mb-2">Frontend</p>
              <ul className="space-y-1">
                <li>• Next.js 15 (App Router, Turbopack)</li>
                <li>• React 19 + React Compiler</li>
                <li>• TanStack Query (서버 상태)</li>
                <li>• shadcn/ui + Tailwind CSS</li>
              </ul>
            </div>
            <div>
              <p className="font-semibold text-foreground mb-2">Backend</p>
              <ul className="space-y-1">
                <li>• Spring Boot 3.2.1 (Kotlin)</li>
                <li>• PostgreSQL 18 + pgvector</li>
                <li>• ONNX Runtime (multilingual-e5-base)</li>
                <li>• QueryDSL + MyBatis</li>
              </ul>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
