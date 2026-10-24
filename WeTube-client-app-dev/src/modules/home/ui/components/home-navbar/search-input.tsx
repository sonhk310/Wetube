"use client";

import { useSearchSuggestQuery, useSearchVideosFullQuery } from "@/app/api/searchApi";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { toastEmitter } from "@/lib/toastEmitter";
import { recordSearch, useSearchHistory } from "@/lib/searchHistory";
import { useAuth } from "@/contexts/auth-context";
import { useRoomStore } from "@/store/zustand/useRoomStore";
import { useStompStore } from "@/store/zustand/useStompStore";
import { History, SearchIcon, SquarePlus, XIcon } from "lucide-react";
import Image from 'next/image';
import { usePathname, useSearchParams } from "next/navigation";
import { useProgressRouter } from "@/hooks/use-progress-router";
import { Suspense, useEffect, useRef, useState } from "react";

export const SearchInput = () => (
  <Suspense fallback={<Skeleton className="h-10 w-full" />}>
    <SearchInputSuspense />
  </Suspense>
);

const SearchInputSuspense = () => {
  const router = useProgressRouter();
  const searchParams = useSearchParams();
  const pathname = usePathname();
  const mode: "home" | "rooms" = pathname.includes("rooms") ? "rooms" : "home";

  const query = searchParams.get("query") || "";
  const categoryId = searchParams.get("categoryId") || "";

  const [value, setValue] = useState(query);
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const { publish } = useStompStore();
  const { room } = useRoomStore();
  const { user } = useAuth();
  const loggedIn = !!user?.sub;

  // Lịch sử tìm kiếm (trang chủ): ô trống thì hiện các từ khoá gần nhất, đang gõ thì vài từ khoá khớp
  const { items: history, remove: removeHistory } = useSearchHistory(
    value,
    loggedIn,
    mode === "home" && isOpen
  );

  // ✅ gọi suggest; chỉ skip khi rỗng hoàn toàn
  const {
    data: suggestions = [],
    isFetching: isSuggestFetching,
    isError,
    error,
  } = useSearchSuggestQuery(
    { prefix: value },
    { skip: mode !== "home" || value.trim().length === 0 }
  );

  const { data: fullSearchData, isFetching: isFullSearchFetching } =
    useSearchVideosFullQuery(
      { query: value },
      { skip: mode !== "rooms" || value.trim().length === 0 }
    );
  
  const roomResults = fullSearchData?.content ?? [];
  const isFetching = isSuggestFetching || isFullSearchFetching;
  
  const handleSearch = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const newQuery = value.trim();
    if (!newQuery) return;
    const url = new URL("/search", window.location.origin);
    url.searchParams.set("query", newQuery);
    if (categoryId) url.searchParams.set("categoryId", categoryId);
    setValue(newQuery);
    if (mode === "home") recordSearch(newQuery, loggedIn);
    router.push(url.toString());
    setIsOpen(false);
  };

  // Bấm một gợi ý / mục lịch sử
  const searchFor = (text: string) => {
    setValue(text);
    recordSearch(text, loggedIn);
    router.push(`/search?query=${encodeURIComponent(text)}`);
    setIsOpen(false);
  };

  const historyLower = new Set(history.map((h) => h.toLocaleLowerCase("vi")));
  const otherSuggestions = suggestions.filter((t) => !historyLower.has(t.toLocaleLowerCase("vi")));
  const showDropdown = isOpen && (value.length > 0 || (mode === "home" && history.length > 0));

  // đóng dropdown khi click ngoài
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <div
      ref={containerRef}
      className="relative w-full max-w-[600px]"
    >
      {/* Input */}
      <form
        className="flex w-full"
        onSubmit={handleSearch}
      >
        <div className="relative w-full">
          <input
            value={value}
            onChange={(e) => {
              setValue(e.target.value);
              setIsOpen(true);
            }}
            onFocus={() => setIsOpen(true)}
            type="text"
            placeholder="Tìm kiếm"
            className="w-full pl-4 py-2 pr-12 rounded-l-full border focus:outline-none focus:border-blue-500"
            autoComplete="off"
          />
          {value && (
            <Button
              variant="ghost"
              type="button"
              size="icon"
              onClick={() => setValue("")}
              className="absolute right-2 top-1/2 -translate-y-1/2 rounded-full"
            >
              <XIcon className="text-gray-500" />
            </Button>
          )}
        </div>
        <button
          disabled={!value.trim()}
          type="submit"
          className="px-5 py-2.5 bg-gray-100 border border-l-0 rounded-r-full hover:bg-gray-200 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <SearchIcon className="size-5" />
        </button>
      </form>

      {/* Dropdown */}
      {showDropdown && (
        <div className="absolute z-[9999] mt-1 w-full bg-white border rounded-xl shadow-lg py-2 max-h-96 overflow-y-auto">
          {mode === "home" &&
            history.map((text) => (
              <div key={`h-${text}`} className="group flex items-center hover:bg-gray-100">
                <button
                  type="button"
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => searchFor(text)}
                  className="flex min-w-0 flex-1 items-center gap-3 px-4 py-1.5 text-left font-semibold text-purple-800"
                >
                  <History className="size-4 shrink-0 text-gray-500" />
                  <span className="truncate">{text}</span>
                </button>
                <button
                  type="button"
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => removeHistory(text)}
                  className="mr-2 shrink-0 rounded-full px-2 py-1 text-xs text-blue-600 hover:underline"
                >
                  Xoá
                </button>
              </div>
            ))}

          {value.length > 0 && isFetching && (
            <div className="p-2 text-sm text-gray-500">Đang tìm...</div>
          )}

          {/* {!isFetching && isError && (
            <div className="p-2 text-sm text-red-500">
              Suggest error: {String((error as any)?.status || "")}
            </div>
          )}

          {!isFetching && !isError && suggestions.length === 0 && (
            <div className="p-2 text-sm text-gray-400">Không có gợi ý</div>
          )}

          {!isFetching &&
            !isError &&
            suggestions.length > 0 &&
            suggestions.map((text, i) => (
              <button
                key={`${text}-${i}`}
                type="button"
                onMouseDown={(e) => e.preventDefault()} // giữ dropdown khi click
                onClick={() => {
                  if (mode === "rooms") {
                    publish(`/app/room/addSong/${room?.roomId}`, {
                      videoTitle: text,
                    });
                    toastEmitter.success("Đã thêm video!");
                  } else {
                    router.push(`/search?query=${encodeURIComponent(text)}`);
                  }
                  setIsOpen(false);
                }}
                className="w-full px-3 py-2 text-left hover:bg-gray-100"
              >
                {text}
              </button>
            ))} */}

          {value.length > 0 && !isFetching && (
            <>
              {/* Giao diện cho chế độ "home" */}
              {mode === "home" &&
                (otherSuggestions.length > 0 ? (
                  otherSuggestions.map((text, i) => (
                    <button
                      key={`${text}-${i}`}
                      type="button"
                      onClick={() => searchFor(text)}
                      className="flex w-full items-center gap-3 px-4 py-1.5 text-left hover:bg-gray-100"
                    >
                      <SearchIcon className="size-4 shrink-0 text-gray-500" />
                      <span className="truncate">{text}</span>
                    </button>
                  ))
                ) : (
                  history.length === 0 && (
                    <div className="px-4 py-2 text-sm text-gray-400">
                      Không có gợi ý
                    </div>
                  )
                ))}

              {/* Giao diện cho chế độ "rooms" */}
              {mode === "rooms" &&
                (roomResults.length > 0 ? (
                  roomResults.map((video) => (
                    <button
                      key={video.id}
                      type="button"
                      onClick={() => {
                        // ✅ BƯỚC 3: GỬI ĐÚNG DỮ LIỆU (videoId)
                        publish(`/app/room/addSong/${room?.roomId}`, {
                          videoId: video.id,
                        });
                        toastEmitter.success("Đã thêm video!");
                        setIsOpen(false);
                      }}
                      className="flex w-full items-center gap-3 p-2 hover:bg-gray-100 text-left"
                    >
                      <Image
                        src={video.thumbnailUrl}
                        alt={video.title}
                        width={100}
                        height={56}
                        className="rounded object-cover flex-shrink-0"
                      />
                      <div className="overflow-hidden">
                        <p className="font-medium truncate text-sm">
                          {video.title}
                        </p>
                        <p className="text-xs text-gray-500 truncate">
                          {video.user.name}
                        </p>
                      </div>
                      <SquarePlus className="w-6 h-6 text-gray-400 ml-auto mr-2" />
                    </button>
                  ))
                ) : (
                  <div className="p-2 text-sm text-gray-400">
                    Không tìm thấy kết quả
                  </div>
                ))}
            </>
          )}

        </div>
      )}
    </div>
  );
};

export default SearchInput;
