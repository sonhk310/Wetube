"use client";

import ProtectedRoute from '@/components/ProtectedRoute';

const Page = () => {
  return (
    <ProtectedRoute>
      <div>Danh sách phát</div>;
    </ProtectedRoute>
  );
};

export default Page;
