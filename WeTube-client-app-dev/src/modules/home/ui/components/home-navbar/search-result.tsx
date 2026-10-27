"use client";

import { useState, useEffect, Suspense, Fragment } from "react";
import { useSearchParams } from "next/navigation";
import { useProgressRouter } from "@/hooks/use-progress-router";
import Image from "next/image";
import { SlidersHorizontal, X } from "lucide-react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import apiClient from "@/lib/apiClient";
import { cn, formatCompact, formatDuration } from "@/lib/utils";


interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

interface VideoDto {
  id: string;
  title: string;
  description: string;
  thumbnailUrl: string;
  duration: number;
  totalView: number;
  createdAt: string;
  author: {
    id: string;
    name: string;
    picture: string;
  };
  isShort?: boolean;
}

interface ChannelResult {
  id: string;
  name: string;
  picture: string | null;
  description: string | null;
  totalSubscribers: number;
  totalVideos: number;
}

type FilterKey = "type" | "duration" | "uploaded" | "sort";

// Giá trị gửi lên API (backend: SearchFilters) và nhãn như YouTube tiếng Việt
const TYPE_CHIPS = [
  { value: "", label: "Tất cả" },
  { value: "video", label: "Video" },
  { value: "short", label: "Shorts" },
  { value: "channel", label: "Kênh" },
];

const FILTER_GROUPS: { key: FilterKey; title: string; options: { value: string; label: string }[] }[] = [
  {
    key: "duration",
    title: "Thời lượng",
    options: [
      { value: "short", label: "Dưới 4 phút" },
      { value: "medium", label: "4 - 20 phút" },
      { value: "long", label: "Trên 20 phút" },
    ],
  },
  {
    key: "uploaded",
    title: "Ngày tải lên",
    options: [
      { value: "today", label: "Hôm nay" },
      { value: "week", label: "Tuần này" },
      { value: "month", label: "Tháng này" },
      { value: "year", label: "Năm nay" },
    ],
  },
  {
    key: "sort",
    title: "Sắp xếp theo",
    options: [
      { value: "", label: "Mức độ liên quan" },
      { value: "date", label: "Ngày tải lên" },
      { value: "views", label: "Lượt xem" },
    ],
  },
];

interface VideoResult {
  id: string;
  title: string;
  thumbnail: string;
  channel: {
    name: string;
    picture: string;
  };
  duration: string;
  views: string;
  uploadTime: string;
  description?: string;
  isShort: boolean;
}

const toVideoResult = (dto: VideoDto): VideoResult => ({
  id: dto.id,
  title: dto.title,
  thumbnail: dto.thumbnailUrl,
  description: dto.description,
  duration: formatDuration(dto.duration),
  views: `${dto.totalView ? Number(dto.totalView).toLocaleString() : 0} lượt xem`,
  uploadTime: new Date(dto.createdAt).toLocaleDateString("vi-VN"),
  channel: {
    name: dto.author?.name || "Unknown",
    picture: dto.author?.picture || "/default-avatar.png",
  },
  isShort: !!dto.isShort,
});

/** Khung chờ của danh sách kết quả: ảnh bên trái, tiêu đề + thông tin bên phải. */
function ResultsSkeleton() {
  return (
    <div className="space-y-4">
      {Array.from({ length: 5 }).map((_, i) => (
        <div key={i} className="flex gap-4">
          <Skeleton className="w-80 h-48 rounded-lg shrink-0" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-6 w-3/4" />
            <Skeleton className="h-4 w-1/2" />
            <Skeleton className="h-4 w-1/3" />
          </div>
        </div>
      ))}
    </div>
  );
}

function SearchResultsContent() {
  const searchParams = useSearchParams();
  const router = useProgressRouter();

  const [results, setResults] = useState<VideoResult[]>([]);
  const [channels, setChannels] = useState<ChannelResult[]>([]);
  const [similar, setSimilar] = useState<VideoResult[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState<boolean>(true);
  const [hoveredId, setHoveredId] = useState<string | null>(null);
  const [panelOpen, setPanelOpen] = useState(false);

  const query = searchParams.get("query");
  const type = searchParams.get("type") ?? "";
  const duration = searchParams.get("duration") ?? "";
  const uploaded = searchParams.get("uploaded") ?? "";
  const sort = searchParams.get("sort") ?? "";
  const isChannel = type === "channel";
  const hasFilter = Boolean(duration || uploaded || sort);

  /** Đổi một bộ lọc trên URL (chia sẻ / quay lại được); bấm lại lựa chọn đang chọn thì bỏ chọn. */
  const setFilter = (key: FilterKey, value: string) => {
    const params = new URLSearchParams(searchParams.toString());
    if (!value || params.get(key) === value) params.delete(key);
    else params.set(key, value);
    if (key === "type" && value === "channel") {
      // Kênh không lọc theo thời lượng / ngày / sắp xếp
      ["duration", "uploaded", "sort"].forEach((k) => params.delete(k));
      setPanelOpen(false);
    }
    router.replace(`/search?${params.toString()}`);
  };

  const clearFilters = () => {
    const params = new URLSearchParams(searchParams.toString());
    ["duration", "uploaded", "sort"].forEach((k) => params.delete(k));
    router.replace(`/search?${params.toString()}`);
  };

  useEffect(() => {
    if (query) {
      setLoading(true);
      setSimilar([]);
      const fetchResults = async () => {
        try {
          if (isChannel) {
            const response = await apiClient.get<ChannelResult[]>("/channels/search", {
              params: { q: query },
            });
            setChannels(response.data);
            setTotal(response.data.length);
            setResults([]);
            return;
          }
          // Tab "Tất cả" (chưa lọc gì): thêm tối đa 2 kênh khớp chặt ở đầu, như YouTube
          const channelsPromise =
            type === "" && !hasFilter
              ? apiClient
                  .get<ChannelResult[]>("/channels/search", { params: { q: query, limit: 2, strict: true } })
                  .then((r) => r.data)
                  .catch(() => [] as ChannelResult[])
              : Promise.resolve([] as ChannelResult[]);
          const response = await apiClient.get<PageResponse<VideoDto>>(
            "/videos/search/full",
            {
              params: {
                title: query,
                page: 0,
                size: 20,
                type: type || undefined,
                duration: duration || undefined,
                uploaded: uploaded || undefined,
                sort: sort || undefined,
              },
            }
          );

          const mappedResults: VideoResult[] = response.data.content.map(toVideoResult);

          setResults(mappedResults);
          setChannels(await channelsPromise);
          setTotal(response.data.totalElements ?? mappedResults.length);
          // "Video tương tự": chỉ khi chưa lọc, và không chặn kết quả chính nếu lỗi
          if (!hasFilter && mappedResults.length > 0) {
            apiClient
              .get<VideoDto[]>("/videos/search/similar", {
                params: { title: query, size: 20, type: type || undefined },
              })
              .then((r) => setSimilar(r.data.map(toVideoResult)))
              .catch(() => setSimilar([]));
          }
        } catch (error) {
          console.error("Failed to fetch search results:", error);
          setResults([]);
          setChannels([]);
          setTotal(0);
        } finally {
          setLoading(false);
        }
      };
      fetchResults();
    } else {
      setResults([]);
      setChannels([]);
      setLoading(false);
    }
  }, [query, isChannel, type, duration, uploaded, sort, hasFilter]);

  // Điểm yêu thích thẻ do backend tự cộng khi lượt xem được tính, không gửi từ đây
  const handleClick = (videoId: string, isShort = false) => {
    router.push(isShort ? `/shorts/${videoId}` : `/watch/${videoId}`);
  };

  // Như YouTube: Shorts gom thành một hàng thẻ dọc, đặt sau 2 video đầu
  const shortResults = results.filter((v) => v.isShort);
  const videoResults = results.filter((v) => !v.isShort);
  const shelfAt = Math.min(2, videoResults.length);
  const shortsShelf = shortResults.length > 0 && (
    <section className="py-2">
      <h2 className="mb-3 text-xl font-bold">Shorts</h2>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
        {shortResults.map((s) => (
          <div key={s.id} onClick={() => handleClick(s.id, true)} className="cursor-pointer">
            <div className="relative aspect-[9/16] overflow-hidden rounded-xl bg-gray-300">
              <Image src={s.thumbnail} alt={s.title} fill sizes="(max-width: 640px) 50vw, 220px" className="object-cover" />
            </div>
            <h3 className="mt-2 line-clamp-2 font-medium">{s.title}</h3>
            <p className="text-sm text-gray-600">{s.views}</p>
          </div>
        ))}
      </div>
    </section>
  );

  const filterBar = (
    <div className="mb-4">
      <div className="flex flex-wrap items-center gap-2">
        {TYPE_CHIPS.map((chip) => (
          <button
            key={chip.value || "all"}
            type="button"
            onClick={() => setFilter("type", chip.value)}
            className={cn(
              "rounded-lg px-3 py-1.5 text-sm font-medium transition-colors",
              type === chip.value ? "bg-black text-white" : "bg-gray-100 hover:bg-gray-200"
            )}
          >
            {chip.label}
          </button>
        ))}
        {!isChannel && (
          <button
            type="button"
            onClick={() => setPanelOpen((o) => !o)}
            aria-expanded={panelOpen}
            className={cn(
              "ml-auto flex items-center gap-1.5 rounded-full px-3 py-1.5 text-sm font-medium hover:bg-gray-100",
              (panelOpen || hasFilter) && "bg-gray-100"
            )}
          >
            <SlidersHorizontal className="h-4 w-4" />
            Bộ lọc
          </button>
        )}
      </div>

      <Dialog open={panelOpen && !isChannel} onOpenChange={setPanelOpen}>
        <DialogContent className="grid-cols-[minmax(0,1fr)] sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>Bộ lọc tìm kiếm</DialogTitle>
          </DialogHeader>
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-3">
          {FILTER_GROUPS.map((group) => {
            const current = { type, duration, uploaded, sort }[group.key];
            return (
              <div key={group.key}>
                <p className="mb-2 border-b pb-2 text-xs font-semibold uppercase text-gray-500">{group.title}</p>
                <div className="flex flex-col">
                  {group.options.map((opt) => {
                    const active = current === opt.value;
                    return (
                      <button
                        key={opt.value || "default"}
                        type="button"
                        onClick={() => {
                          setFilter(group.key, opt.value);
                          setPanelOpen(false); // như YouTube: chọn xong thì đóng
                        }}
                        className={cn(
                          "flex items-center justify-between rounded-md px-2 py-1.5 text-left text-sm hover:bg-gray-100",
                          active && "font-semibold"
                        )}
                      >
                        {opt.label}
                        {active && opt.value && <X className="h-3.5 w-3.5 text-gray-500" />}
                      </button>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
        </DialogContent>
      </Dialog>
    </div>
  );

  // --- Loading Skeleton ---
  if (loading) {
    return (
      <div className="container mx-auto px-4 py-6">
        {filterBar}
        <ResultsSkeleton />
      </div>
    );
  }

  // --- Render Results ---
  return (
    <main className="container mx-auto px-4 py-6">
      {filterBar}
      <div className="mb-6 flex flex-wrap items-center gap-3">
        <p className="text-gray-600">
          Khoảng {total.toLocaleString("vi-VN")} kết quả cho <b>{query}</b>
        </p>
        {hasFilter && !isChannel && (
          <button type="button" onClick={clearFilters} className="text-sm font-medium text-blue-600 hover:underline">
            Xoá bộ lọc
          </button>
        )}
      </div>
      {!isChannel && results.length === 0 && channels.length === 0 && (
        <p className="py-10 text-center text-gray-600">
          Không tìm thấy kết quả nào{hasFilter ? ". Thử bỏ bớt bộ lọc." : "."}
        </p>
      )}
      {(isChannel || channels.length > 0) && (
        <div className="mb-4 space-y-2">
          {isChannel && channels.length === 0 && (
            <p className="py-10 text-center text-gray-600">Không tìm thấy kênh nào.</p>
          )}
          {channels.map((ch) => (
            <div
              key={ch.id}
              onClick={() => router.push(`/channel/${ch.id}`)}
              className="flex cursor-pointer items-center gap-4 rounded-lg p-2 transition-colors hover:bg-gray-100 sm:gap-8"
            >
              <div className="flex w-24 shrink-0 justify-center sm:w-80">
                <Image
                  src={ch.picture || "/default-avatar.png"}
                  alt={ch.name}
                  width={136}
                  height={136}
                  className="h-24 w-24 rounded-full object-cover sm:h-[136px] sm:w-[136px]"
                />
              </div>
              <div className="min-w-0 flex-1">
                <h3 className="text-lg font-medium">{ch.name}</h3>
                <p className="text-sm text-gray-600">
                  {formatCompact(ch.totalSubscribers)} người đăng ký • {ch.totalVideos} video
                </p>
                {ch.description && (
                  <p className="mt-1 line-clamp-2 text-sm text-gray-600">{ch.description}</p>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
      <div className="space-y-4">
        {videoResults.map((video, index) => (
          <Fragment key={video.id}>
          {index === shelfAt && shortsShelf}
          <div
            onClick={() => handleClick(video.id)}
            onMouseEnter={() => setHoveredId(video.id)}
            onMouseLeave={() => setHoveredId(null)}
            className="flex gap-4 hover:bg-gray-100 p-2 rounded-lg transition-colors cursor-pointer"
          >
            {/* Thumbnail */}
            <div className="relative flex-shrink-0">
              <div className="relative w-80 h-48 bg-gray-300 rounded-lg overflow-hidden">
                <Image
                  src={video.thumbnail}
                  alt={video.title}
                  fill
                  sizes="(max-width: 640px) 100vw, 320px"
                  className="object-cover"
                />
                <div className="absolute bottom-2 right-2 bg-black bg-opacity-80 text-white text-xs px-1 py-0.5 rounded">
                  {video.duration}
                </div>
              </div>
            </div>
            {/* Info */}
            <div className="flex-1 min-w-0">
              <h3
                className={`text-lg font-medium line-clamp-2 mb-1 transition-colors ${
                  hoveredId === video.id ? "text-red-600" : "text-black"
                }`}
              >
                {video.title}
              </h3>
              <div className="text-gray-600 text-sm mb-2">
                <span>{video.views}</span>
                <span className="mx-1">•</span>
                <span>{video.uploadTime}</span>
              </div>
              {/* Channel Info */}
              <div className="flex items-center gap-2 mb-2">
                <div className="w-6 h-6 rounded-full overflow-hidden">
                  <Image
                    src={video.channel.picture}
                    alt={video.channel.name}
                    width={24}
                    height={24}
                    className="object-cover"
                  />
                </div>
                <span className="text-gray-600 text-sm hover:text-black cursor-pointer">
                  {video.channel.name}
                </span>
              </div>
              {video.description && (
                <p className="text-gray-600 text-sm line-clamp-2">
                  {video.description}
                </p>
              )}
            </div>
          </div>
          </Fragment>
        ))}
        {shelfAt === videoResults.length && shortsShelf}
      </div>
      {similar.length > 0 && (
        <section className="mt-8 border-t pt-4">
          <h2 className="mb-3 text-xl font-bold">Video tương tự</h2>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {similar.map((v) => (
              <div
                key={v.id}
                onClick={() => router.push(v.isShort ? `/shorts/${v.id}` : `/watch/${v.id}`)}
                className="cursor-pointer"
              >
                <div className="relative aspect-video overflow-hidden rounded-lg bg-gray-300">
                  <Image src={v.thumbnail} alt={v.title} fill sizes="(max-width: 640px) 100vw, 320px" className="object-cover" />
                  <div className="absolute bottom-2 right-2 rounded bg-black bg-opacity-80 px-1 py-0.5 text-xs text-white">
                    {v.duration}
                  </div>
                </div>
                <h3 className="mt-2 line-clamp-2 font-medium">{v.title}</h3>
                <p className="text-sm text-gray-600">
                  {v.channel.name} • {v.views}
                </p>
              </div>
            ))}
          </div>
        </section>
      )}
    </main>
  );
}

export default function SearchResults() {
  return (
    <div className="min-h-screen bg-gray-50 text-black">
      <Suspense
        fallback={
          <div className="container mx-auto px-4 py-6">
            <ResultsSkeleton />
          </div>
        }
      >
        <SearchResultsContent />
      </Suspense>
    </div>
  );
}
