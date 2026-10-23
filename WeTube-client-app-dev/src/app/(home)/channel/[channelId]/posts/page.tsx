"use client";

import { useParams } from "next/navigation";
import { useGetChannelInfoQuery } from "@/app/api/channelApi";
import CommunityPosts from "@/modules/channel/ui/posts/communityPosts";

export default function ChannelPostsPage() {
  const { channelId } = useParams<{ channelId: string }>();
  const { data: channel } = useGetChannelInfoQuery(channelId);

  if (!channel?.userId) return null;

  return (
    <CommunityPosts
      userId={channel.userId}
      ownerName={channel.name}
      ownerPicture={channel.picture}
    />
  );
}
