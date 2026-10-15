"use client";

import SubscribedFeedPage from "@/modules/subscribe/ui/views/subcribe-view";
import { Suspense } from 'react';

const Page = () => {
  return (
    <div>
      <h1>My Subscribed Feed</h1>
      <Suspense fallback={<p>Loading feed...</p>}>
        <SubscribedFeedPage />
      </Suspense>
    </div>
  );
};

export default Page;
