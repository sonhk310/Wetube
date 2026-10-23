"use client";
import {
    ChevronDownIcon,
    ChevronUpIcon,
    HandThumbUpIcon,
} from "@heroicons/react/24/outline";
import Profile from "../misc/profile";
import { motion } from "motion/react";
import { useCallback, useEffect, useState } from "react";
import { useGetLikeInfoQuery, useToggleLikeMutation } from "@/app/api/likeApi";
import { useAuth } from "@/contexts/auth-context";
import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";
import {
    useCreateCommentMutation,
    useDeleteCommentMutation,
    useUpdateCommentMutation,
} from "@/app/api/commentApi";
import { useVideoStore } from "@/store/zustand/videoStore";

export interface Comment {
    id: string;
    authorId?: string;
    commenter: string;
    comment: string;
    picture: string;
    commentedAt: string;
    likes: number;
    dislikes: number;
    replyCount: number;
    replies?: Comment[];
}

// Chuyển bình luận từ API sang dạng hiển thị
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const mapApiCommentToUI = (apiComment: any): Comment => ({
    id: apiComment.id,
    authorId: apiComment.user?.id,
    commenter: apiComment.user?.name ?? "Ẩn danh",
    comment: apiComment.content,
    picture: apiComment.user?.picture ?? "",
    commentedAt: apiComment.createdAt,
    likes: apiComment.likeCount ?? 0,
    dislikes: 0,
    replyCount: apiComment.replyCount ?? 0,
    replies: apiComment.replies ?? [],
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

/** Ô nhập nhanh dùng cho "Trả lời" và "Sửa". */
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
    // Nội dung sau khi sửa, hiển thị ngay mà không chờ tải lại danh sách
    const [editedContent, setEditedContent] = useState<string | null>(null);
    const { user } = useAuth();
    const userId = user?.sub;
    const videoId = useVideoStore((state) => state.videoDetail?.detail.id);

    const { data: likeInfo } = useGetLikeInfoQuery(
        { targetId: comment.id, targetType: "COMMENT", userId: userId! },
        { skip: !userId }
    );
    const [toggleLike] = useToggleLikeMutation();
    const [deleteComment, { isLoading: isDeleting }] = useDeleteCommentMutation();
    const [createComment, { isLoading: isPosting }] = useCreateCommentMutation();
    const [updateComment, { isLoading: isUpdating }] = useUpdateCommentMutation();
    const isOwner = !!userId && userId === comment.authorId;
    // Chủ bình luận hoặc admin được xoá (backend cũng kiểm tra quyền)
    const canDelete = !!userId && (isOwner || !!user?.roles?.includes("ROLE_ADMIN"));
    // Danh sách bình luận của video được làm mới theo id video
    const listTargetId = videoId ?? targetId;

    const handleDelete = async () => {
        if (!window.confirm("Xoá bình luận này?")) return;
        try {
            await deleteComment({ id: comment.id, targetId: listTargetId }).unwrap();
            onDeleted?.(comment.id);
        } catch (err) {
            console.error("Delete comment failed:", err);
        }
    };

    const handleLike = async () => {
        if (!userId) return;
        await toggleLike({ targetId: comment.id, targetType: "COMMENT", userId });
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

    // Trả lời một phản hồi thì gắn vào bình luận gốc (kèm @tên); trả lời bình luận gốc thì gắn thẳng vào nó
    const parentCommentId = rootId ?? comment.id;
    const replyPrefix = rootId ? `@${comment.commenter} ` : "";

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
            console.error("Reply failed:", err);
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
            console.error("Edit comment failed:", err);
        }
    };

    const handleRepliesSection = () => {
        setIsRepliesSectionOpen((prev) => !prev);
    };

    // Số phản hồi hiển thị: ưu tiên danh sách đã tải (đúng sau khi thêm/xoá), nếu chưa tải thì lấy từ API
    const replyCount = replies.loaded ? replies.replies.length : comment.replyCount;

    return (
        <motion.div
            variants={item}
            className="flex w-full justify-start items-start gap-4"
        >
            {comment.picture ? (
                <Profile url={comment.picture} size="10" />
            ) : (
                <div
                    className={`h-10 w-10 ${getRandomBackground()} text-white font-semibold text-lg flex justify-center items-center rounded-full`}
                >
                    {comment.commenter.toUpperCase().charAt(0)}
                </div>
            )}
            <div className="flex flex-col justify-center items-start w-full gap-1">
                <span className="flex justify-start items-center gap-2">
          <p className="text-gray-700 text-sm font-semibold">
            {comment.commenter}
          </p>
          <p className="text-xs text-gray-600">
            {new Date(comment.commentedAt).toLocaleDateString("vi-VN")}
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
                    <p className="text-sm whitespace-pre-wrap break-words">
                        {editedContent ?? comment.comment}
                    </p>
                )}

                <div className="flex justify-start items-center gap-3">
                    <button
                        onClick={handleLike}
                        className={`inline-flex justify-center items-center gap-1 ${
                            likeInfo?.liked ? "text-blue-600" : "hover:text-blue-600"
                        }`}
                    >
                        <HandThumbUpIcon className="size-5" />
                        {(likeInfo?.likeCount ?? 0) > 0 && (
                            <p className="text-xs text-gray-600 ">
                                {likeInfo?.likeCount ?? 0}
                            </p>
                        )}
                    </button>
                    {userId && (
                        <button
                            onClick={() => {
                                setIsReplying((prev) => !prev);
                                setIsEditing(false);
                            }}
                            className="text-xs font-semibold text-gray-600 hover:text-blue-600"
                        >
                            Trả lời
                        </button>
                    )}
                    {isOwner && (
                        <button
                            onClick={() => {
                                setIsEditing((prev) => !prev);
                                setIsReplying(false);
                            }}
                            className="text-xs text-gray-500 hover:text-blue-600"
                        >
                            Sửa
                        </button>
                    )}
                    {canDelete && (
                        <button
                            onClick={handleDelete}
                            disabled={isDeleting}
                            className="text-xs text-gray-500 hover:text-red-600 disabled:opacity-50"
                        >
                            Xoá
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
                    <div className="flex flex-col justify-center items-start w-full gap-6 ml-12 mt-2">
                        {replies.isLoading && !replies.loaded ? (
                            <p className="text-xs text-gray-500">Đang tải phản hồi...</p>
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
        </motion.div>
    );
};
