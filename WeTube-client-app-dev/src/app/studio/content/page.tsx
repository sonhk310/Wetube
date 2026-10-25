import { Suspense } from "react";
import ContentListView from "@/modules/studio/ui/views/content-list-view";

export default function StudioContentPage() {
  // ContentListView đọc ?tab= bằng useSearchParams
  return (
    <Suspense fallback={null}>
      <ContentListView />
    </Suspense>
  );
}
