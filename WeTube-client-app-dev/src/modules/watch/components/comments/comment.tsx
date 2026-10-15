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
// import { useCreateCommentMutation } from "@/app/api/commentApi";

export interface Comment {
    id: string;
    commenter: string;
    comment: string;
    picture: string;
    commentedAt: string;
    likes: number;
    dislikes: number;
    replyCount: number;
    replies?: Comment[];
}

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

export const CommentComponent = ({ comment }: { comment: Comment }) => {
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
                const res = await fetch(`/api/comments/${commentId}/replies`);
                if (!res.ok) throw new Error("Failed to fetch replies");
                const data: Comment[] = await res.json();
                setReplies({ isLoading: false, replies: data });
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
            {new Date(comment.commentedAt).toLocaleDateString()}
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
                            {comment.replyCount} replies
                        </p>
                    </button>
                )}

                {/* Hiển thị replies */}
                {isRepliesSectionOpen && (
                    <div className="flex flex-col justify-center items-start w-full gap-6 ml-12 mt-2">
                        {replies.isLoading ? (
                            <p className="text-xs text-gray-500">Loading replies...</p>
                        ) : (
                            replies.replies.map((reply) => (
                                <CommentComponent key={reply.id} comment={reply} />
                            ))
                        )}
                    </div>
                )}
            </div>
        </motion.div>
    );
};
