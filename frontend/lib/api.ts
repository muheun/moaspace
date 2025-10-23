import axios from 'axios';

// API 기본 설정
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api';

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 타입 정의
export interface Post {
  id: number;
  title: string;
  content: string;
  author: string;
  hasVector: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PostCreateRequest {
  title: string;
  content: string;
  author: string;
}

export interface PostUpdateRequest {
  title?: string;
  content?: string;
}

export interface VectorSearchRequest {
  query: string;
  limit: number;
}

export interface VectorSearchResult {
  post: Post;
  similarityScore: number | null;
}

// API 함수들
export const postsApi = {
  // 모든 게시글 조회
  getAll: async (): Promise<Post[]> => {
    const response = await api.get<Post[]>('/posts');
    return response.data;
  },

  // 게시글 상세 조회
  getById: async (id: number): Promise<Post> => {
    const response = await api.get<Post>(`/posts/${id}`);
    return response.data;
  },

  // 게시글 생성
  create: async (data: PostCreateRequest): Promise<Post> => {
    const response = await api.post<Post>('/posts', data);
    return response.data;
  },

  // 게시글 수정
  update: async (id: number, data: PostUpdateRequest): Promise<Post> => {
    const response = await api.put<Post>(`/posts/${id}`, data);
    return response.data;
  },

  // 게시글 삭제
  delete: async (id: number): Promise<void> => {
    await api.delete(`/posts/${id}`);
  },

  // 벡터 검색
  searchByVector: async (data: VectorSearchRequest): Promise<VectorSearchResult[]> => {
    const response = await api.post<VectorSearchResult[]>('/posts/search/vector', data);
    return response.data;
  },

  // 제목으로 검색
  searchByTitle: async (title: string): Promise<Post[]> => {
    const response = await api.get<Post[]>('/posts/search', {
      params: { title },
    });
    return response.data;
  },
};
