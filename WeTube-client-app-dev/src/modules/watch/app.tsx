import { useGetVideoDetailQuery } from "@/app/api/videoApi";
import { Comments } from "./components/comments/comments";
import Shorts from "./components/shorts";
import ActiveVideo from "./components/video";
import VideoMetadata from "./components/video-metadata";
import VideoTags from "./components/video-tags";
import VideosList from "./components/videos-list";
import {useVideoStore } from "@/store/zustand/videoStore";
import { useEffect } from "react";
import { useTheater } from "@/hooks/use-controls";
import { cn } from "@/lib/utils";
interface AppProps {
  videoId: string;
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

  if (isLoading)
    return (
        <div className="flex justify-center items-center py-10">
          <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
        </div>
    );
  if (isError) return <p>Đã có lỗi xảy ra.</p>;

  return (
    <section className="h-full w-full relative">
      {/*
        Trình phát luôn là phần tử đầu của lưới (không đổi chỗ trong cây DOM), nên bật/tắt chế độ rạp
        không làm video tải lại. Hàng 2 dùng 1fr để cột phải dài không đẩy giãn hàng của trình phát.
      */}
      <div className="py-6 px-6 xxl:px-20 grid grid-cols-1 md:grid-cols-6 xxl:grid-cols-8 md:grid-rows-[auto_1fr] gap-x-6 h-full overflow-y-scroll">
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
          <Comments />
        </section>
        <section
          className={cn(
            "col-span-1 md:col-span-2 flex flex-col gap-6 pt-6 md:pt-0",
            theater ? "md:row-start-2 md:pt-2" : "md:row-start-1 md:row-span-2"
          )}
        >
          <VideoTags />
          <Shorts />
          <VideosList />
        </section>
      </div>
    </section>
  );
}
