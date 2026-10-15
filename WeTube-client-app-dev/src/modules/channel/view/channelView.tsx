// modules/channel/channel-view.tsx
import FeaturedHero from "../ui/featuredHero";
import SuggestedVideos from "../ui/suggestedVideo";
import HighlightedVideos from "../ui/highlightedVideo";

export default function ChannelView() {
  return (
    <div className="flex flex-col gap-4 px-4 pt-0 pb-4">
      <FeaturedHero />
      <SuggestedVideos />
      <HighlightedVideos />
    </div>
  );
}
