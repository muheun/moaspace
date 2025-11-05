import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  experimental: {
    // React Compiler 활성화 (React 19)
    reactCompiler: true,
  },
  images: {
    // 외부 이미지 도메인 허용 (Google OAuth 프로필 이미지)
    remotePatterns: [
      {
        protocol: 'https',
        hostname: 'lh3.googleusercontent.com',
      },
    ],
  },
};

export default nextConfig;
