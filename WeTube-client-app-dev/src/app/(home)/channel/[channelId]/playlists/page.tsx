import ProtectedRoute from '@/components/ProtectedRoute';
import ChannelPlaylists from "@/modules/channel/ui/playlists/channelPlayLists";

export default function ChannelPostsPage() {
  return (
    <ProtectedRoute>
      <ChannelPlaylists />
    </ProtectedRoute>
  );
}