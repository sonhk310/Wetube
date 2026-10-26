import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  typescript: {
    ignoreBuildErrors: true,
  },
  eslint: {
    ignoreDuringBuilds: true,
  },
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "**",
        port: "",
        pathname: "/**",
      },
    ],
  },
  reactStrictMode: false,
  async rewrites() {
    // Link kênh dạng /@handle (và /@handle/videos...): giữ nguyên trên thanh địa chỉ, dùng các trang /channel/[channelId]
    return [{ source: "/@:handle/:path*", destination: "/channel/:handle/:path*" }];
  },
  /* config options here */
};

export default nextConfig;
