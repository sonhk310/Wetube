"use client";

import PostCard from "@/modules/channel/ui/posts/postCard";
import {useCreatePostMutation, useGetPostsByUserQuery} from "@/app/api/postApi";
import CreatePostForm from "@/modules/channel/ui/posts/createPost";
import {useAuth} from "@/contexts/auth-context";

type Props = {
    /** Chủ kênh: bài đăng của người này được hiển thị. */
    userId: string;
    ownerName?: string;
    ownerPicture?: string | null;
};

export default function CommunityPosts({ userId, ownerName, ownerPicture }: Props) {
    const {user} = useAuth();
    const isOwner = !!user?.sub && user.sub === userId;

    const { data: posts, isLoading, isError, refetch } = useGetPostsByUserQuery(userId, {
        skip: !userId,
        refetchOnMountOrArgChange: true,
        refetchOnFocus: true,
        refetchOnReconnect: true,
    });

    const [createPost] = useCreatePostMutation();


    const handleCreatePost = async (data: {
        content: string;
        imageUrl?: string | null;
        poll?: { options: { optionText: string }[] } | null;
    }) => {
        try {
            await createPost({
                userId,
                content: data.content,
                poll: data.poll || undefined,
                imageUrl: data.imageUrl ?? null,
            }).unwrap();

            refetch();
        } catch (err) {
            console.error("Create post failed:", err);
        }
    };


    if (isLoading) return <div className="p-4 text-gray-500">Đang tải...</div>;
    if (isError) return <div className="p-4 text-red-500">Không tải được bài đăng.</div>;

    const postsWithAuthor =
        posts
            ?.map((post) => ({
                ...post,
                author: post.author || {
                    id: userId,
                    name: ownerName ?? "",
                    avatarUrl: ownerPicture ?? "",
                },
            }))
            .sort(
                (a, b) =>
                    new Date(b.createdAt).getTime() -
                    new Date(a.createdAt).getTime()
            ) ?? [];

    return (
        <div className="flex flex-col gap-4 px-4 py-4">
            {isOwner && <CreatePostForm userId={userId} onSubmit={handleCreatePost} />}

            {postsWithAuthor.length === 0 && (
                <p className="text-center text-gray-500">Kênh này chưa có bài đăng nào.</p>
            )}

            {postsWithAuthor?.map((post) => (
                <PostCard
                    key={post.id}
                    post={post}
                    userId={user?.sub ?? ""}
                />
            ))}
        </div>
    );
}
