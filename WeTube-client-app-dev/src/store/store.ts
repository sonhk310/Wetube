import { commentApi } from "@/app/api/commentApi";
import { likesApi } from "@/app/api/likeApi";
import { playlistApi } from "@/app/api/playlistApi";
import { postsApi } from "@/app/api/postApi";
import { recommendApi } from "@/app/api/recommentApi";
import { homeApi } from "@/app/api/homeApi";
import { searchApi } from "@/app/api/searchApi";
import { videoApi } from "@/app/api/videoApi";
import { subscriptionsApi } from "@/app/api/subscriptionsApi";
import { configureStore } from "@reduxjs/toolkit";
import { setupListeners } from "@reduxjs/toolkit/query";
import { channelApi } from '@/app/api/channelApi';
import { interactionApi } from "@/app/api/interactionApi";
import { analyticsApi } from "@/app/api/analyticsApi";
import { liveApi } from "@/app/api/liveApi";
import { subtitleApi } from "@/app/api/subtitleApi";

export const store = configureStore({
  reducer: {
    [postsApi.reducerPath]: postsApi.reducer,
    [likesApi.reducerPath]: likesApi.reducer,
    [commentApi.reducerPath]: commentApi.reducer,
    [recommendApi.reducerPath]: recommendApi.reducer,
    [homeApi.reducerPath]: homeApi.reducer,
    [videoApi.reducerPath]: videoApi.reducer,
    [searchApi.reducerPath]: searchApi.reducer,
    [playlistApi.reducerPath]: playlistApi.reducer,
    [subscriptionsApi.reducerPath]: subscriptionsApi.reducer,
    [channelApi.reducerPath]: channelApi.reducer,
    [interactionApi.reducerPath] : interactionApi.reducer,
    [analyticsApi.reducerPath]: analyticsApi.reducer,
    [liveApi.reducerPath]: liveApi.reducer,
    [subtitleApi.reducerPath]: subtitleApi.reducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(
      postsApi.middleware,
      likesApi.middleware,
      commentApi.middleware,
      recommendApi.middleware,
      homeApi.middleware,
      videoApi.middleware,
      searchApi.middleware,
      playlistApi.middleware,
      channelApi.middleware,
      subscriptionsApi.middleware,
      interactionApi.middleware,
      analyticsApi.middleware,
      liveApi.middleware,
      subtitleApi.middleware
    ),
});

// Cho phép refetchOnFocus / refetchOnReconnect của các query
setupListeners(store.dispatch);

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
