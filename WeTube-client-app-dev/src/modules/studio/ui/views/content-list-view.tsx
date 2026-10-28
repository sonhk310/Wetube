"use client";

import { type ReactNode, useEffect, useMemo, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useDispatch } from "react-redux";
import {
  ArrowDown,
  ArrowLeft,
  ArrowUp,
  Ban,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Download,
  ExternalLink,
  Link as LinkIcon,
  Loader2,
  MoreVertical,
  Pencil,
  Search,
  Trash2,
  X,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { playlistApi } from "@/app/api/playlistApi";
import { useDeleteVideoMutation, useUpdateVisibilityMutation } from "@/app/api/videoApi";
import { apiErrorMessage } from "@/lib/apiError";
import { useCopyToClipboard } from "@/lib/clipboard";
import { toastEmitter } from "@/lib/toastEmitter";
import { cn, formatDuration } from "@/lib/utils";
import { videoDownloadUrl, videoImageUrl } from "@/lib/videoMedia";
import type { VideoFromPlaylist } from "@/types/playlistSummary";
import { useUploadedVideos } from "../../hooks/use-uploaded-videos";
import { STATUS } from "../components/content-card";
import { DeleteVideoDialog } from "../components/delete-video-dialog";
import { DraftEditDialog } from "../components/draft-edit-dialog";
import LiveContentList from "./live-content-list";

const PAGE_SIZE = 30;

const VISIBILITY_ACTIONS: { value: "PUBLIC" | "UNLISTED" | "PRIVATE"; label: string }[] = [
  { value: "PUBLIC", label: "Công khai" },
  { value: "UNLISTED", label: "Không công khai" },
  { value: "PRIVATE", label: "Riêng tư" },
];

const TABS = [
  { value: "video", label: "Video" },
  { value: "shorts", label: "Shorts" },
  { value: "live", label: "Trực tiếp" },
] as const;

type ContentTab = (typeof TABS)[number]["value"];

const checkboxClass = "size-4 cursor-pointer accent-black";

const TAB_TITLES: Record<ContentTab, string> = { video: "Video", shorts: "Shorts", live: "Trực tiếp" };

/** "Xem tất cả" của trang Nội dung: tiêu đề, các tab và bảng của tab đang chọn (video, Shorts hoặc buổi phát trực tiếp). */
export default function ContentListView() {
  const searchParams = useSearchParams();
  const param = searchParams.get("tab");
  const tab: ContentTab = param === "shorts" ? "shorts" : param === "live" ? "live" : "video";

  return (
    <div className="flex flex-col pt-2.5">
      <div className="flex items-center gap-3 px-4">
        <Link href="/studio" aria-label="Quay lại Nội dung kênh" className="rounded-full p-2 hover:bg-gray-100">
          <ArrowLeft className="size-6" />
        </Link>
        <h1 className="text-2xl font-bold">{TAB_TITLES[tab]}</h1>
      </div>

      <div className="flex gap-2 px-6 pb-3 pt-4">
        {TABS.map((t) => (
          <Link
            key={t.value}
            href={`/studio/content?tab=${t.value}`}
            replace
            className={cn(
              "rounded-lg px-3 py-1.5 text-sm font-medium",
              tab === t.value ? "bg-black text-white" : "bg-gray-100 hover:bg-gray-200"
            )}
          >
            {t.label}
          </Link>
        ))}
      </div>

      {tab === "live" ? <LiveContentList /> : <VideoContentList tab={tab} />}
    </div>
  );
}

/** Bảng video / Shorts như YouTube Studio, chọn nhiều để đổi chế độ hoặc xoá. */
function VideoContentList({ tab }: { tab: "video" | "shorts" }) {
  const isShortsTab = tab === "shorts";
  const dispatch = useDispatch();
  const [, copy] = useCopyToClipboard();
  const { videos, isLoading, error } = useUploadedVideos();
  const [deleteVideo, { isLoading: deletingOne }] = useDeleteVideoMutation();
  const [updateVisibility, { isLoading: updatingVisibility }] = useUpdateVisibilityMutation();

  const [query, setQuery] = useState("");
  const [newestFirst, setNewestFirst] = useState(true);
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [toDelete, setToDelete] = useState<VideoFromPlaylist | null>(null);
  const [bulkDeleteOpen, setBulkDeleteOpen] = useState(false);
  const [bulkConfirmed, setBulkConfirmed] = useState(false);
  const [bulkDeleting, setBulkDeleting] = useState(false);
  const [draftId, setDraftId] = useState<string | null>(null);

  // Đổi tab: bỏ chọn, về trang đầu
  useEffect(() => {
    setSelected(new Set());
    setPage(0);
  }, [tab]);

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase();
    return videos
      .filter((v) => !!v.isShort === isShortsTab)
      .filter((v) => !q || v.videoTitle.toLowerCase().includes(q))
      .sort((a, b) => {
        const diff = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
        return newestFirst ? -diff : diff;
      });
  }, [videos, isShortsTab, query, newestFirst]);

  const pageCount = Math.max(1, Math.ceil(rows.length / PAGE_SIZE));
  const currentPage = Math.min(page, pageCount - 1);
  const pageRows = rows.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE);
  const allOnPageSelected = pageRows.length > 0 && pageRows.every((v) => selected.has(v.videoId));

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
      for (const v of pageRows) {
        if (allOnPageSelected) next.delete(v.videoId);
        else next.add(v.videoId);
      }
      return next;
    });

  const refreshList = () => dispatch(playlistApi.util.invalidateTags(["Playlist"]));
  const watchHref = (id: string) => `/${isShortsTab ? "shorts" : "watch"}/${id}`;

  const changeVisibility = async (status: "PUBLIC" | "UNLISTED" | "PRIVATE") => {
    const ids = [...selected];
    try {
      const { updated } = await updateVisibility({ videoIds: ids, status }).unwrap();
      const skipped = ids.length - updated;
      toastEmitter.success(
        `Đã đổi chế độ hiển thị của ${updated} video` +
          (skipped > 0 ? ` (bỏ qua ${skipped} video là bản nháp, bị ẩn hoặc đã đúng chế độ)` : "")
      );
      setSelected(new Set());
      refreshList();
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không đổi được chế độ hiển thị, vui lòng thử lại."));
    }
  };

  const deleteSelected = async () => {
    const ids = [...selected];
    setBulkDeleting(true);
    const results = await Promise.allSettled(ids.map((id) => deleteVideo(id).unwrap()));
    const failed = results.filter((r) => r.status === "rejected").length;
    setBulkDeleting(false);
    setBulkDeleteOpen(false);
    setBulkConfirmed(false);
    setSelected(new Set());
    refreshList();
    if (failed) toastEmitter.error(`Đã xoá ${ids.length - failed} video, ${failed} video không xoá được.`);
    else toastEmitter.success(`Đã xoá ${ids.length} video`);
  };

  const deleteOne = async () => {
    if (!toDelete) return;
    try {
      await deleteVideo(toDelete.videoId).unwrap();
      toastEmitter.success(toDelete.videoStatus === "DRAFT" ? "Đã xoá video nháp" : "Đã xoá video");
      setSelected((prev) => {
        const next = new Set(prev);
        next.delete(toDelete.videoId);
        return next;
      });
      setToDelete(null);
      refreshList();
    } catch {
      toastEmitter.error("Không xoá được video");
    }
  };

  const copyLink = async (id: string) => {
    if (await copy(`${window.location.origin}${watchHref(id)}`)) toastEmitter.success("Đã sao chép liên kết");
    else toastEmitter.error("Không sao chép được, hãy chép thủ công");
  };

  return (
    <div className="flex flex-col">
      {/* Hàng lọc; khi đã chọn video thì thành hàng thao tác hàng loạt */}
      <div className="flex min-h-14 items-center gap-3 border-y px-6">
        {selected.size > 0 ? (
          <>
            <span className="text-sm font-medium">Đã chọn {selected.size}</span>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="outline" size="sm" className="rounded-full" disabled={updatingVisibility}>
                  {updatingVisibility && <Loader2 className="mr-1 size-4 animate-spin" />}
                  Chế độ hiển thị <ChevronDown className="ml-1 size-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="start">
                {VISIBILITY_ACTIONS.map((a) => (
                  <DropdownMenuItem key={a.value} onSelect={() => changeVisibility(a.value)}>
                    {a.label}
                  </DropdownMenuItem>
                ))}
              </DropdownMenuContent>
            </DropdownMenu>
            <Button variant="outline" size="sm" className="rounded-full" onClick={() => setBulkDeleteOpen(true)}>
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
        <p className="p-6 text-center text-red-500">Không tải được danh sách video. Vui lòng thử lại sau.</p>
      ) : rows.length === 0 ? (
        <p className="p-10 text-center text-gray-500">
          {query ? "Không có video nào khớp." : isShortsTab ? "Chưa có Shorts nào." : "Chưa có video nào."}
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
                  <TableHead className="min-w-[360px]">{isShortsTab ? "Shorts" : "Video"}</TableHead>
                  <TableHead>Thông báo</TableHead>
                  <TableHead>Chế độ hiển thị</TableHead>
                  <TableHead>
                    <button
                      type="button"
                      onClick={() => setNewestFirst((n) => !n)}
                      className="flex items-center gap-1 font-semibold text-black"
                    >
                      Ngày {newestFirst ? <ArrowDown className="size-4" /> : <ArrowUp className="size-4" />}
                    </button>
                  </TableHead>
                  <TableHead className="text-right">Lượt xem</TableHead>
                  <TableHead className="pr-6 text-right">Số bình luận</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {pageRows.map((video) => {
                  const isDraft = video.videoStatus === "DRAFT";
                  const status = STATUS[video.videoStatus ?? "ACTIVE"] ?? STATUS.ACTIVE;
                  const StatusIcon = status.icon;
                  const image = videoImageUrl(video.thumbnailUrl, video.videoUrl);
                  const checked = selected.has(video.videoId);
                  return (
                    <TableRow key={video.videoId} data-state={checked ? "selected" : undefined} className="group">
                      <TableCell className="pl-6 align-top">
                        <input
                          type="checkbox"
                          aria-label={`Chọn ${video.videoTitle}`}
                          checked={checked}
                          onChange={() => toggleRow(video.videoId)}
                          className={cn(checkboxClass, "mt-1")}
                        />
                      </TableCell>
                      <TableCell className="py-3">
                        <div className="flex gap-3">
                          <div className="relative h-[72px] aspect-video shrink-0 overflow-hidden rounded-md bg-gray-200">
                            {image && isShortsTab ? (
                              // Shorts: cùng khung 16:9 với video, ảnh dọc ở giữa, hai bên là chính ảnh đó làm mờ
                              <>
                                <Image src={image} alt="" aria-hidden fill sizes="128px" className="scale-125 object-cover blur-md" />
                                <Image src={image} alt={video.videoTitle} fill sizes="128px" className="object-contain" />
                              </>
                            ) : (
                              image && <Image src={image} alt={video.videoTitle} fill sizes="128px" className="object-cover" />
                            )}
                            {isDraft && <div className="absolute inset-0 bg-black/60" />}
                            {video.duration > 0 && (
                              <span className="absolute bottom-1 right-1 rounded bg-black/80 px-1 text-[11px] font-medium text-white">
                                {formatDuration(video.duration)}
                              </span>
                            )}
                          </div>
                          <div className="min-w-0 flex-1 whitespace-normal">
                            <p className="line-clamp-1 font-medium" title={video.videoTitle}>
                              {video.videoTitle}
                            </p>
                            {/* Như YouTube: rê chuột vào hàng thì dòng mô tả thành các nút thao tác */}
                            <p className="mt-1 line-clamp-1 text-xs text-gray-500 group-hover:hidden group-focus-within:hidden">
                              {video.description || "Thêm nội dung mô tả"}
                            </p>
                            <div className="mt-0.5 hidden items-center gap-1 group-hover:flex group-focus-within:flex">
                              {isDraft ? (
                                <RowAction label="Chỉnh sửa bản nháp" onClick={() => setDraftId(video.videoId)}>
                                  <Pencil className="size-4" />
                                </RowAction>
                              ) : (
                                <>
                                  <Link href={`/studio/videos/${video.videoId}`} title="Chi tiết" className={rowActionClass}>
                                    <Pencil className="size-4" />
                                    <span className="sr-only">Chi tiết</span>
                                  </Link>
                                  <Link
                                    href={watchHref(video.videoId)}
                                    target="_blank"
                                    title="Xem trên WeTube"
                                    className={rowActionClass}
                                  >
                                    <ExternalLink className="size-4" />
                                    <span className="sr-only">Xem trên WeTube</span>
                                  </Link>
                                </>
                              )}
                              <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                  <button type="button" title="Tuỳ chọn" className={rowActionClass}>
                                    <MoreVertical className="size-4" />
                                    <span className="sr-only">Tuỳ chọn</span>
                                  </button>
                                </DropdownMenuTrigger>
                                <DropdownMenuContent align="start">
                                  {!isDraft && (
                                    <DropdownMenuItem onSelect={() => copyLink(video.videoId)}>
                                      <LinkIcon className="size-4" /> Sao chép đường liên kết
                                    </DropdownMenuItem>
                                  )}
                                  <DropdownMenuItem asChild>
                                    <a href={videoDownloadUrl(video.videoUrl)} target="_blank" rel="noreferrer">
                                      <Download className="size-4" /> Tải video xuống
                                    </a>
                                  </DropdownMenuItem>
                                  <DropdownMenuSeparator />
                                  <DropdownMenuItem
                                    onSelect={() => setToDelete(video)}
                                    className="text-red-600 focus:text-red-600"
                                  >
                                    <Trash2 className="size-4" /> {isDraft ? "Xoá video nháp" : "Xoá vĩnh viễn"}
                                  </DropdownMenuItem>
                                </DropdownMenuContent>
                              </DropdownMenu>
                            </div>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell className="align-top text-sm text-gray-600">
                        {video.videoStatus === "INACTIVE" ? (
                          <span className="flex items-center gap-1.5 text-red-600">
                            <Ban className="size-4" /> Bị quản trị viên ẩn
                          </span>
                        ) : (
                          "—"
                        )}
                      </TableCell>
                      <TableCell className={cn("align-top text-sm", status.className)}>
                        <span className="flex items-center gap-1.5">
                          <StatusIcon className="size-4 shrink-0" /> {status.label}
                        </span>
                      </TableCell>
                      {isDraft ? (
                        <TableCell colSpan={3} className="pr-6 text-right align-top">
                          <Button
                            variant="secondary"
                            className="rounded-full"
                            onClick={() => setDraftId(video.videoId)}
                          >
                            Chỉnh sửa bản nháp
                          </Button>
                        </TableCell>
                      ) : (
                        <>
                          <TableCell className="align-top text-sm">
                            {new Date(video.createdAt).toLocaleDateString("vi-VN")}
                            <p className="text-xs text-gray-500">Ngày đăng</p>
                          </TableCell>
                          <TableCell className="text-right align-top text-sm">{video.totalView}</TableCell>
                          <TableCell className="pr-6 text-right align-top text-sm">{video.commentCount ?? 0}</TableCell>
                        </>
                      )}
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

      <DeleteVideoDialog video={toDelete} deleting={deletingOne} onCancel={() => setToDelete(null)} onConfirm={deleteOne} />

      <Dialog
        open={bulkDeleteOpen}
        onOpenChange={(open) => {
          if (bulkDeleting) return;
          setBulkDeleteOpen(open);
          if (!open) setBulkConfirmed(false);
        }}
      >
        <DialogContent className="grid-cols-[minmax(0,1fr)] sm:max-w-md rounded-2xl">
          <DialogHeader>
            <DialogTitle className="text-xl">Xoá vĩnh viễn {selected.size} video?</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-gray-600">
            Video, lượt xem và bình luận sẽ bị xoá vĩnh viễn và không khôi phục được.
          </p>
          <label className="flex cursor-pointer items-start gap-3 text-sm">
            <input
              type="checkbox"
              checked={bulkConfirmed}
              onChange={(e) => setBulkConfirmed(e.target.checked)}
              className={cn(checkboxClass, "mt-0.5")}
            />
            Tôi hiểu rằng các video này sẽ bị xoá vĩnh viễn
          </label>
          <DialogFooter>
            <Button variant="ghost" className="rounded-full" onClick={() => setBulkDeleteOpen(false)} disabled={bulkDeleting}>
              Huỷ
            </Button>
            <Button
              variant="destructive"
              className="rounded-full"
              onClick={deleteSelected}
              disabled={!bulkConfirmed || bulkDeleting}
            >
              {bulkDeleting && <Loader2 className="mr-2 size-4 animate-spin" />}
              Xoá vĩnh viễn
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {draftId && (
        <DraftEditDialog videoId={draftId} open onOpenChange={(open) => !open && setDraftId(null)} />
      )}
    </div>
  );
}

const rowActionClass = "flex size-8 items-center justify-center rounded-full text-gray-700 hover:bg-gray-200";

function RowAction({ label, onClick, children }: { label: string; onClick: () => void; children: ReactNode }) {
  return (
    <button type="button" title={label} onClick={onClick} className={rowActionClass}>
      {children}
      <span className="sr-only">{label}</span>
    </button>
  );
}

function TableSkeleton() {
  return (
    <div className="space-y-3 px-6 py-4">
      {Array.from({ length: 6 }).map((_, i) => (
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
