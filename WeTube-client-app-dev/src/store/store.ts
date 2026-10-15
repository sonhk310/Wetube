import { commentApi } from "@/app/api/commentApi";
import { likesApi } from "@/app/api/likeApi";
import { playlistApi } from "@/app/api/playlistApi";
import { postsApi } from "@/app/api/postApi";
import { recommendApi } from "@/app/api/recommentApi";
import { searchApi } from "@/app/api/searchApi";
import { videoApi } from "@/app/api/videoApi";
import { subscriptionsApi } from "@/app/api/subscriptionsApi";
import { configureStore } from "@reduxjs/toolkit";
import { channelApi } from '@/app/api/channelApi';
import { interactionApi } from "@/app/api/interactionApi";

export const store = configureStore({
  reducer: {
    [postsApi.reducerPath]: postsApi.reducer,
    [likesApi.reducerPath]: likesApi.reducer,
    [commentApi.reducerPath]: commentApi.reducer,
    [recommendApi.reducerPath]: recommendApi.reducer,
    [videoApi.reducerPath]: videoApi.reducer,
    [searchApi.reducerPath]: searchApi.reducer,
    [playlistApi.reducerPath]: playlistApi.reducer,
    [subscriptionsApi.reducerPath]: subscriptionsApi.reducer,
    [channelApi.reducerPath]: channelApi.reducer,
    [interactionApi.reducerPath] : interactionApi.reducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(
      postsApi.middleware,
      likesApi.middleware,
      commentApi.middleware,
      recommendApi.middleware,
      videoApi.middleware,
      searchApi.middleware,
      playlistApi.middleware,
      channelApi.middleware,
      subscriptionsApi.middleware,
      interactionApi.middleware
    ),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
