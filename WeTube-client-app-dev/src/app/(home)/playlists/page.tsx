"use client";

import ProtectedRoute from '@/components/ProtectedRoute';

const Page = () => {
  return (
    <ProtectedRoute>
      <div>Playlist</div>;
    </ProtectedRoute>
  );
};

export default Page;
