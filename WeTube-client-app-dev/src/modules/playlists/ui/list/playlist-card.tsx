import Image from 'next/image';
import Link from "next/link";

export interface Playlists {
  playlistId: string;
  playlistTitle: string;
  playlistType: string;
  totalVideos: number;
  createdAt: string;
  privacy: string;
  lastUpdatedLabel: string;
  thumbnailUrl: string;
  videos?: Video[];
}
export interface Video {
  videoId: string;
  videoUrl: string;
  thumbnailUrl: string;
  createdAt: string;
}

export default function PlaylistCard({
  playlists,
  category,
}: {
  playlists: Playlists;
  category: string;
}) {
  const href =
    category === "watchlater"
      ? "/playlists/watchlater"
      : category === "liked"
      ? "/playlists/liked"
      : `/playlists/${playlists.playlistId}?category=${category.toLowerCase()}`;

      const latestThumbnail =
        playlists.videos?.sort(
          (a, b) =>
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        )[0]?.thumbnailUrl ||
        playlists.thumbnailUrl ||
        "/images/thumbnail.png";


  return (
    <div className="bg-white rounded-lg overflow-hidden shadow-sm hover:shadow-md transition-shadow h-full flex flex-col">
      <div className="relative aspect-video">
        <Image
          src={latestThumbnail || "/images/thumbnail.png"}
          alt={playlists.playlistTitle}
          fill
          className="w-full h-full object-cover"
        />
        <div className="absolute bottom-2 right-2 bg-black/80 text-white text-xs px-2 py-1 rounded">
          {playlists.totalVideos} video
        </div>
      </div>

      <div className="p-4 flex flex-col flex-1">
        <h3 className="font-medium text-gray-900 mb-2 line-clamp-2">
          {playlists.playlistTitle}
        </h3>

        <div className="text-xs text-gray-600 space-y-1">
          <p>{playlists.privacy} • Danh sách phát</p>
          <p className="text-gray-500">{playlists.lastUpdatedLabel}</p>

          <Link
            href={href}
            className="text-xs text-black hover:underline font-medium"
          >
            Xem toàn bộ danh sách
          </Link>
        </div>
      </div>
    </div>
  );
}
