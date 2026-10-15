"use client";

import { useAuth } from "@/contexts/auth-context";
import CommunityPosts from "@/modules/channel/ui/posts/communityPosts";


export default function ChannelPostsPage() {
  const { user } = useAuth();
  
  if (!user) return null;

  const userId = user.sub;

  return <CommunityPosts userId={userId} />;
}
