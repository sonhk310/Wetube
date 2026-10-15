"use client";

import PostCard from "@/modules/channel/ui/posts/postCard";
import {useCreatePostMutation, useGetPostsByUserQuery} from "@/app/api/postApi";
import CreatePostForm from "@/modules/channel/ui/posts/createPost";
import {useAuth} from "@/contexts/auth-context";

type Props = {
    userId: string;
};

export default function CommunityPosts({ userId }: Props) {
    const {user} = useAuth();

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


    if (isLoading) return <div>LOADING...</div>;
    if (isError) return <div>ERROR</div>;

    const postsWithAuthor =
        posts
            ?.map((post) => ({
                ...post,
                author: post.author ||
                    (user
                        ? {
                            id: user.sub,
                            name: user.name,
                            avatarUrl: user.picture,
                        }
                        : undefined),
            }))
            .sort(
                (a, b) =>
                    new Date(b.createdAt).getTime() -
                    new Date(a.createdAt).getTime()
            ) ?? [];

    return (
        <div className="flex flex-col gap-4 px-4 py-4">
            <CreatePostForm userId={userId} onSubmit={handleCreatePost} />

            {postsWithAuthor?.map((post) => (
                <PostCard
                    key={post.id}
                    post={post}
                    userId={userId}
                />
            ))}
        </div>
    );
}
