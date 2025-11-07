// Frontend API Types (Backend DTO와 수동 동기화)
// Constitution Principle X: API 타입은 frontend/types/api/ 디렉토리에 정의

export interface PostDto {
  id: number;
  title: string;
  contentMarkdown: string;
  contentHtml: string;
  author: AuthorInfo;
  hashtags: string[];
  createdAt: string;  // ISO 8601
  updatedAt?: string;  // ISO 8601, 수정이 없으면 null
}

export interface AuthorInfo {
  id: number;
  name: string;
  profileImageUrl: string | null;
}

export interface CreatePostRequest {
  title: string;              // 1~200자, 필수
  contentHtml: string;        // HTML 렌더링, 필수 (Backend가 Markdown과 Text 자동 변환)
  hashtags: string[];         // 해시태그 배열, 기본값 []
}

export interface UpdatePostRequest {
  title: string;               // 1~200자, 필수 (Backend 요구사항)
  contentHtml: string;         // HTML 렌더링, 필수 (Backend가 Markdown과 Text 자동 변환)
  hashtags: string[];          // 해시태그 배열, 기본값 [] (Backend 요구사항)
}

export interface VectorSearchRequest {
  query: string;           // 검색어, 필수
  threshold?: number;      // 유사도 임계값 (0.0~1.0), 기본값 0.0
  limit?: number;          // 결과 개수 (1~100), 기본값 10
}

export interface VectorSearchResponse {
  results: Array<{
    post: PostDto;             // 게시글 정보
    similarityScore: number;   // 유사도 점수 (0.0~1.0)
  }>;
}

// Phase 6: 필드별 가중치 검색 타입 (User Story 4)
export type PostSearchField = 'title' | 'content' | 'hashtags' | 'author';

export interface PostSearchRequest {
  query: string;                    // 검색어, 필수
  fields?: PostSearchField[];       // 검색 대상 필드 (미지정 시 모든 필드)
  limit?: number;                   // 최대 결과 개수 (1~100), 기본값 10
}

export interface PostSearchResultItem {
  post: PostDto;                    // 게시글 정보
  totalScore: number;               // 전체 가중 합산 스코어
  fieldScores: Record<string, number>;  // 필드별 유사도 스코어 (디버깅용)
}

export interface PostSearchResponse {
  results: PostSearchResultItem[];  // 검색 결과 목록
  totalResults: number;             // 총 결과 개수
}

export interface PostListResponse {
  posts: PostDto[];          // 게시글 목록
  pagination: {
    page: number;            // 현재 페이지 (0-based)
    size: number;            // 페이지 크기
    totalElements: number;   // 전체 게시글 수
    totalPages: number;      // 전체 페이지 수
  };
}

export interface ErrorResponse {
  error: {
    code: 'FORBIDDEN' | 'POST_NOT_FOUND' | 'VALIDATION_ERROR' | 'UNAUTHORIZED';
    message: string;           // 에러 메시지 (한글)
    timestamp: string;         // 에러 발생 시각 (ISO 8601)
  };
}
