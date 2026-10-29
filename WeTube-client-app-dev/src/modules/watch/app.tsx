import { useGetVideoDetailQuery } from "@/app/api/videoApi";
import { Comments } from "./components/comments/comments";
import Shorts from "./components/shorts";
import ActiveVideo from "./components/video";
import VideoMetadata from "./components/video-metadata";
import VideoTags from "./components/video-tags";
import VideosList from "./components/videos-list";
import PlaylistPanel from "./components/playlist-panel";
import ChaptersPanel from "./components/chapters-panel";
import AdCard from "@/modules/ads/ad-card";
import {useVideoStore } from "@/store/zustand/videoStore";
import { useEffect } from "react";
import { useTheater } from "@/hooks/use-controls";
import { cn } from "@/lib/utils";
import { Skeleton } from "@/components/ui/skeleton";
interface AppProps {
  videoId: string;
}

/** Khung chờ cùng lưới với trang xem: trình phát + tiêu đề + kênh bên trái, danh sách video bên phải. */
function WatchSkeleton() {
  return (
    <div className="py-6 px-6 xxl:px-20 grid grid-cols-1 md:grid-cols-6 xxl:grid-cols-8 gap-x-6">
      <div className="col-span-1 md:col-span-4 xxl:col-span-6">
        <Skeleton className="w-full aspect-video rounded-xl" />
        <Skeleton className="h-7 w-3/4 mt-4" />
        <div className="flex items-center gap-3 mt-4">
          <Skeleton className="size-10 rounded-full" />
          <div className="space-y-2">
            <Skeleton className="h-4 w-32" />
            <Skeleton className="h-3 w-24" />
          </div>
        </div>
        <Skeleton className="h-24 w-full mt-4 rounded-xl" />
      </div>
      <div className="col-span-1 md:col-span-2 flex flex-col gap-3 pt-6 md:pt-0">
        {Array.from({ length: 6 }).map((_, i) => (
          <div key={i} className="flex gap-2">
            <Skeleton className="w-40 aspect-video rounded-lg shrink-0" />
            <div className="flex-1 space-y-2">
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-3 w-2/3" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default function App({ videoId }: AppProps) {
  const { data, isLoading, isError } = useGetVideoDetailQuery({videoId});
  const setVideoDetail = useVideoStore((state) => state.setVideoDetail);
  const clearVideoDetail = useVideoStore((state) => state.clearVideoDetail);
  const { theater } = useTheater();

  useEffect(() => {
    if (data) setVideoDetail(data);
    return () => clearVideoDetail();
  }, [data, setVideoDetail, clearVideoDetail]);

  if (isLoading) return <WatchSkeleton />;
  if (isError) return <p>Đã có lỗi xảy ra.</p>;

  return (
    <section className="h-full w-full relative">
      {/*
        Trình phát luôn là phần tử đầu của lưới (không đổi chỗ trong cây DOM), nên bật/tắt chế độ rạp
        không làm video tải lại. Hàng 2 dùng 1fr để cột phải dài không đẩy giãn hàng của trình phát.
      */}
      <div className="py-6 px-6 xxl:px-20 grid grid-cols-1 md:grid-cols-6 xxl:grid-cols-8 md:grid-rows-[auto_1fr] gap-x-6">
        <div
          className={cn(
            theater
              ? "col-span-full -mx-6 -mt-6 mb-4 xxl:-mx-20 bg-black"
              : "col-span-1 md:col-span-4 xxl:col-span-6 md:row-start-1"
          )}
        >
          <ActiveVideo key={videoId} videoId={videoId} />
        </div>
        <section className="col-span-1 md:col-span-4 xxl:col-span-6 md:row-start-2 md:col-start-1">
          <VideoMetadata />
          {/* Điện thoại: quảng cáo nằm dưới thông tin video, trước bình luận */}
          <AdCard variant="side" className="my-4 md:hidden" />
          <Comments />
        </section>
        <section
          className={cn(
            "col-span-1 md:col-span-2 flex flex-col gap-6 pt-6 md:pt-0",
            theater ? "md:row-start-2 md:pt-2" : "md:row-start-1 md:row-span-2"
          )}
        >
          <AdCard variant="side" className="hidden md:block" />
          <ChaptersPanel videoId={videoId} />
          <PlaylistPanel videoId={videoId} />
          <VideoTags />
          <Shorts />
          <VideosList />
        </section>
      </div>
    </section>
  );
}
