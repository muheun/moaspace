/**
 * Posts 커스텀 훅 (TanStack Query)
 *
 * Constitution Principle VII: TanStack Query (서버) + React 19 (클라이언트) 상태 분리
 * 서버 상태는 TanStack Query로 관리, 클라이언트 상태는 React hooks로 관리
 */

'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createPost,
  getPostById,
  getPosts,
  updatePost,
  deletePost,
  searchPostsByVector,
} from '@/lib/api/posts';
import type {
  CreatePostRequest,
  UpdatePostRequest,
  PostResponse,
  PostListResponse,
  VectorSearchRequest,
  VectorSearchResponse,
} from '@/types/api/post';

/**
 * Query keys for cache management
 */
export const postKeys = {
  all: ['posts'] as const,
  lists: () => [...postKeys.all, 'list'] as const,
  list: (page: number, size: number, hashtag?: string) =>
    [...postKeys.lists(), { page, size, hashtag }] as const,
  details: () => [...postKeys.all, 'detail'] as const,
  detail: (id: number) => [...postKeys.details(), id] as const,
  search: (query: string) => [...postKeys.all, 'search', query] as const,
};

/**
 * 게시글 생성 mutation
 *
 * @example
 * const { mutate, isPending, error } = useCreatePost();
 * mutate({ title: "제목", content: "<p>내용</p>", plainContent: "내용", hashtags: ["태그"] });
 */
export function useCreatePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreatePostRequest) => createPost(request),
    onSuccess: () => {
      // 게시글 목록 캐시 무효화 (새 게시글 반영)
      queryClient.invalidateQueries({ queryKey: postKeys.lists() });
    },
  });
}

/**
 * 게시글 상세 조회 query
 *
 * @param id 게시글 ID
 * @param enabled 쿼리 활성화 여부 (기본값: true)
 *
 * @example
 * const { data, isLoading, error } = usePost(123);
 */
export function usePost(id: number, enabled: boolean = true) {
  return useQuery({
    queryKey: postKeys.detail(id),
    queryFn: () => getPostById(id),
    enabled,
  });
}

/**
 * 게시글 목록 조회 query (페이지네이션)
 *
 * @param page 페이지 번호 (0부터 시작)
 * @param size 페이지 크기
 * @param hashtag 해시태그 필터 (선택)
 *
 * @example
 * const { data, isLoading, error } = usePosts(0, 20);
 * const { data } = usePosts(1, 20, "Next.js");
 */
export function usePosts(
  page: number = 0,
  size: number = 20,
  hashtag?: string
) {
  return useQuery({
    queryKey: postKeys.list(page, size, hashtag),
    queryFn: () => getPosts(page, size, hashtag),
  });
}

/**
 * 게시글 수정 mutation
 *
 * @example
 * const { mutate } = useUpdatePost();
 * mutate({ id: 123, request: { title: "수정", content: "<p>수정</p>", plainContent: "수정", hashtags: [] } });
 */
export function useUpdatePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: UpdatePostRequest }) =>
      updatePost(id, request),
    onSuccess: (data) => {
      // 해당 게시글 상세 캐시 업데이트
      queryClient.setQueryData(postKeys.detail(data.id), data);
      // 게시글 목록 캐시 무효화 (수정된 내용 반영)
      queryClient.invalidateQueries({ queryKey: postKeys.lists() });
    },
  });
}

/**
 * 게시글 삭제 mutation (소프트 삭제)
 *
 * @example
 * const { mutate } = useDeletePost();
 * mutate(123);
 */
export function useDeletePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => deletePost(id),
    onSuccess: (_, id) => {
      // 삭제된 게시글 캐시 제거
      queryClient.removeQueries({ queryKey: postKeys.detail(id) });
      // 게시글 목록 캐시 무효화 (삭제 반영)
      queryClient.invalidateQueries({ queryKey: postKeys.lists() });
    },
  });
}

/**
 * 벡터 검색 query
 *
 * @param request 검색 요청 (query, threshold, limit)
 * @param enabled 쿼리 활성화 여부 (기본값: false, 수동 실행)
 *
 * @example
 * const { data, refetch } = useSearchPosts({ query: "Next.js", threshold: 0.6 }, false);
 * // 검색 버튼 클릭 시 refetch() 호출
 */
export function useSearchPosts(
  request: VectorSearchRequest,
  enabled: boolean = false
) {
  return useQuery({
    queryKey: postKeys.search(request.query),
    queryFn: () => searchPostsByVector(request),
    enabled,
  });
}
