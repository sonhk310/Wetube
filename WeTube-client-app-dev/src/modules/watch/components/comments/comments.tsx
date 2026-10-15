
import { CommentInput } from "../inputs/comment-input";
import { CommentComponent, Comment } from "./comment";

import { Loader } from "../misc/loader";
import { useVideoStore } from "@/store/zustand/videoStore";
import { useGetCommentsByTargetQuery } from "@/app/api/commentApi";

// const container = {
//   hidden: { opacity: 0 },
//   show: {
//     opacity: 1,
//     transition: {
//       duration: 0.15,
//       type: "spring",
//       staggerChildren: 0.1,
//       bounce: 0.2,
//     },
//   },
// };

const mapApiCommentToUI = (apiComment: any): Comment => ({
    id: apiComment.id,
    commenter: apiComment.user?.name ?? "Unknown",
    comment: apiComment.content,
    picture: apiComment.user?.picture ?? "",
    commentedAt: apiComment.createdAt,
    likes: apiComment.likeCount ?? 0,
    dislikes: 0,
    replyCount: apiComment.replyCount ?? 0,
    replies: apiComment.replies ?? [],
});

export const Comments = () => {
    const videoDetail = useVideoStore((state) => state.videoDetail);

    const { data: apiComments, isLoading } = useGetCommentsByTargetQuery(
        videoDetail
            ? { targetId: videoDetail.detail.id, targetType: "VIDEO" }
            : { targetId: "", targetType: "VIDEO" },
        { skip: !videoDetail }
    );

    if (!videoDetail) return <Loader />;
    if (isLoading) return <Loader />;

    const comments: Comment[] = (apiComments ?? []).map(mapApiCommentToUI);

    return (
        <div className="mt-4">
            <p className="text-xl font-bold">
                {comments.length} {comments.length > 1 ? "Comments" : "Comment"}
            </p>

            <div className="mt-3">
                <CommentInput />
            </div>

            <div className="mt-3">
                {comments.map((c) => (
                    <CommentComponent key={c.id} comment={c} />
                ))}
            </div>
        </div>
    );
};