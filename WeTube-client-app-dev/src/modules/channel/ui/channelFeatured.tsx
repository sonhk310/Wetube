// modules/channel/ui/components/channel-featured.tsx
export default function ChannelFeatured() {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-4">
      <div>
        <h2 className="text-lg font-semibold mb-2">Video nổi bật</h2>
        <div className="w-full aspect-video bg-gray-200 rounded-lg" />
      </div>
      <div>
        <h2 className="text-lg font-semibold mb-2">Danh sách phát</h2>
        <div className="space-y-2">
          <div className="w-full h-24 bg-gray-100 rounded-md" />
          <div className="w-full h-24 bg-gray-100 rounded-md" />
        </div>
      </div>
    </div>
  );
}
