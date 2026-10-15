"use client";

import {useEffect, useState} from "react";
import { useVotePollMutation } from "@/app/api/postApi";
import CommentPanel from "@/components/comments/commentPanel";
import {Heart, MessageCircle, Share2} from "lucide-react";
import {useGetLikeInfoQuery, useToggleLikeMutation} from "@/app/api/likeApi";
import PostMenu from "@/modules/channel/ui/posts/postMenu";
import {PollOption} from "@/types/post"
import Image from 'next/image';

interface PollPostCardProps {
    id: string;
    userId: string;
    avatar: string;
    channelName: string;
    timestamp: string;
    content: string;
    poll: {
        id: string;
        options: PollOption[];
        totalVotes: number;
    };
    comments?: number;
    likes: number;
    isCommentClicked?: boolean;
    onEdit: (id: string) => void;
    onDelete: (id: string) => void;
}

export default function PollPostCard({
                                         id,
                                         userId,
                                         avatar,
                                         channelName,
                                         timestamp,
                                         content,
                                         poll,
                                         comments,
                                         isCommentClicked = false,
                                         onEdit,
                                         onDelete,
                                     }: PollPostCardProps) {
    const { data: likeInfo } = useGetLikeInfoQuery({
        targetId: id,
        targetType: "POST",
        userId,
    });

    const [votePoll] = useVotePollMutation();
    const [selectedOption, setSelectedOption] = useState<string | null>(null);
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
        } catch (err) {
            console.error("Toggle like failed", err);
        }
    };

    const handleVote = async (optionId: string) => {
        try {
            setSelectedOption(optionId);
            const res = await votePoll({
                postId: id,
                optionId,
                userId,
            }).unwrap();

            if (res && res.options) {
                const updatedOptions = res.options.map((opt: any) => ({
                    ...opt,
                    percentage: poll.totalVotes > 0
                        ? Math.round((opt.voteCount / res.totalVotes) * 100)
                        : 0,
                }));

                poll.options = updatedOptions;
                poll.totalVotes = res.totalVotes;
            } else {
                const updatedOptions = poll.options.map((opt) =>
                    opt.optionId === optionId
                        ? {
                            ...opt,
                            voteCount: opt.voteCount + 1,
                            percentage: ((opt.voteCount + 1) / (poll.totalVotes + 1)) * 100,
                        }
                        : {
                            ...opt,
                            percentage: (opt.voteCount / (poll.totalVotes + 1)) * 100,
                        }
                );

                poll.options = updatedOptions;
                poll.totalVotes += 1;
            }
        } catch (e) {
            console.error("Vote failed", e);
            setSelectedOption(null);
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

        <div className="text-black text-sm whitespace-pre-line">{content}</div>

        <div className="space-y-2">
          {poll.options.map((opt) => (
            <button
              key={opt.optionId}
              onClick={() => handleVote(opt.optionId)}
              className={`w-full border rounded-lg px-3 py-2 text-left relative overflow-hidden ${
                selectedOption === opt.optionId
                  ? "border-blue-500"
                  : "border-gray-300"
              }`}
            >
              <div
                className={`absolute inset-0 ${
                  selectedOption === opt.optionId
                    ? "bg-blue-500/30"
                    : "bg-gray-200/40"
                }`}
                style={{ width: `${opt.percentage ?? 0}%` }}
              />

              {/* Nội dung option */}
              <div className="relative flex justify-between items-center">
                <span className="font-medium">{opt.optionText}</span>
                <span className="text-sm text-gray-600">
                  {opt.percentage ?? 0}% ({opt.voteCount})
                </span>
              </div>
            </button>
          ))}
          <div className="text-xs text-gray-500">
            Total votes: {poll.totalVotes}
          </div>
        </div>

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
