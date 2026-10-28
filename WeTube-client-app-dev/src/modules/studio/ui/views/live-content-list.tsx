"use client";

import { type ReactNode, useEffect, useMemo, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import {
  ArrowDown,
  ArrowUp,
  ChevronLeft,
  ChevronRight,
  ExternalLink,
  Link as LinkIcon,
  MoreVertical,
  Play,
  Search,
  Trash2,
  X,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { useCopyToClipboard } from "@/lib/clipboard";
import { toastEmitter } from "@/lib/toastEmitter";
import { cn } from "@/lib/utils";
import { formatLiveDuration, liveUrl } from "@/modules/live/lib/format";
import type { MyPastLive } from "@/modules/live/types";
import { usePastLives } from "../../hooks/use-past-lives";
import { DeleteLivesDialog } from "../components/delete-lives-dialog";
import { ReplayStatus, liveCover, waitingForUpload } from "../components/live-content-card";

const PAGE_SIZE = 30;

const checkboxClass = "size-4 cursor-pointer accent-black";
const rowActionClass = "flex size-8 items-center justify-center rounded-full text-gray-700 hover:bg-gray-200";

/** Tab Trực tiếp của "Xem tất cả" ở trang Nội dung: bảng các buổi đã phát như bảng Video / Shorts, chọn nhiều để xoá. */
export default function LiveContentList() {
  const [, copy] = useCopyToClipboard();
  const { lives, isLoading, error, reload } = usePastLives();
  const [query, setQuery] = useState("");
  const [newestFirst, setNewestFirst] = useState(true);
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [toDelete, setToDelete] = useState<MyPastLive[] | null>(null);

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase();
    const time = (l: MyPastLive) => new Date(l.endedAt ?? l.startedAt ?? 0).getTime();
    return lives
      .filter((l) => !q || l.title.toLowerCase().includes(q))
      .sort((a, b) => (newestFirst ? time(b) - time(a) : time(a) - time(b)));
  }, [lives, query, newestFirst]);

  const pageCount = Math.max(1, Math.ceil(rows.length / PAGE_SIZE));
  const currentPage = Math.min(page, pageCount - 1);
  const pageRows = rows.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE);
  const selectable = pageRows.filter((l) => !waitingForUpload(l));
  const allOnPageSelected = selectable.length > 0 && selectable.every((l) => selected.has(l.id));

  // Buổi đã biến mất (xoá xong, hoặc xoá ở tab khác) thì bỏ khỏi phần đã chọn
  useEffect(() => {
    setSelected((prev) => {
      const next = new Set([...prev].filter((id) => lives.some((l) => l.id === id && !waitingForUpload(l))));
      return next.size === prev.size ? prev : next;
    });
  }, [lives]);

  const toggleRow = (id: string) =>
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  const togglePage = () =>
    setSelected((prev) => {
      const next = new Set(prev);
      for (const l of selectable) {
        if (allOnPageSelected) next.delete(l.id);
        else next.add(l.id);
      }
      return next;
    });

  const copyLink = async (id: string) => {
    if (await copy(liveUrl(id))) toastEmitter.success("Đã sao chép liên kết");
    else toastEmitter.error("Không sao chép được, hãy chép thủ công");
  };

  return (
    <>
      {/* Hàng lọc; khi đã chọn buổi thì thành hàng thao tác hàng loạt */}
      <div className="flex min-h-14 items-center gap-3 border-y px-6">
        {selected.size > 0 ? (
          <>
            <span className="text-sm font-medium">Đã chọn {selected.size}</span>
            <Button
              variant="outline"
              size="sm"
              className="rounded-full"
              onClick={() => setToDelete(lives.filter((l) => selected.has(l.id)))}
            >
              <Trash2 className="mr-1 size-4" /> Xoá
            </Button>
            <button
              type="button"
              aria-label="Bỏ chọn"
              onClick={() => setSelected(new Set())}
              className="ml-auto rounded-full p-2 hover:bg-gray-100"
            >
              <X className="size-5" />
            </button>
          </>
        ) : (
          <>
            <Search className="size-5 text-gray-500" />
            <input
              value={query}
              onChange={(e) => {
                setQuery(e.target.value);
                setPage(0);
              }}
              placeholder="Lọc theo tiêu đề"
              className="flex-1 bg-transparent py-3 text-sm outline-none"
            />
            {query && (
              <button type="button" aria-label="Xoá bộ lọc" onClick={() => setQuery("")} className="rounded-full p-1 hover:bg-gray-100">
                <X className="size-4" />
              </button>
            )}
          </>
        )}
      </div>

      {isLoading ? (
        <TableSkeleton />
      ) : error ? (
        <p className="p-6 text-center text-red-500">Không tải được các buổi phát trực tiếp. Vui lòng thử lại sau.</p>
      ) : rows.length === 0 ? (
        <p className="p-10 text-center text-gray-500">
          {query ? "Không có buổi phát nào khớp." : "Chưa có buổi phát trực tiếp nào đã kết thúc."}
        </p>
      ) : (
        <>
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-12 pl-6">
                    <input
                      type="checkbox"
                      aria-label="Chọn tất cả trên trang này"
                      checked={allOnPageSelected}
                      onChange={togglePage}
                      className={checkboxClass}
                    />
                  </TableHead>
                  <TableHead className="min-w-[360px]">Buổi phát trực tiếp</TableHead>
                  <TableHead>Video xem lại</TableHead>
                  <TableHead>
                    <button
                      type="button"
                      onClick={() => setNewestFirst((n) => !n)}
                      className="flex items-center gap-1 font-semibold text-black"
                    >
                      Ngày {newestFirst ? <ArrowDown className="size-4" /> : <ArrowUp className="size-4" />}
                    </button>
                  </TableHead>
                  <TableHead className="pr-6 text-right">Người xem cao nhất</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {pageRows.map((live) => {
                  const duration = formatLiveDuration(live.startedAt, live.endedAt);
                  const checked = selected.has(live.id);
                  const waiting = waitingForUpload(live);
                  return (
                    <TableRow key={live.id} data-state={checked ? "selected" : undefined} className="group">
                      <TableCell className="pl-6 align-top">
                        <input
                          type="checkbox"
                          aria-label={`Chọn ${live.title}`}
                          checked={checked}
                          disabled={waiting}
                          onChange={() => toggleRow(live.id)}
                          className={cn(checkboxClass, "mt-1")}
                        />
                      </TableCell>
                      <TableCell className="py-3">
                        <div className="flex gap-3">
                          <div className="relative h-[72px] aspect-video shrink-0 overflow-hidden rounded-md bg-neutral-900">
                            {liveCover(live) && (
                              <Image src={liveCover(live)} alt={live.title} fill sizes="128px" className="object-cover" />
                            )}
                            {duration && (
                              <span className="absolute bottom-1 right-1 rounded bg-black/80 px-1 text-[11px] font-medium text-white">
                                {duration}
                              </span>
                            )}
                          </div>
                          <div className="min-w-0 flex-1 whitespace-normal">
                            <Link href={`/live/${live.id}`} className="line-clamp-1 font-medium hover:text-blue-700" title={live.title}>
                              {live.title}
                            </Link>
                            {/* Như bảng video: rê chuột vào hàng thì dòng phụ thành các nút thao tác */}
                            <p className="mt-1 line-clamp-1 text-xs text-gray-500 group-hover:hidden group-focus-within:hidden">
                              {live.recordingMessage ?? (duration ? `Thời lượng ${duration}` : "Đã phát")}
                            </p>
                            <div className="mt-0.5 hidden items-center gap-1 group-hover:flex group-focus-within:flex">
                              <Link href={`/live/${live.id}`} target="_blank" title="Xem trang buổi phát" className={rowActionClass}>
                                <ExternalLink className="size-4" />
                                <span className="sr-only">Xem trang buổi phát</span>
                              </Link>
                              {live.replayVideoId && (
                                <Link href={`/watch/${live.replayVideoId}`} target="_blank" title="Xem video xem lại" className={rowActionClass}>
                                  <Play className="size-4" />
                                  <span className="sr-only">Xem video xem lại</span>
                                </Link>
                              )}
                              <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                  <button type="button" title="Tuỳ chọn" className={rowActionClass}>
                                    <MoreVertical className="size-4" />
                                    <span className="sr-only">Tuỳ chọn</span>
                                  </button>
                                </DropdownMenuTrigger>
                                <DropdownMenuContent align="start">
                                  <DropdownMenuItem onSelect={() => copyLink(live.id)}>
                                    <LinkIcon className="size-4" /> Sao chép đường liên kết
                                  </DropdownMenuItem>
                                  <DropdownMenuSeparator />
                                  <DropdownMenuItem
                                    onSelect={() => setToDelete([live])}
                                    disabled={waiting}
                                    className="text-red-600 focus:text-red-600"
                                  >
                                    <Trash2 className="size-4" /> Xoá vĩnh viễn
                                  </DropdownMenuItem>
                                </DropdownMenuContent>
                              </DropdownMenu>
                            </div>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell className="align-top text-sm">
                        <ReplayStatus live={live} />
                      </TableCell>
                      <TableCell className="align-top text-sm">
                        {live.endedAt ? new Date(live.endedAt).toLocaleDateString("vi-VN") : "—"}
                        <p className="text-xs text-gray-500">Ngày phát</p>
                      </TableCell>
                      <TableCell className="pr-6 text-right align-top text-sm">
                        {live.peakViewers.toLocaleString("vi-VN")}
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </div>

          <div className="flex items-center justify-end gap-2 border-t px-6 py-3 text-sm text-gray-600">
            <span>
              {currentPage * PAGE_SIZE + 1}–{Math.min((currentPage + 1) * PAGE_SIZE, rows.length)} trong tổng số{" "}
              {rows.length}
            </span>
            <Button
              variant="ghost"
              size="icon"
              className="rounded-full"
              aria-label="Trang trước"
              disabled={currentPage === 0}
              onClick={() => setPage(currentPage - 1)}
            >
              <ChevronLeft className="size-5" />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              className="rounded-full"
              aria-label="Trang sau"
              disabled={currentPage >= pageCount - 1}
              onClick={() => setPage(currentPage + 1)}
            >
              <ChevronRight className="size-5" />
            </Button>
          </div>
        </>
      )}

      <DeleteLivesDialog lives={toDelete} onClose={() => setToDelete(null)} onDeleted={reload} />
    </>
  );
}

function TableSkeleton(): ReactNode {
  return (
    <div className="space-y-3 px-6 py-4">
      {Array.from({ length: 4 }).map((_, i) => (
        <div key={i} className="flex items-center gap-4">
          <Skeleton className="size-4" />
          <Skeleton className="h-[72px] aspect-video rounded-md" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-4 w-1/2" />
            <Skeleton className="h-3 w-1/3" />
          </div>
        </div>
      ))}
    </div>
  );
}
