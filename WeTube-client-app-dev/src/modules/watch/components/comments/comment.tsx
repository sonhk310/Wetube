"use client";
import {
    ChevronDownIcon,
    ChevronUpIcon,
    EllipsisVerticalIcon,
    HandThumbUpIcon,
    HeartIcon,
} from "@heroicons/react/24/outline";
import { HeartIcon as HeartSolidIcon } from "@heroicons/react/24/solid";
import { Flag, Pencil, Pin, PinOff, Trash2 } from "lucide-react";
import Profile from "../misc/profile";
import { motion } from "motion/react";
import { useCallback, useEffect, useState } from "react";
import { useGetLikeInfoQuery, useToggleLikeMutation } from "@/app/api/likeApi";
import { useAuth } from "@/contexts/auth-context";
import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";
import { apiErrorMessage } from "@/lib/apiError";
import { toastEmitter } from "@/lib/toastEmitter";
import { timeAgo } from "@/lib/utils";
import {
    useCreateCommentMutation,
    useDeleteCommentMutation,
    useHeartCommentMutation,
    usePinCommentMutation,
    useUpdateCommentMutation,
} from "@/app/api/commentApi";
import { useVideoStore } from "@/store/zustand/videoStore";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import ReportDialog from "@/modules/reports/report-dialog";
import LinkifiedText from "../linkified-text";
import { Spinner } from "@/components/ui/spinner";
import PremiumBadge from "@/components/premium-badge";
import { confirmDialog } from "@/lib/dialogs";

export interface Comment {
    id: string;
    authorId?: string;
    commenter: string;
    /** @handle của kênh người viết (không có "@"). */
    handle?: string;
    comment: string;
    picture: string;
    commentedAt: string;
    /** Có sau khi người viết sửa bình luận. */
    editedAt?: string;
    likes: number;
    dislikes: number;
    replyCount: number;
    replies?: Comment[];
    pinned: boolean;
    hearted: boolean;
    byCreator: boolean;
    /** Người viết đang có Premium. */
    premium: boolean;
}

// Sửa sau khi đăng hơn chừng này thì hiện "(đã chỉnh sửa)"
const EDITED_THRESHOLD_MS = 2000;

// Chuyển bình luận từ API sang dạng hiển thị
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const mapApiCommentToUI = (apiComment: any): Comment => ({
    id: apiComment.id,
    authorId: apiComment.user?.id,
    commenter: apiComment.user?.name ?? "Ẩn danh",
    handle: apiComment.user?.handle ?? undefined,
    comment: apiComment.content,
    picture: apiComment.user?.picture ?? "",
    commentedAt: apiComment.createdAt,
    editedAt:
        apiComment.updatedAt &&
        new Date(apiComment.updatedAt).getTime() - new Date(apiComment.createdAt).getTime() > EDITED_THRESHOLD_MS
            ? apiComment.updatedAt
            : undefined,
    likes: apiComment.likeCount ?? 0,
    dislikes: 0,
    replyCount: apiComment.replyCount ?? 0,
    replies: apiComment.replies ?? [],
    pinned: !!apiComment.pinned,
    hearted: !!apiComment.hearted,
    byCreator: !!apiComment.byCreator,
    premium: !!apiComment.user?.premium,
});

const item = {
    hidden: { opacity: 0 },
    show: { opacity: 1 },
};

const colors = [
    "bg-red-400",
    "bg-blue-400",
    "bg-pink-400",
    "bg-emerald-400",
    "bg-green-400",
    "bg-orange-400",
];

const getRandomBackground = () => {
    const num = Math.floor(Math.random() * colors.length);
    return colors[num];
};

const showError = (err: unknown, fallback: string) =>
    toastEmitter.error(apiErrorMessage(err, fallback), { duration: 4000 });

/** Ô nhập nhanh dùng cho "Phản hồi" và "Chỉnh sửa". */
const InlineComposer = ({
    initialValue,
    placeholder,
    submitLabel,
    busy,
    onSubmit,
    onCancel,
}: {
    initialValue: string;
    placeholder: string;
    submitLabel: string;
    busy: boolean;
    onSubmit: (value: string) => void;
    onCancel: () => void;
}) => {
    const [value, setValue] = useState(initialValue);
    return (
        <div className="w-full flex flex-col">
            <input
                autoFocus
                className="w-full py-2 placeholder-gray-600 text-sm outline-0 border-b-[1px] border-gray-400 focus:border-gray-800 focus:outline-none"
                placeholder={placeholder}
                value={value}
                maxLength={2000}
                onChange={(event) => setValue(event.target.value)}
                onKeyDown={(event) => {
                    if (event.key === "Enter" && value.trim() && !busy) onSubmit(value.trim());
                    if (event.key === "Escape") onCancel();
                }}
            />
            <div className="w-full flex gap-2 justify-end items-center py-1.5">
                <button
                    onClick={onCancel}
                    className="bg-white hover:bg-gray-200 rounded-full cursor-pointer py-2 px-3"
                >
                    <p className="text-sm font-bold">Hủy</p>
                </button>
                <button
                    onClick={() => onSubmit(value.trim())}
                    disabled={!value.trim() || busy}
                    className="bg-blue-600 disabled:bg-gray-200 disabled:text-gray-400 text-white py-2 px-3 rounded-full"
                >
                    <p className="text-sm font-bold">{busy ? "Đang gửi..." : submitLabel}</p>
                </button>
            </div>
        </div>
    );
};

export const CommentComponent = ({
    comment,
    targetId,
    rootId,
    onDeleted,
}: {
    comment: Comment;
    /** Video (bình luận gốc) hoặc bình luận cha (trả lời). */
    targetId: string;
    /** Chỉ có với bình luận trả lời: id bình luận gốc mà các phản hồi mới sẽ được gắn vào. */
    rootId?: string;
    onDeleted?: (id: string) => void;
}) => {
    const [isRepliesSectionOpen, setIsRepliesSectionOpen] = useState(false);
    const [replies, setReplies] = useState<{
        isLoading: boolean;
        loaded: boolean;
        replies: Comment[];
    }>({ isLoading: false, loaded: false, replies: [] });
    const [isReplying, setIsReplying] = useState(false);
    const [isEditing, setIsEditing] = useState(false);
    const [reportOpen, setReportOpen] = useState(false);
    // Nội dung sau khi sửa, hiển thị ngay mà không chờ tải lại danh sách
    const [editedContent, setEditedContent] = useState<string | null>(null);
    // Tim vừa bấm (chưa có trong dữ liệu từ API)
    const [heartOverride, setHeartOverride] = useState<boolean | null>(null);
    const { user } = useAuth();
    const userId = user?.sub;
    const video = useVideoStore((state) => state.videoDetail?.detail);
    const videoId = video?.id;

    const { data: likeInfo } = useGetLikeInfoQuery(
        { targetId: comment.id, targetType: "COMMENT", userId: userId! },
        { skip: !userId }
    );
    const [toggleLike] = useToggleLikeMutation();
    const [deleteComment, { isLoading: isDeleting }] = useDeleteCommentMutation();
    const [createComment, { isLoading: isPosting }] = useCreateCommentMutation();
    const [updateComment, { isLoading: isUpdating }] = useUpdateCommentMutation();
    const [pinComment] = usePinCommentMutation();
    const [heartComment, { isLoading: isHearting }] = useHeartCommentMutation();
    const isOwner = !!userId && userId === comment.authorId;
    const isAdmin = !!user?.roles?.includes("ROLE_ADMIN");
    // Chủ video: ghim, thả tim, gỡ bình luận của người khác (backend cũng kiểm tra quyền)
    const isVideoOwner = !!user?.channelId && user.channelId === video?.channelId;
    const canDelete = !!userId && (isOwner || isVideoOwner || isAdmin);
    const hearted = heartOverride ?? comment.hearted;
    // Danh sách bình luận của video được làm mới theo id video
    const listTargetId = videoId ?? targetId;

    const handleDelete = async () => {
        const ok = await confirmDialog({
            title: isOwner ? "Xoá bình luận này?" : "Gỡ bình luận này khỏi video?",
            confirmLabel: isOwner ? "Xoá" : "Gỡ",
            destructive: true,
        });
        if (!ok) return;
        try {
            await deleteComment({ id: comment.id, targetId: listTargetId }).unwrap();
            onDeleted?.(comment.id);
        } catch (err) {
            showError(err, "Không xoá được bình luận.");
        }
    };

    const handleLike = async () => {
        if (!userId) {
            toastEmitter.info("Hãy đăng nhập để thích bình luận");
            return;
        }
        await toggleLike({ targetId: comment.id, targetType: "COMMENT", userId });
    };

    const handlePin = async () => {
        try {
            await pinComment({ id: comment.id, targetId: listTargetId, pinned: !comment.pinned }).unwrap();
            toastEmitter.success(comment.pinned ? "Đã bỏ ghim bình luận" : "Đã ghim bình luận");
        } catch (err) {
            showError(err, "Không ghim được bình luận.");
        }
    };

    const handleHeart = async () => {
        const next = !hearted;
        setHeartOverride(next);
        try {
            await heartComment({ id: comment.id, hearted: next }).unwrap();
        } catch (err) {
            setHeartOverride(!next);
            showError(err, "Không thả tim được.");
        }
    };

    const loadReplies = useCallback(async (commentId: string) => {
        try {
            setReplies((prev) => ({ ...prev, isLoading: true }));
            const res = await apiFetch(`${API_PREFIX}/comments/reply/${commentId}`);
            if (!res.ok) throw new Error("Không tải được các phản hồi");
            const data = await res.json();
            setReplies({ isLoading: false, loaded: true, replies: data.map(mapApiCommentToUI) });
        } catch (err) {
            console.error(err);
            setReplies({ isLoading: false, loaded: true, replies: [] });
        }
    }, []);

    useEffect(() => {
        if (isRepliesSectionOpen && !replies.loaded && comment.id) {
            void loadReplies(comment.id);
        }
    }, [comment.id, isRepliesSectionOpen, replies.loaded, loadReplies]);

    // Trả lời một phản hồi thì gắn vào bình luận gốc (kèm @handle người được trả lời); trả lời bình luận gốc thì gắn thẳng vào nó
    const parentCommentId = rootId ?? comment.id;
    const replyPrefix = rootId && comment.handle ? `@${comment.handle} ` : "";

    const handleSubmitReply = async (content: string) => {
        if (!userId || !videoId || !content.trim()) return;
        try {
            await createComment({
                targetId: videoId,
                targetType: "VIDEO",
                userId,
                content,
                parentCommentId,
            }).unwrap();
            setIsReplying(false);
            if (rootId) {
                // Phản hồi mới nằm ở bình luận gốc: nhờ bình luận gốc tải lại danh sách của nó
                window.dispatchEvent(new CustomEvent("wetube:reload-replies", { detail: rootId }));
            } else {
                setIsRepliesSectionOpen(true);
                await loadReplies(comment.id);
            }
        } catch (err) {
            // Ô phản hồi vẫn mở, giữ nguyên chữ để gửi lại
            showError(err, "Không gửi được phản hồi, vui lòng thử lại.");
        }
    };

    useEffect(() => {
        const onReload = (event: Event) => {
            if ((event as CustomEvent<string>).detail === comment.id) {
                setIsRepliesSectionOpen(true);
                void loadReplies(comment.id);
            }
        };
        window.addEventListener("wetube:reload-replies", onReload);
        return () => window.removeEventListener("wetube:reload-replies", onReload);
    }, [comment.id, loadReplies]);

    const handleSubmitEdit = async (content: string) => {
        if (!userId || !content.trim()) return;
        try {
            await updateComment({
                id: comment.id,
                userId,
                content,
                targetId: listTargetId,
                targetType: "VIDEO",
            }).unwrap();
            setEditedContent(content);
            setIsEditing(false);
        } catch (err) {
            showError(err, "Không lưu được bình luận, vui lòng thử lại.");
        }
    };

    const handleRepliesSection = () => {
        setIsRepliesSectionOpen((prev) => !prev);
    };

    // Số phản hồi hiển thị: ưu tiên danh sách đã tải (đúng sau khi thêm/xoá), nếu chưa tải thì lấy từ API
    const replyCount = replies.loaded ? replies.replies.length : comment.replyCount;
    const canPin = isVideoOwner && !rootId;
    const canReport = !!userId && !isOwner;
    const hasMenu = canPin || isOwner || canDelete || canReport;
    const isEdited = !!comment.editedAt || editedContent !== null;

    return (
        <motion.div
            variants={item}
            className="group flex w-full justify-start items-start gap-4"
        >
            {comment.picture ? (
                <Profile url={comment.picture} size="10" />
            ) : (
                <div
                    className={`h-10 w-10 shrink-0 ${getRandomBackground()} text-white font-semibold text-lg flex justify-center items-center rounded-full`}
                >
                    {comment.commenter.toUpperCase().charAt(0)}
                </div>
            )}
            <div className="flex flex-col justify-center items-start w-full min-w-0 gap-1">
                {comment.pinned && !rootId && (
                    <p className="flex items-center gap-1 text-xs text-gray-600">
                        <Pin className="size-3.5" />
                        Đã ghim bởi {video?.name ?? "chủ kênh"}
                    </p>
                )}
                <span className="flex justify-start items-center gap-2">
                    {comment.byCreator ? (
                        <p className="bg-gray-500 text-white text-xs font-semibold rounded-full px-2 py-0.5">
                            {comment.commenter}
                        </p>
                    ) : (
                        <p className="text-gray-700 text-sm font-semibold">
                            {comment.commenter}
                        </p>
                    )}
                    {comment.premium && <PremiumBadge />}
                    {comment.handle && <p className="text-xs text-gray-500">@{comment.handle}</p>}
                    <p className="text-xs text-gray-600">
                        {timeAgo(comment.commentedAt)}
                        {isEdited && " (đã chỉnh sửa)"}
                    </p>
                </span>

                {isEditing ? (
                    <InlineComposer
                        initialValue={editedContent ?? comment.comment}
                        placeholder="Sửa bình luận..."
                        submitLabel="Lưu"
                        busy={isUpdating}
                        onSubmit={handleSubmitEdit}
                        onCancel={() => setIsEditing(false)}
                    />
                ) : (
                    <LinkifiedText text={editedContent ?? comment.comment} className="text-sm" />
                )}

                <div className="flex justify-start items-center gap-3">
                    <button
                        onClick={handleLike}
                        className={`inline-flex justify-center items-center gap-1 ${
                            likeInfo?.liked ? "text-blue-600" : "hover:text-blue-600"
                        }`}
                        aria-label="Thích"
                    >
                        <HandThumbUpIcon className="size-5" />
                        {(likeInfo?.likeCount ?? comment.likes) > 0 && (
                            <p className="text-xs text-gray-600 ">
                                {likeInfo?.likeCount ?? comment.likes}
                            </p>
                        )}
                    </button>
                    {hearted ? (
                        // Tim của chủ kênh: ảnh đại diện kênh kèm tim đỏ; chủ kênh bấm để bỏ tim
                        <button
                            onClick={isVideoOwner ? handleHeart : undefined}
                            disabled={!isVideoOwner || isHearting}
                            title={`Được ${video?.name ?? "chủ kênh"} thả tim`}
                            aria-label={`Được ${video?.name ?? "chủ kênh"} thả tim`}
                            className="relative size-6 shrink-0 disabled:cursor-default"
                        >
                            {video?.picture ? (
                                // eslint-disable-next-line @next/next/no-img-element
                                <img src={video.picture} alt="" className="size-5 rounded-full object-cover" />
                            ) : (
                                <span className="block size-5 rounded-full bg-gray-300" />
                            )}
                            <HeartSolidIcon className="absolute -bottom-0.5 -right-0.5 size-3.5 text-red-600" />
                        </button>
                    ) : (
                        isVideoOwner && (
                            <button
                                onClick={handleHeart}
                                disabled={isHearting}
                                className="hover:text-red-600"
                                aria-label="Thả tim"
                                title="Thả tim"
                            >
                                <HeartIcon className="size-5" />
                            </button>
                        )
                    )}
                    {userId && (
                        <button
                            onClick={() => {
                                setIsReplying((prev) => !prev);
                                setIsEditing(false);
                            }}
                            className="text-xs font-semibold text-gray-600 hover:text-blue-600 rounded-full px-2 py-1 hover:bg-gray-100"
                        >
                            Phản hồi
                        </button>
                    )}
                </div>

                {isReplying && (
                    <InlineComposer
                        initialValue={replyPrefix}
                        placeholder="Viết phản hồi..."
                        submitLabel="Phản hồi"
                        busy={isPosting}
                        onSubmit={handleSubmitReply}
                        onCancel={() => setIsReplying(false)}
                    />
                )}

                {/* Nút xem replies */}
                {replyCount > 0 && (
                    <button
                        className="hover:bg-blue-100 text-blue-600 rounded-full flex items-center gap-2 py-2 px-3"
                        onClick={handleRepliesSection}
                    >
                        {isRepliesSectionOpen ? (
                            <ChevronUpIcon className="size-5" />
                        ) : (
                            <ChevronDownIcon className="size-5" />
                        )}
                        <p className="text-sm font-bold">
                            {replyCount} phản hồi
                        </p>
                    </button>
                )}

                {/* Hiển thị replies */}
                {isRepliesSectionOpen && (
                    <div className="flex flex-col justify-center items-start w-full gap-6 mt-2">
                        {replies.isLoading && !replies.loaded ? (
                            <Spinner />
                        ) : (
                            replies.replies.map((reply) => (
                                <CommentComponent
                                    key={reply.id}
                                    comment={reply}
                                    targetId={comment.id}
                                    rootId={comment.id}
                                    onDeleted={(id) =>
                                        setReplies((prev) => ({
                                            ...prev,
                                            replies: prev.replies.filter((r) => r.id !== id),
                                        }))
                                    }
                                />
                            ))
                        )}
                    </div>
                )}
            </div>

            {hasMenu && (
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        {/* Màn hình lớn: chỉ hiện khi rê chuột vào bình luận (giống YouTube) */}
                        <button
                            className="shrink-0 rounded-full p-1.5 hover:bg-gray-100 md:opacity-0 md:group-hover:opacity-100 focus-visible:opacity-100 data-[state=open]:opacity-100"
                            aria-label="Tác vụ khác"
                        >
                            <EllipsisVerticalIcon className="size-5" />
                        </button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                        {canPin && (
                            <DropdownMenuItem onClick={handlePin}>
                                {comment.pinned ? <PinOff className="size-4" /> : <Pin className="size-4" />}
                                {comment.pinned ? "Bỏ ghim" : "Ghim"}
                            </DropdownMenuItem>
                        )}
                        {isOwner && (
                            <DropdownMenuItem
                                onClick={() => {
                                    setIsEditing(true);
                                    setIsReplying(false);
                                }}
                            >
                                <Pencil className="size-4" />
                                Chỉnh sửa
                            </DropdownMenuItem>
                        )}
                        {canDelete && (
                            <DropdownMenuItem onClick={handleDelete} disabled={isDeleting}>
                                <Trash2 className="size-4" />
                                {isOwner ? "Xoá" : "Gỡ bỏ"}
                            </DropdownMenuItem>
                        )}
                        {canReport && (
                            <DropdownMenuItem onClick={() => setReportOpen(true)}>
                                <Flag className="size-4" />
                                Báo cáo
                            </DropdownMenuItem>
                        )}
                    </DropdownMenuContent>
                </DropdownMenu>
            )}

            {canReport && (
                <ReportDialog
                    open={reportOpen}
                    onOpenChange={setReportOpen}
                    target={{ commentId: comment.id }}
                    title="Báo cáo bình luận"
                />
            )}
        </motion.div>
    );
};
