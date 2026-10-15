"use client";

import {
  Comment,
  useCreateCommentMutation,
  useUpdateCommentMutation,
  useDeleteCommentMutation,
  useGetRepliesQuery,
} from "@/app/api/commentApi";
import CommentActions from "@/components/ui/commentActions";
import { Heart, MessageCircle } from "lucide-react";
import Image from "next/image";
import { useState } from "react";
import { useGetLikeInfoQuery, useToggleLikeMutation } from "@/app/api/likeApi";

interface CommentItemProps {
  comment: Comment;
  targetId: string;
  targetType: "POST" | "VIDEO" | "COMMENT";
  parentId?: string;
  currentUserId: string;
  isOwnerOfPost?: boolean;
  isAdmin?: boolean;
  onPin?: (id: string) => void;
}

export default function CommentItem({
  comment,
  targetId,
  targetType,
  parentId,
  currentUserId,
  isOwnerOfPost = false,
  isAdmin = false,
  onPin,
}: CommentItemProps) {
  const { data: likeInfo, refetch } = useGetLikeInfoQuery({
    targetId: comment.id,
    targetType: "COMMENT",
    userId: currentUserId,
  });

  const [toggleLike] = useToggleLikeMutation();

  const [createComment] = useCreateCommentMutation();
  const [updateComment] = useUpdateCommentMutation();
  const [deleteComment] = useDeleteCommentMutation();

  const [isEditing, setIsEditing] = useState(false);
  const [editContent, setEditContent] = useState(comment.content);

  const [showReplyInput, setShowReplyInput] = useState(false);
  const [replyContent, setReplyContent] = useState("");

  const [showReplies, setShowReplies] = useState(false);

  const { data: replies } = useGetRepliesQuery(comment.id, {
    skip: !showReplies,
  });

  const handleUpdate = async () => {
    if (!editContent.trim()) return;
    await updateComment({
      id: comment.id,
      userId: currentUserId,
      content: editContent,
      targetId,
      targetType,
    });
    setIsEditing(false);
  };

  const handleDelete = async () => {
    await deleteComment({ id: comment.id, targetId });
  };

  const handleReply = async () => {
    if (!replyContent.trim()) return;
    await createComment({
      targetId,
      targetType,
      userId: currentUserId,
      content: replyContent,
      parentCommentId: parentId ?? comment.id,
    });
    setReplyContent("");
    setShowReplyInput(false);
    setShowReplies(true);
  };

  const handleShowReplyInput = () => {
    setShowReplyInput(true);
    const mention = `@${comment.user?.name || "user"} `;
    if (!replyContent.startsWith(mention)) {
      setReplyContent(mention);
    }
  };

  const handleToggleLike = async () => {
    await toggleLike({
      targetId: comment.id,
      targetType: "COMMENT",
      userId: currentUserId,
    });
    refetch();
  };

  return (
    <li className="bg-[#1a1a1a] p-3 rounded-lg">
      <div className="flex items-start space-x-2">
        {/* Avatar */}
        <Image
          src={comment.user?.picture || "/default-avatar.png"}
          alt={comment.user?.name || "avatar"}
          width={32}
          height={32}
          className="rounded-full"
        />
        <div className="flex-1">
          {/* User name */}
          <p className="font-semibold">{comment.user?.name || "Người dùng"}</p>

          {/* Content */}
          {isEditing ? (
            <div className="space-y-2">
              <input
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                className="w-full px-2 py-1 bg-[#222] rounded"
              />
              <div className="space-x-2">
                <button
                  onClick={handleUpdate}
                  className="text-green-400"
                >
                  Lưu
                </button>
                <button
                  onClick={() => setIsEditing(false)}
                  className="text-gray-400"
                >
                  Hủy
                </button>
              </div>
            </div>
          ) : (
            <p className="text-gray-200">{comment.content}</p>
          )}

          {/* Actions: Like, Reply, Edit/Delete/Pin */}
          <div className="flex items-center space-x-6 text-neutral-400 text-sm mt-1">
            {/* Like */}
            <div
              className={`flex items-center space-x-1 cursor-pointer transition-transform duration-200 ${
                likeInfo?.liked
                  ? "text-pink-500 scale-110"
                  : "text-gray-500 hover:text-white"
              }`}
              onClick={handleToggleLike}
            >
              <Heart
                size={18}
                fill={likeInfo?.liked ? "rgb(236,72,153)" : "transparent"}
                className="transition-colors duration-300"
              />
              <span>{likeInfo?.likeCount ?? 0}</span>
            </div>

            {/* Reply */}
            <div
              className="flex items-center space-x-1 cursor-pointer hover:text-white transition-transform duration-200"
              onClick={handleShowReplyInput}
            >
              <MessageCircle size={18} />
              <span>Trả lời</span>
            </div>

            {/* Edit/Delete/Pin */}
            <CommentActions
              canPin={isOwnerOfPost}
              canEdit={comment.userId === currentUserId}
              canDelete={comment.userId === currentUserId || isAdmin}
              onEdit={() => setIsEditing(true)}
              onDelete={handleDelete}
              onPin={() => onPin?.(comment.id)}
            />

            {/* Toggle replies */}
            {comment.replyCount ? (
              <button
                onClick={() => setShowReplies(!showReplies)}
                className="text-gray-400 hover:text-white"
              >
                {showReplies
                  ? "Ẩn phản hồi"
                  : `Xem ${comment.replyCount} phản hồi`}
              </button>
            ) : null}
          </div>

          {/* Reply input */}
          {showReplyInput && (
            <div className="mt-2">
              <input
                value={replyContent}
                onChange={(e) => setReplyContent(e.target.value)}
                placeholder="Viết phản hồi..."
                className="w-full px-2 py-1 bg-[#222] rounded"
              />
              <button
                onClick={handleReply}
                className="text-blue-400 mt-1"
              >
                Gửi
              </button>
            </div>
          )}

          {/* Replies */}
          {showReplies && replies && replies.length > 0 && (
            <ul className="mt-2 space-y-2 ml-6 border-l border-gray-700 pl-3">
              {replies.map((r) => (
                <CommentItem
                  key={r.id}
                  comment={r}
                  targetId={targetId}
                  targetType={targetType}
                  parentId={comment.id}
                  currentUserId={currentUserId}
                  isOwnerOfPost={isOwnerOfPost}
                  isAdmin={isAdmin}
                  onPin={onPin}
                />
              ))}
            </ul>
          )}
        </div>
      </div>
    </li>
  );
}
