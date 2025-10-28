/**
 * API 클라이언트 (Axios 인스턴스)
 *
 * Constitution Principle IX: JWT 인증을 위한 인터셉터 설정
 * axios.ts를 client.ts로 export하여 posts.ts에서 사용
 */

import api from './axios';

export const apiClient = api;
