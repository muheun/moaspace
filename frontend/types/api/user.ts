/**
 * User API 타입 정의
 *
 * Constitution Principle IX: Backend DTO와 수동 동기화 필요
 */

export interface UserResponse {
  id: number;
  email: string;
  name: string;
  profileImageUrl?: string;
  createdAt: string;
}
