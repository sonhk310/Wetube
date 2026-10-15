import PlaylistDetail from "@/modules/playlists/ui/list/playlistdetail-list";

interface PlaylistsProps {
  readonly params: Promise<{ playlistId: string }>;
}

export default async function PlaylistsPage({ params }: PlaylistsProps) {
    const { playlistId } = await params;
  return <PlaylistDetail playlistId={playlistId} />;
}
