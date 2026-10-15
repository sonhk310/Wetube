"use client";

import { Heart, MessageCircle, Share2 } from "lucide-react";
import {useGetLikeInfoQuery, useToggleLikeMutation} from "@/app/api/likeApi";
import {useEffect, useState} from "react";
import CommentPanel from "@/components/comments/commentPanel";
import PostMenu from "@/modules/channel/ui/posts/postMenu";
import Image from "next/image";

interface NormalPostCardProps {
    id: string;
    userId: string;
    avatar: string;
    channelName: string;
    timestamp: string;
    content: string;
    imageUrl?: string;
    videoLink?: string;
    comments: number;
    likes: number;
    isCommentClicked?: boolean;
    onEdit: (id: string) => void;
    onDelete: (id: string) => void;
}

export default function NormalPostCard({
                                           id,
                                           userId,
                                           avatar,
                                           channelName,
                                           timestamp,
                                           content,
                                           imageUrl,
                                           videoLink,
                                           comments,
                                           isCommentClicked = false,
                                           onEdit,
                                           onDelete,
                                       }: NormalPostCardProps) {
    const { data: likeInfo, refetch } = useGetLikeInfoQuery({
        targetId: id,
        targetType: "POST",
        userId,
    });

    const [toggleLike] = useToggleLikeMutation();

    const [liked, setLiked] = useState<boolean>(false);
    const [likeCount, setLikeCount] = useState<number>(0);
    const [clicked, setClicked] = useState(isCommentClicked);
    const [showComment, setShowComment] = useState(false);

    useEffect(() => {
        if (likeInfo) {
            setLiked(likeInfo.liked ?? false);
            setLikeCount(likeInfo.likeCount ?? 0);
        }
    }, [likeInfo]);

    const handleLike = async () => {
        try {
            const res = await toggleLike({
                targetId: id,
                targetType: "POST",
                userId,
            }).unwrap();

            if (res) {
                setLiked(res.liked ?? false);
                setLikeCount(res.likeCount ?? 0);
            }
            refetch();
        } catch (err) {
            console.error("Toggle like failed", err);
        }
    };

    return (
      <div
        className="bg-white shadow-xl rounded-xl p-4 space-y-3
                w-full max-w-[600px]
                mx-auto overflow-hidden"
      >
        <div className="flex items-start space-x-3">
          <Image
            src={avatar}
            alt={channelName || "avatar"}
            width={40}
            height={40}
            className="rounded-full"
          />{" "}
          <div className="flex flex-col text-black text-sm">
            <div className="font-semibold">{channelName}</div>
            <div className="text-neutral-400">{timestamp}</div>
          </div>
          <PostMenu
            onEdit={() => onEdit(id)}
            onDelete={() => onDelete(id)}
          />
        </div>

        <div className="text-black text-sm whitespace-pre-line">
          {content}
          {videoLink && (
            <div className="text-blue-400 underline mt-1">{videoLink}</div>
          )}
        </div>

        {imageUrl && (
          <div className="rounded-lg overflow-hidden border border-neutral-800">
            <Image
              src={imageUrl}
              alt={content.substring(0, 50) || "Post image"}
              width={600}
              height={400}
              className="w-full object-cover"
            />
          </div>
        )}

        <div className="flex items-center space-x-6 text-neutral-400 text-sm">
          <div
            className={`flex items-center space-x-1 cursor-pointer transition-transform duration-200 ${
              liked
                ? "text-pink-500 scale-110"
                : "text-gray-500 hover:text-black"
            }`}
            onClick={handleLike}
          >
            <Heart
              size={20}
              fill={liked ? "rgb(236,72,153)" : "transparent"}
              className="transition-colors duration-300"
            />
            <span>{likeCount}</span>
          </div>
          <div className="flex items-center space-x-1 hover:text-black cursor-pointer">
            <Share2 size={20} />
          </div>
          <div
            className={`flex items-center space-x-1 cursor-pointer transition-transform duration-200 ${
              clicked
                ? "text-blue-500 scale-110"
                : "text-gray-500 hover:text-black"
            }`}
            onClick={() => {
              setShowComment(true);
              setClicked(true);
            }}
          >
            <MessageCircle
              size={20}
              fill={clicked ? "rgb(72,184,236)" : "transparent"}
              className="transition-colors duration-300"
            />
            <span>{comments}</span>
          </div>
        </div>

        <CommentPanel
          targetId={id}
          targetType="POST"
          isOpen={showComment}
          onClose={() => {
            setShowComment(false);
            setClicked(false);
          }}
        />
      </div>
    );
}
