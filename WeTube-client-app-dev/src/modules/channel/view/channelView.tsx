import ChannelVideos from "../ui/channelVideos";

export default function ChannelView() {
  return (
    <div className="flex flex-col gap-4 px-4 pt-0 pb-4">
      <h3 className="text-lg font-semibold">Video mới nhất</h3>
      <ChannelVideos size={6} />
    </div>
  );
}
