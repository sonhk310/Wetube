"use client";

import { useSearchSuggestQuery } from "@/app/api/searchApi";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { recordSearch, useSearchHistory } from "@/lib/searchHistory";
import { useAuth } from "@/contexts/auth-context";
import { History, SearchIcon, XIcon } from "lucide-react";
import { useSearchParams } from "next/navigation";
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

  const query = searchParams.get("query") || "";
  const categoryId = searchParams.get("categoryId") || "";

  const [value, setValue] = useState(query);
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const { user } = useAuth();
  const loggedIn = !!user?.sub;

  // Lịch sử tìm kiếm (trang chủ): ô trống thì hiện các từ khoá gần nhất, đang gõ thì vài từ khoá khớp
  const { items: history, remove: removeHistory } = useSearchHistory(
    value,
    loggedIn,
    isOpen
  );

  // ✅ gọi suggest; chỉ skip khi rỗng hoàn toàn
  const { data: suggestions = [], isFetching } = useSearchSuggestQuery(
    { prefix: value },
    { skip: value.trim().length === 0 }
  );

  const handleSearch = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const newQuery = value.trim();
    if (!newQuery) return;
    const url = new URL("/search", window.location.origin);
    url.searchParams.set("query", newQuery);
    if (categoryId) url.searchParams.set("categoryId", categoryId);
    setValue(newQuery);
    recordSearch(newQuery, loggedIn);
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
  const showDropdown = isOpen && (value.length > 0 || history.length > 0);

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
          {history.map((text) => (
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

          {value.length > 0 && !isFetching && (
            <>
              {otherSuggestions.length > 0 ? (
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
                )}
            </>
          )}

        </div>
      )}
    </div>
  );
};

export default SearchInput;
