"use client";

import NormalPostCard from "./normPost";
import PollPostCard from "./pollPost";
import {Post} from "@/types/post";
import {useDeletePostMutation, useUpdatePostMutation} from "@/app/api/postApi";
import EditPostModal from "@/modules/channel/ui/posts/editPostModal";
import {useState} from "react";

interface PostCardProps {
    post: Post;
    userId: string;
}

export default function PostCard({post, userId}: PostCardProps) {
    const [deletePost] = useDeletePostMutation();
    const [updatePost] = useUpdatePostMutation();
    const [isEditOpen, setIsEditOpen] = useState(false);

    const handleDelete = async (id: string) => {
        if (confirm("Bạn có chắc chắn muốn xóa bài viết này?")) {
            await deletePost(id);
        }
    };

    const handleEdit = () => {
        setIsEditOpen(true);
    };

    const handleSave = async (newContent: string) => {
        await updatePost({
            postId: post.id,
            body: { content: newContent },
        });
    };

    const author = post.author!;

    return (
        <>
            {post.poll ? (
                <PollPostCard
                    id={post.id}
                    userId={userId}
                    avatar={author.avatarUrl}
                    channelName={author.name}
                    timestamp={post.createdAt}
                    content={post.content}
                    poll={{
                        id: post.poll.id,
                        options: post.poll.options,
                        totalVotes: post.poll.totalVotes ?? 0,
                    }}
                    likes={post.likeCount}
                    comments={post.commentCount}
                    onEdit={handleEdit}
                    onDelete={handleDelete}
                />
            ) : (
                <NormalPostCard
                    id={post.id}
                    userId={userId}
                    avatar={author.avatarUrl}
                    channelName={author.name}
                    timestamp={post.createdAt}
                    content={post.content}
                    imageUrl={post.imageUrl}
                    videoLink={post.videoLink}
                    likes={post.likeCount}
                    comments={post.commentCount}
                    onEdit={handleEdit}
                    onDelete={handleDelete}
                />
            )}

            <EditPostModal
                isOpen={isEditOpen}
                initialContent={post.content}
                onClose={() => setIsEditOpen(false)}
                onSave={handleSave}
            />
        </>
)
    ;
}
