import axios from 'axios';

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
});

// JWT 인터셉터 (Request) + Cookie 동기화
apiClient.interceptors.request.use(
  (config) => {
    // localStorage에서 JWT 토큰 가져오기
    const token = localStorage.getItem('access_token');

    if (token) {
      // 1. Authorization 헤더 설정 (API 요청용)
      config.headers.Authorization = `Bearer ${token}`;

      // 2. Cookie 동기화 (Middleware 인증용)
      // 브라우저에서 Cookie가 없거나 만료된 경우 자동으로 재설정
      if (typeof document !== 'undefined') {
        const cookieValue = document.cookie
          .split('; ')
          .find(row => row.startsWith('access_token='))
          ?.split('=')[1];

        // Cookie가 없거나 localStorage와 다른 경우 동기화
        if (!cookieValue || cookieValue !== token) {
          document.cookie = `access_token=${token}; path=/; max-age=${60*60*24*7}; SameSite=Lax`;
        }
      }
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 응답 인터셉터 (401 처리)
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // /api/auth/me는 선택적 인증 (401을 조용히 처리)
      const isAuthCheck = error.config?.url?.includes('/api/auth/me');

      if (!isAuthCheck) {
        // JWT 만료 시 localStorage와 Cookie 모두 제거
        localStorage.removeItem('access_token');

        if (typeof document !== 'undefined') {
          document.cookie = 'access_token=; path=/; max-age=0; SameSite=Lax';
        }

        // 현재 페이지가 로그인 페이지가 아닐 경우에만 리다이렉션
        if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
