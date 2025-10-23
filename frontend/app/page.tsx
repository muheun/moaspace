'use client';

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { postsApi, type Post, type VectorSearchResult } from '@/lib/api';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { formatDistance } from 'date-fns';
import { ko } from 'date-fns/locale';

export default function Home() {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const [searchMode, setSearchMode] = useState<'all' | 'vector'>('all');
  const [searchResults, setSearchResults] = useState<VectorSearchResult[]>([]);

  // 새 게시글 입력 상태
  const [newPost, setNewPost] = useState({
    title: '',
    content: '',
    author: '',
  });

  // 모든 게시글 조회
  const { data: posts = [], isLoading } = useQuery({
    queryKey: ['posts'],
    queryFn: postsApi.getAll,
  });

  // 게시글 생성
  const createMutation = useMutation({
    mutationFn: postsApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['posts'] });
      setNewPost({ title: '', content: '', author: '' });
    },
  });

  // 벡터 검색
  const searchMutation = useMutation({
    mutationFn: (query: string) => postsApi.searchByVector({ query, limit: 10 }),
    onSuccess: (data) => {
      setSearchResults(data);
      setSearchMode('vector');
    },
  });

  const handleCreatePost = (e: React.FormEvent) => {
    e.preventDefault();
    if (newPost.title && newPost.content && newPost.author) {
      createMutation.mutate(newPost);
    }
  };

  const handleVectorSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      searchMutation.mutate(searchQuery);
    }
  };

  const handleShowAllPosts = () => {
    setSearchMode('all');
    setSearchQuery('');
    setSearchResults([]);
  };

  const displayPosts = searchMode === 'all' ? posts : searchResults.map(r => r.post);

  return (
    <div className="space-y-8">
      {/* 벡터 검색 섹션 */}
      <Card>
        <CardHeader>
          <CardTitle>벡터 검색</CardTitle>
          <CardDescription>
            의미 기반으로 관련 게시글을 검색합니다
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleVectorSearch} className="flex gap-2">
            <Input
              placeholder="검색어를 입력하세요..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="flex-1"
            />
            <Button type="submit" disabled={searchMutation.isPending}>
              {searchMutation.isPending ? '검색 중...' : '검색'}
            </Button>
            {searchMode === 'vector' && (
              <Button type="button" variant="outline" onClick={handleShowAllPosts}>
                전체 보기
              </Button>
            )}
          </form>
          {searchMode === 'vector' && (
            <p className="text-sm text-gray-500 mt-2">
              검색 결과: {searchResults.length}개
            </p>
          )}
        </CardContent>
      </Card>

      {/* 새 게시글 작성 */}
      <Card>
        <CardHeader>
          <CardTitle>새 게시글 작성</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleCreatePost} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="title">제목</Label>
              <Input
                id="title"
                value={newPost.title}
                onChange={(e) => setNewPost({ ...newPost, title: e.target.value })}
                placeholder="제목을 입력하세요"
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="content">내용</Label>
              <Textarea
                id="content"
                value={newPost.content}
                onChange={(e) => setNewPost({ ...newPost, content: e.target.value })}
                placeholder="내용을 입력하세요"
                rows={5}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="author">작성자</Label>
              <Input
                id="author"
                value={newPost.author}
                onChange={(e) => setNewPost({ ...newPost, author: e.target.value })}
                placeholder="작성자명을 입력하세요"
                required
              />
            </div>
            <Button type="submit" disabled={createMutation.isPending}>
              {createMutation.isPending ? '작성 중...' : '작성하기'}
            </Button>
          </form>
        </CardContent>
      </Card>

      {/* 게시글 목록 */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-bold">
            {searchMode === 'all' ? '전체 게시글' : '검색 결과'}
          </h2>
          <span className="text-sm text-gray-500">
            총 {displayPosts.length}개
          </span>
        </div>

        {isLoading ? (
          <div className="text-center py-8 text-gray-500">로딩 중...</div>
        ) : displayPosts.length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            {searchMode === 'vector' ? '검색 결과가 없습니다.' : '게시글이 없습니다.'}
          </div>
        ) : (
          <div className="grid gap-4">
            {displayPosts.map((post, index) => {
              const result = searchMode === 'vector' ? searchResults[index] : null;
              return (
                <Card key={post.id}>
                  <CardHeader>
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <CardTitle className="text-lg">{post.title}</CardTitle>
                        <CardDescription className="flex items-center gap-2 mt-1">
                          <span>{post.author}</span>
                          <span>•</span>
                          <span>
                            {formatDistance(new Date(post.createdAt), new Date(), {
                              addSuffix: true,
                              locale: ko,
                            })}
                          </span>
                        </CardDescription>
                      </div>
                      {result && result.similarityScore !== null && (
                        <div className="text-sm text-gray-500 bg-gray-100 px-2 py-1 rounded">
                          유사도: {(1 - result.similarityScore).toFixed(3)}
                        </div>
                      )}
                    </div>
                  </CardHeader>
                  <CardContent>
                    <p className="text-gray-700 whitespace-pre-wrap">{post.content}</p>
                    <div className="mt-3 flex items-center gap-2">
                      {post.hasVector ? (
                        <span className="text-xs bg-green-100 text-green-700 px-2 py-1 rounded">
                          벡터 생성됨
                        </span>
                      ) : (
                        <span className="text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded">
                          벡터 미생성
                        </span>
                      )}
                    </div>
                  </CardContent>
                </Card>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
