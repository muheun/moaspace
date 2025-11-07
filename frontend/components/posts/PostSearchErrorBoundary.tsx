'use client';

import { AlertCircle } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';

interface PostSearchErrorBoundaryProps {
  error: Error;
  onRetry?: () => void;
}

export function PostSearchErrorBoundary({ error, onRetry }: PostSearchErrorBoundaryProps) {
  const getErrorMessage = (error: Error): string => {
    if (error.message.includes('401') || error.message.includes('Unauthorized')) {
      return '로그인이 필요합니다. 로그인 후 다시 시도해주세요.';
    }
    if (error.message.includes('403') || error.message.includes('Forbidden')) {
      return '접근 권한이 없습니다.';
    }
    if (error.message.includes('404') || error.message.includes('Not Found')) {
      return '검색 API를 찾을 수 없습니다. 서버 상태를 확인해주세요.';
    }
    if (error.message.includes('Network') || error.message.includes('fetch')) {
      return '네트워크 연결을 확인해주세요.';
    }
    return '검색 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
  };

  return (
    <Alert variant="destructive">
      <AlertCircle className="h-4 w-4" />
      <AlertTitle>검색 실패</AlertTitle>
      <AlertDescription className="space-y-3">
        <p>{getErrorMessage(error)}</p>
        {onRetry && (
          <Button variant="outline" size="sm" onClick={onRetry}>
            다시 시도
          </Button>
        )}
      </AlertDescription>
    </Alert>
  );
}
