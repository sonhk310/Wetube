export interface WatchMember {
    userId: string;
    username: string;
    host: boolean;
}

export interface VideoRoom {
    id: string;
    title: string;
    duration: number;
    thumbnailUrl: string;
    videoUrl: string;
    author: string;
    authorImg: string;
    totalView: number;
    position: number;
    createdAt: string;
}

export interface MediaPlayerState {
    roomId: string;
    playing: boolean;
    currentTimeMillis: number;
    currentSongId: string;
}

export interface Room {
    roomId: string;
    members: WatchMember[];
    playlist: VideoRoom[];
    playerState: MediaPlayerState;
}
