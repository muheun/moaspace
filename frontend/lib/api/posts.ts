import apiClient from './client';
import type {
  PostDto,
  CreatePostRequest,
  UpdatePostRequest,
  VectorSearchRequest,
  VectorSearchResponse,
  PostListResponse,
} from '@/types/api/post';

export const postsApi = {
  // 게시글 목록 조회
  async getPosts(page = 0, size = 20, hashtag?: string): Promise<PostListResponse> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    if (hashtag) {
      params.append('hashtag', hashtag);
    }

    const response = await apiClient.get<PostListResponse>(`/api/posts?${params}`);
    return response.data;
  },

  // 게시글 상세 조회
  async getPostById(id: number): Promise<PostDto> {
    const response = await apiClient.get<PostDto>(`/api/posts/${id}`);
    return response.data;
  },

  // 게시글 생성
  async createPost(request: CreatePostRequest): Promise<PostDto> {
    const response = await apiClient.post<PostDto>('/api/posts', request);
    return response.data;
  },

  // 게시글 수정
  async updatePost(id: number, request: UpdatePostRequest): Promise<PostDto> {
    const response = await apiClient.put<PostDto>(`/api/posts/${id}`, request);
    return response.data;
  },

  // 게시글 삭제 (Soft Delete)
  async deletePost(id: number): Promise<void> {
    await apiClient.delete(`/api/posts/${id}`);
  },

  // 벡터 유사도 검색
  async searchPosts(request: VectorSearchRequest): Promise<VectorSearchResponse> {
    const response = await apiClient.post<VectorSearchResponse>('/api/posts/search', request);
    return response.data;
  },
};
