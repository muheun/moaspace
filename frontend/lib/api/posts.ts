/**
 * Posts API 클라이언트 함수
 *
 * Constitution Principle IX: Axios 인터셉터 JWT 인증 통합
 * apiClient는 lib/api/client.ts에서 import (JWT 헤더 자동 추가)
 */

import { apiClient } from './client';
import type {
  CreatePostRequest,
  UpdatePostRequest,
  PostResponse,
  PostListResponse,
  VectorSearchRequest,
  VectorSearchResponse,
} from '@/types/api/post';

/**
 * 게시글 생성
 * POST /api/posts
 *
 * @param request 게시글 생성 요청 (title, content, plainContent, hashtags)
 * @returns 생성된 게시글 정보
 */
export async function createPost(
  request: CreatePostRequest
): Promise<PostResponse> {
  const { data } = await apiClient.post<PostResponse>('/api/posts', request);
  return data;
}

/**
 * 게시글 상세 조회
 * GET /api/posts/:id
 *
 * @param id 게시글 ID
 * @returns 게시글 상세 정보
 */
export async function getPostById(id: number): Promise<PostResponse> {
  const { data } = await apiClient.get<PostResponse>(`/api/posts/${id}`);
  return data;
}

/**
 * 게시글 목록 조회 (페이지네이션)
 * GET /api/posts?page=0&size=20&hashtag=tag
 *
 * @param page 페이지 번호 (0부터 시작)
 * @param size 페이지 크기
 * @param hashtag 해시태그 필터 (선택)
 * @returns 게시글 목록 및 페이지 정보
 */
export async function getPosts(
  page: number = 0,
  size: number = 20,
  hashtag?: string
): Promise<PostListResponse> {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
  });

  if (hashtag) {
    params.append('hashtag', hashtag);
  }

  const { data } = await apiClient.get<PostListResponse>(
    `/api/posts?${params.toString()}`
  );
  return data;
}

/**
 * 게시글 수정
 * PUT /api/posts/:id
 *
 * @param id 게시글 ID
 * @param request 게시글 수정 요청 (title, content, plainContent, hashtags)
 * @returns 수정된 게시글 정보
 */
export async function updatePost(
  id: number,
  request: UpdatePostRequest
): Promise<PostResponse> {
  const { data } = await apiClient.put<PostResponse>(
    `/api/posts/${id}`,
    request
  );
  return data;
}

/**
 * 게시글 삭제 (소프트 삭제)
 * DELETE /api/posts/:id
 *
 * @param id 게시글 ID
 * @returns void (204 No Content)
 */
export async function deletePost(id: number): Promise<void> {
  await apiClient.delete(`/api/posts/${id}`);
}

/**
 * 벡터 검색 (유사 게시글 검색)
 * POST /api/posts/search
 *
 * @param request 검색 요청 (query, threshold, limit)
 * @returns 유사도 기반 검색 결과
 */
export async function searchPostsByVector(
  request: VectorSearchRequest
): Promise<VectorSearchResponse> {
  const { data } = await apiClient.post<VectorSearchResponse>(
    '/api/posts/search',
    request
  );
  return data;
}
