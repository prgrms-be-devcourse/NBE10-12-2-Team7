import type { NextConfig } from "next";

const BACKEND_ORIGIN = process.env.BACKEND_ORIGIN ?? "http://localhost:8080";
// 거래 법률 도우미 — Spring 백엔드와 분리된 독립 FastAPI 서비스(agent/, Python + LangGraph).
const AGENT_ORIGIN = process.env.AGENT_ORIGIN ?? "http://localhost:8000";

const nextConfig: NextConfig = {
  // 컨테이너 배포용: 최소 런타임(.next/standalone)만 뽑아 이미지 경량화
  output: "standalone",
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${BACKEND_ORIGIN}/api/:path*`,
      },
      {
        source: "/agent/:path*",
        destination: `${AGENT_ORIGIN}/agent/:path*`,
      },
    ];
  },
};

export default nextConfig;
