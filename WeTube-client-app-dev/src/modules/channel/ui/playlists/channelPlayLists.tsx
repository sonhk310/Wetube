// modules/channel/pages/channel-playlists.tsx
import { mockPlaylists } from "@/modules/channel/ui/playlists/mockPL";
import PlaylistCard from "@/modules/channel/ui/playlists/playListsCard";

export default function ChannelPlaylists() {
  return (
    <div className="grid gap-6 p-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
      {mockPlaylists.map((playlist) => (
        <PlaylistCard key={playlist.id} playlist={playlist} />
      ))}
    </div>
  );
}
