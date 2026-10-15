import { useEffect, useRef, useState } from "react";
import { useAuth } from "@/contexts/auth-context";
import {
    useCreateCommentMutation,
    useGetCommentsByTargetQuery,
} from "@/app/api/commentApi";
import { X } from "lucide-react";
import CommentItem from "@/components/comments/commentItem";

interface CommentsProps {
    targetId: string;
    targetType: "POST" | "VIDEO" | "COMMENT";
    isOpen: boolean;
    onClose: () => void;
    isOwnerOfPost?: boolean;
    isAdmin?: boolean;
    onPin?: (id: string) => void;
}

export default function CommentPanel({
                                         targetId,
                                         targetType,
                                         isOpen,
                                         onClose,
                                         isOwnerOfPost,
                                         isAdmin,
                                         onPin,
                                     }: CommentsProps) {
    const { user } = useAuth();
    const currentUserId = user?.sub;

    const { data: comments = [], isLoading, refetch } = useGetCommentsByTargetQuery({
        targetId,
        targetType,
    });

    const [createComment] = useCreateCommentMutation();
    const [newComment, setNewComment] = useState("");
    const commentsEndRef = useRef<HTMLDivElement | null>(null);

    useEffect(() => {
        if (commentsEndRef.current) {
            commentsEndRef.current.scrollIntoView({ behavior: "smooth" });
        }
    }, [comments]);

    if (!currentUserId || isLoading) return null;

    const handleAddComment = async () => {
        if (!newComment.trim()) return;
        await createComment({
            targetId,
            targetType,
            userId: currentUserId,
            content: newComment,
        });
        setNewComment("");
        refetch();
    };

    return (
        <div
            className={`fixed top-0 right-0 h-full w-[450px] max-w-[90vw] bg-[#111] text-white px-4 py-4 transition-all duration-500 ease-in-out z-50
        ${isOpen ? "translate-x-0 opacity-100" : "translate-x-full opacity-0 pointer-events-none"}`}
        >
            <div className="flex flex-col h-full">
                <div className="flex justify-between items-center mb-4">
                    <h2 className="font-bold text-lg">Bình luận</h2>
                    <button onClick={onClose}>
                        <X className="w-5 h-5" />
                    </button>
                </div>

                <div className="flex-1 overflow-y-auto p-3 space-y-3">
                    {comments.length > 0 ? (
                        <ul className="space-y-3">
                            {comments.map((c) => (
                                <CommentItem
                                    key={c.id}
                                    comment={c}
                                    targetId={targetId}
                                    targetType={targetType}
                                    currentUserId={currentUserId}
                                    isOwnerOfPost={isOwnerOfPost}
                                    isAdmin={isAdmin}
                                    onPin={onPin}
                                />
                            ))}
                            <div ref={commentsEndRef} />
                        </ul>
                    ) : (
                        <p className="text-gray-400">Chưa có bình luận nào.</p>
                    )}
                </div>

                <form
                    onSubmit={(e) => {
                        e.preventDefault();
                        handleAddComment();
                    }}
                    className="absolute bottom-4 left-0 right-0 px-4"
                >
                    <input
                        type="text"
                        value={newComment}
                        onChange={(e) => setNewComment(e.target.value)}
                        placeholder="Viết bình luận..."
                        className="w-full px-4 py-2 rounded-full bg-[#222] text-white placeholder-gray-400 border border-gray-600 focus:outline-none"
                    />
                </form>
            </div>
        </div>
    );
}
