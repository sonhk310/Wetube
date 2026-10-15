export interface PlaylistItem {
  id: string;
  title: string;
  videoCount: number;
  stackThumbnails: string[];
}

export const mockPlaylists: PlaylistItem[] = [
  {
    id: "ss4",
    title: "HỎI ĐÁP SS4",
    videoCount: 19,
    stackThumbnails: [
      "https://yt3.ggpht.com/agQ2D2IMMYa2o3SqfuevOGi3ImWU81EnbrdIHNEHS4UmU_tPyQocycp2agw7St9ss7fFfU8KUiM0=s1600-c-fcrop64=1,00000000ffffffff-rw-nd-v1",
      "https://placehold.co/600x600/000/FFF?text=Transform",
      "https://placehold.co/600x600/000/FFF?text=Trans",
    ],
  },
];
