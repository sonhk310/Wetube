import { configureStore } from "@reduxjs/toolkit";
import { managerApi } from "../services/api/managerApi.ts";
import { channelApi } from '../services/api/channelApi.ts';
import { videoApi } from "../services/api/videoApi.ts";
import { authApi } from "../services/api/authApi.ts";

export const store = configureStore({
  reducer: {
    [managerApi.reducerPath]: managerApi.reducer,
    [channelApi.reducerPath]: channelApi.reducer,
    [videoApi.reducerPath]: videoApi.reducer,
    [authApi.reducerPath]: authApi.reducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware()
      .concat(managerApi.middleware)
      .concat(channelApi.middleware)
      .concat(videoApi.middleware)
      .concat(authApi.middleware),
});



export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;