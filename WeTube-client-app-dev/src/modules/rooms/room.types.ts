export interface Room {
    id: string;
    hostId: string;
    videoUrl?: string;
}

export interface Member {
    id: string;
    name: string;
}

export interface VideoItem {
    id: string;          // ID video (string cho an toàn, có thể là UUID)
    title: string;       // tiêu đề video
    thumbnail: string;   // link ảnh thumbnail
    url: string;         // link video (stream / file / YouTube embed)
    channelName: string; // tên kênh
    views: number;       // số lượt xem
    duration?: string;   // thời lượng video, ví dụ "12:34"
    publishedAt?: string; // ngày đăng (ISO string)
}