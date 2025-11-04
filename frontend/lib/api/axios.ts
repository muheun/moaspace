import axios from 'axios';

/**
 * Axios 인스턴스 (JWT 인터셉터 포함)
 *
 * Constitution Principle IX: JWT 인증을 위한 인터셉터 설정
 */
const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor: JWT 토큰 자동 주입
api.interceptors.request.use(
  (config) => {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('access_token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

/**
 * Response Interceptor: API 오류 처리 및 사용자 친화적 메시지
 * T097: 사용자 친화적 메시지로 API 오류 처리 추가
 */
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // 네트워크 오류 (서버 응답 없음)
    if (!error.response) {
      const enhancedError = new Error(
        '네트워크 연결을 확인해주세요. 서버에 접속할 수 없습니다.'
      );
      return Promise.reject(enhancedError);
    }

    const { status, data } = error.response;

    // 상태 코드별 사용자 친화적 메시지
    let userMessage = '';

    switch (status) {
      case 400:
        userMessage = data?.message || '잘못된 요청입니다. 입력 내용을 확인해주세요.';
        break;
      case 401:
        userMessage = '로그인이 만료되었습니다. 다시 로그인해주세요.';
        // JWT 만료 또는 인증 실패 - localStorage + Cookie 제거 후 리다이렉트
        if (typeof window !== 'undefined') {
          // 동기 import 사용 (require는 동기, dynamic import는 비동기)
          const { isProtectedRoute } = require('@/lib/constants/routes');

          localStorage.removeItem('access_token');
          document.cookie = 'access_token=; Max-Age=0; path=/;';

          const currentPath = window.location.pathname;

          // Protected route이고 이미 로그인 페이지가 아닐 때만 리다이렉트
          if (isProtectedRoute(currentPath) && currentPath !== '/login') {
            window.location.href = '/login';
          }
        }
        break;
      case 403:
        userMessage = '접근 권한이 없습니다. 작성자만 수정/삭제할 수 있습니다.';
        break;
      case 404:
        userMessage = '요청하신 리소스를 찾을 수 없습니다.';
        break;
      case 409:
        userMessage = data?.message || '이미 존재하는 데이터입니다.';
        break;
      case 422:
        userMessage = data?.message || '유효하지 않은 데이터입니다.';
        break;
      case 429:
        userMessage = '요청이 너무 많습니다. 잠시 후 다시 시도해주세요.';
        break;
      case 500:
        userMessage = '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
        break;
      case 502:
      case 503:
        userMessage = '서버가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해주세요.';
        break;
      case 504:
        userMessage = '서버 응답 시간이 초과되었습니다. 다시 시도해주세요.';
        break;
      default:
        userMessage = data?.message || `오류가 발생했습니다 (${status}).`;
    }

    // 에러 객체에 사용자 친화적 메시지 추가
    const enhancedError = Object.assign(new Error(userMessage), {
      status,
      originalError: error,
    });

    return Promise.reject(enhancedError);
  }
);

export default api;
