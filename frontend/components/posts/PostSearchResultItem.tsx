'use client';

import Link from 'next/link';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import type { PostSearchResultItem as SearchItem } from '@/types/api/post';

interface PostSearchResultItemProps {
  item: SearchItem;
}

export function PostSearchResultItem({ item }: PostSearchResultItemProps) {
  const { post, totalScore, fieldScores } = item;

  const formatScore = (score: number) => score.toFixed(2);
  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  const FIELD_LABELS: Record<string, string> = {
    title: '제목',
    content: '내용',
    hashtags: '해시태그',
    author: '작성자',
  };

  return (
    <Link href={`/posts/${post.id}`}>
      <Card className="hover:bg-accent/50 transition-colors cursor-pointer">
        <CardHeader>
          <div className="flex items-start justify-between gap-4">
            <div className="flex-1 space-y-2">
              <CardTitle className="text-xl">{post.title}</CardTitle>
              <CardDescription>
                {post.author.name} · {formatDate(post.createdAt)}
              </CardDescription>
            </div>
            <div className="flex flex-col items-end gap-1">
              <Badge variant="secondary" className="font-mono">
                스코어: {formatScore(totalScore)}
              </Badge>
            </div>
          </div>
        </CardHeader>

        <CardContent className="space-y-3">
          {/* 해시태그 */}
          {post.hashtags && post.hashtags.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {post.hashtags.map((tag) => (
                <Badge key={tag} variant="outline">
                  #{tag}
                </Badge>
              ))}
            </div>
          )}

          {/* 필드별 스코어 (디버깅용) */}
          {Object.keys(fieldScores).length > 0 && (
            <details className="text-xs text-muted-foreground">
              <summary className="cursor-pointer hover:text-foreground">
                필드별 스코어 보기
              </summary>
              <div className="mt-2 space-y-1 pl-4">
                {Object.entries(fieldScores).map(([field, score]) => (
                  <div key={field} className="flex justify-between">
                    <span>{FIELD_LABELS[field] || field}:</span>
                    <span className="font-mono">{formatScore(score)}</span>
                  </div>
                ))}
              </div>
            </details>
          )}
        </CardContent>
      </Card>
    </Link>
  );
}
