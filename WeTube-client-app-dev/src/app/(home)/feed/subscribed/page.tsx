"use client";

import SubscribedFeedPage from "@/modules/subscribe/ui/views/subcribe-view";
import { Suspense } from 'react';

const Page = () => {
  return (
    <div>
      <h1>Bảng tin kênh đã đăng ký</h1>
      <Suspense fallback={<p>Đang tải bảng tin...</p>}>
        <SubscribedFeedPage />
      </Suspense>
    </div>
  );
};

export default Page;
