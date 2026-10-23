"use client";
import {
    ChevronDownIcon,
    ChevronUpIcon,
    HandThumbUpIcon,
} from "@heroicons/react/24/outline";
import Profile from "../misc/profile";
import { motion } from "motion/react";
import { useEffect, useState } from "react";
import { useGetLikeInfoQuery, useToggleLikeMutation } from "@/app/api/likeApi";
import { useAuth } from "@/contexts/auth-context";
import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";
import { useDeleteCommentMutation } from "@/app/api/commentApi";

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

export const CommentComponent = ({
    comment,
    targetId,
    onDeleted,
}: {
    comment: Comment;
    /** Video (bình luận gốc) hoặc bình luận cha (trả lời): dùng để làm mới danh sách sau khi xoá. */
    targetId: string;
    onDeleted?: (id: string) => void;
}) => {
    const [isRepliesSectionOpen, setIsRepliesSectionOpen] = useState(false);
    const [replies, setReplies] = useState<{
        isLoading: boolean;
        replies: Comment[];
    }>({ isLoading: false, replies: [] });
    const { user } = useAuth();
    const userId = user?.sub;

    const { data: likeInfo } = useGetLikeInfoQuery(
        { targetId: comment.id, targetType: "COMMENT", userId: userId! },
        { skip: !userId }
    );
    const [toggleLike] = useToggleLikeMutation();
    const [deleteComment, { isLoading: isDeleting }] = useDeleteCommentMutation();
    // Chủ bình luận hoặc admin được xoá (backend cũng kiểm tra quyền)
    const canDelete =
        !!userId && (userId === comment.authorId || !!user?.roles?.includes("ROLE_ADMIN"));

    const handleDelete = async () => {
        if (!window.confirm("Xoá bình luận này?")) return;
        try {
            await deleteComment({ id: comment.id, targetId }).unwrap();
            onDeleted?.(comment.id);
        } catch (err) {
            console.error("Delete comment failed:", err);
        }
    };

    // const [createComment] = useCreateCommentMutation();

    const handleLike = async () => {
        if (!userId) return;
        await toggleLike({ targetId: comment.id, targetType: "COMMENT", userId });
    };

    // const handleReply = () => {
    //     if (!userId) return;
    //     setReplying(true);
    //     setReplyText(`@${comment.commenter} `); // auto mention
    // };
    //
    // const handleSubmitReply = async () => {
    //     if (!userId || !replyText.trim()) return;
    //
    //     await createComment({
    //         targetId: comment.id,
    //         targetType: "COMMENT",
    //         userId,
    //         content: replyText,
    //         parentCommentId: comment.id,
    //     });
    //
    //     setReplying(false);
    //     setReplyText("");
    //     setIsRepliesSectionOpen(false);
    // };

    const handleRepliesSection = () => {
        setIsRepliesSectionOpen((prev) => !prev);
    };

    useEffect(() => {
        const fetchReplies = async (commentId: string) => {
            try {
                setReplies((prev) => ({ ...prev, isLoading: true }));
                const res = await apiFetch(`${API_PREFIX}/comments/reply/${commentId}`);
                if (!res.ok) throw new Error("Không tải được các phản hồi");
                const data = await res.json();
                setReplies({ isLoading: false, replies: data.map(mapApiCommentToUI) });
            } catch (err) {
                console.error(err);
                setReplies({ isLoading: false, replies: [] });
            }
        };

        if (isRepliesSectionOpen && !replies.replies.length && comment.id) {
            fetchReplies(comment.id);
        }
    }, [comment.id, isRepliesSectionOpen, replies.replies.length]);

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

                <p className="text-sm">{comment.comment}</p>

                <div className="flex justify-start items-center gap-2">
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

                {/* Nút xem replies */}
                {comment.replyCount > 0 && (
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
                            {comment.replyCount} phản hồi
                        </p>
                    </button>
                )}

                {/* Hiển thị replies */}
                {isRepliesSectionOpen && (
                    <div className="flex flex-col justify-center items-start w-full gap-6 ml-12 mt-2">
                        {replies.isLoading ? (
                            <p className="text-xs text-gray-500">Đang tải phản hồi...</p>
                        ) : (
                            replies.replies.map((reply) => (
                                <CommentComponent
                                    key={reply.id}
                                    comment={reply}
                                    targetId={comment.id}
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
