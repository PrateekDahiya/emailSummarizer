/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  output: 'export',
  trailingSlash: true,
  images: {
    unoptimized: true,
  },
  // For combined deployment, API calls go to same origin
  async rewrites() {
    return [
      {
        source: '/api/backend/:path*',
        destination: '/api/:path*',
      },
    ];
  },
};

module.exports = nextConfig;