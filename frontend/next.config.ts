import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  experimental: {
    // React Compiler 활성화 (React 19)
    reactCompiler: true,
  },
};

export default nextConfig;
