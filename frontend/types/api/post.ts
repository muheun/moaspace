/**
 * Post API 타입 정의
 *
 * Constitution Principle IX: Backend DTO와 수동 동기화 필요
 * Constitution Principle VIII: content (HTML) + plainContent (Plain Text) 분리
 */

export interface CreatePostRequest {
  title: string;
  content: string;       // HTML
  plainContent: string;  // Plain Text (벡터화용)
  hashtags?: string[];
}

export interface UpdatePostRequest {
  title: string;
  content: string;
  plainContent: string;
  hashtags?: string[];
}

export interface PostResponse {
  id: number;
  title: string;
  content: string;
  author: {
    id: number;
    name: string;
    profileImageUrl?: string;
  };
  hashtags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface PostListResponse {
  content: PostSummary[];
  pageable: {
    page: number;
    size: number;
  };
  totalElements: number;
  totalPages: number;
}

export interface PostSummary {
  id: number;
  title: string;
  author: {
    id: number;
    name: string;
    profileImageUrl?: string;
  };
  hashtags: string[];
  createdAt: string;
}

export interface VectorSearchRequest {
  query: string;
  threshold?: number;  // 기본값: 0.6
  limit?: number;      // 기본값: 20
}

export interface VectorSearchResponse {
  results: Array<{
    post: PostSummary;
    similarity: number;
  }>;
  query: string;
  threshold: number;
  totalResults: number;
}
