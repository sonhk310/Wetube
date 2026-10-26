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
    // Link kênh dạng /@handle
    return [{ source: "/@:handle", destination: "/at/:handle" }];
  },
  /* config options here */
};

export default nextConfig;
