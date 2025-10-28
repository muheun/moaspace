/**
 * Error Boundary 컴포넌트
 * T090: 중요 페이지에 Error Boundary 추가
 *
 * Constitution Principle X: Error Boundary 구현
 * Constitution Principle VI: 단일 책임 원칙, Props Interface 명시
 *
 * React 19에서 Error Boundary는 여전히 클래스 컴포넌트만 지원합니다.
 * 자식 컴포넌트에서 발생한 JavaScript 에러를 catch하여 fallback UI를 렌더링합니다.
 */

'use client';

import React, { Component, ReactNode, ErrorInfo } from 'react';
import { AlertCircle, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
}

/**
 * 에러 발생 시 fallback UI를 보여주는 Error Boundary
 *
 * @example
 * ```tsx
 * <ErrorBoundary>
 *   <PostList posts={posts} />
 * </ErrorBoundary>
 * ```
 */
export class ErrorBoundary extends Component<
  ErrorBoundaryProps,
  ErrorBoundaryState
> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
    };
  }

  static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
    // 에러가 발생하면 fallback UI를 보여주도록 상태 업데이트
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    // 에러 로깅 (프로덕션에서는 에러 모니터링 서비스로 전송)
    console.error('ErrorBoundary caught an error:', error, errorInfo);

    this.setState({
      error,
      errorInfo,
    });
  }

  /**
   * 에러 상태 초기화 및 재시도
   */
  handleReset = () => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
    });
  };

  render() {
    if (this.state.hasError) {
      // 커스텀 fallback이 제공된 경우 사용
      if (this.props.fallback) {
        return this.props.fallback;
      }

      // 기본 fallback UI
      return (
        <div className="container mx-auto px-4 py-8" role="alert">
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertTitle>오류가 발생했습니다</AlertTitle>
            <AlertDescription className="mt-2">
              <p className="mb-4">
                예상치 못한 오류가 발생했습니다. 페이지를 새로고침하거나 다시
                시도해주세요.
              </p>
              {process.env.NODE_ENV === 'development' && this.state.error && (
                <details className="mt-4 p-4 bg-red-50 rounded-md text-sm">
                  <summary className="cursor-pointer font-semibold mb-2">
                    개발자 정보 (프로덕션에서는 숨겨짐)
                  </summary>
                  <p className="text-red-800 font-mono whitespace-pre-wrap">
                    {this.state.error.toString()}
                  </p>
                  {this.state.errorInfo && (
                    <pre className="mt-2 text-xs text-red-700 overflow-auto">
                      {this.state.errorInfo.componentStack}
                    </pre>
                  )}
                </details>
              )}
            </AlertDescription>
          </Alert>

          <div className="mt-6 flex gap-4">
            <Button onClick={this.handleReset} variant="default">
              <RefreshCw className="w-4 h-4 mr-2" />
              다시 시도
            </Button>
            <Button
              onClick={() => window.location.reload()}
              variant="outline"
            >
              페이지 새로고침
            </Button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
