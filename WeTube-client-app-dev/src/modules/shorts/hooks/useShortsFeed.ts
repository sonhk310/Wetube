// modules/shorts/hooks/useShortsFeed.ts
"use client";

export interface ShortVideo {
  id: string;
  videoUrl: string;
  caption: string;
  username: string;
  avatar: string;
  hashtags: string;
  music: string;
}

export function useShortsFeed(): ShortVideo[] {
  return [
    {
      id: "1",
      videoUrl: "https://www.w3schools.com/html/mov_bbb.mp4",
      caption: "Bro was so confident 💀",
      username: "@subrozacs",
      avatar: "https://placehold.co/40x40.png",
      hashtags: "#valorant #subroza",
      music: "Let Me Know - Tamar Braxton",
    },
    {
      id: "2",
      videoUrl: "https://www.w3schools.com/html/movie.mp4",
      caption: "Epic clutch by Reyna 🔥",
      username: "@valorqueen",
      avatar: "https://placehold.co/40x40.png?text=R",
      hashtags: "#valorant #clutch",
      music: "Ready or Not - Bridgit Mendler",
    },
    {
      id: "3",
      videoUrl:
        "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
      caption: "Slowmo flower opening 🌸",
      username: "@naturelover",
      avatar: "https://placehold.co/40x40.png?text=N",
      hashtags: "#nature #slowmotion",
      music: "Nature Soundscape - Calm",
    },
  ];
}
