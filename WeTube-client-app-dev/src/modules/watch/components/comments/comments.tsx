"use client";

import { useState } from "react";
import { Bars3BottomLeftIcon } from "@heroicons/react/24/outline";
import { CommentInput } from "../inputs/comment-input";
import { CommentComponent, Comment, mapApiCommentToUI } from "./comment";

import { Loader } from "../misc/loader";
import { useVideoStore } from "@/store/zustand/videoStore";
import { CommentSort, useGetCommentsByTargetQuery } from "@/app/api/commentApi";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuRadioGroup,
    DropdownMenuRadioItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

const SORT_LABELS: Record<CommentSort, string> = {
    top: "Bình luận hàng đầu",
    newest: "Mới nhất xếp trước",
};

export const Comments = () => {
    const videoDetail = useVideoStore((state) => state.videoDetail);
    const [sort, setSort] = useState<CommentSort>("top");

    const { data: apiComments, isLoading, isFetching } = useGetCommentsByTargetQuery(
        videoDetail
            ? { targetId: videoDetail.detail.id, targetType: "VIDEO", sort }
            : { targetId: "", targetType: "VIDEO", sort },
        { skip: !videoDetail }
    );

    if (!videoDetail) return <Loader />;
    if (isLoading) return <Loader />;

    const comments: Comment[] = (apiComments ?? []).map(mapApiCommentToUI);
    // Giống YouTube: số bình luận tính cả phản hồi
    const total = comments.reduce((sum, c) => sum + 1 + c.replyCount, 0);

    return (
        <div className="mt-4">
            <div className="flex items-center gap-8">
                <p className="text-xl font-bold">
                    {total.toLocaleString("vi-VN")} bình luận
                </p>
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <button className="flex items-center gap-2 text-sm font-semibold rounded-full px-2 py-1 hover:bg-gray-100">
                            <Bars3BottomLeftIcon className="size-5" />
                            Sắp xếp theo
                        </button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="start">
                        <DropdownMenuRadioGroup value={sort} onValueChange={(v) => setSort(v as CommentSort)}>
                            {(Object.keys(SORT_LABELS) as CommentSort[]).map((key) => (
                                <DropdownMenuRadioItem key={key} value={key}>
                                    {SORT_LABELS[key]}
                                </DropdownMenuRadioItem>
                            ))}
                        </DropdownMenuRadioGroup>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>

            <div className="mt-3">
                <CommentInput />
            </div>

            <div className={`mt-3 flex flex-col gap-6 ${isFetching ? "opacity-60" : ""}`}>
                {comments.map((c) => (
                    <CommentComponent key={c.id} comment={c} targetId={videoDetail.detail.id} />
                ))}
            </div>
        </div>
    );
};
