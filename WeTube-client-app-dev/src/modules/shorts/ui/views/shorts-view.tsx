// modules/shorts/ui/views/shorts-view.tsx
import ShortsFeed from "../sections/shorts-feed";

export default function ShortsView() {
  return (
    <main className="relative h-full w-full overflow-hidden bg-black text-white">
      <ShortsFeed />
    </main>
  );
}
