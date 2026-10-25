// modules/shorts/ui/views/shorts-view.tsx
import ShortsFeed from "../sections/shorts-feed";

export default function ShortsView({ startId }: { startId?: string }) {
  return (
    <main className="relative h-full w-full overflow-hidden bg-white">
      <ShortsFeed startId={startId} />
    </main>
  );
}
