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
  contentMarkdown: string;    // 마크다운 원본, 필수
  contentHtml: string;        // HTML 렌더링, 필수
  contentText: string;        // Plain Text 벡터화용, 필수
  hashtags: string[];         // 해시태그 배열, 기본값 []
}

export interface UpdatePostRequest {
  title?: string;              // 1~200자, 선택
  contentMarkdown?: string;    // 마크다운 원본, 선택
  contentHtml?: string;        // HTML 렌더링, 선택
  contentText?: string;        // Plain Text 벡터화용, 선택
  hashtags?: string[];         // 해시태그 배열, 선택
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
