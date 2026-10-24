import { apiFetch } from "@/lib/apiFetch";
import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import { API_PREFIX } from "@/constants/appConstant";

export interface Channel {
    channelId: string;
    avatarUrl: string;
    name: string;
    subscribers: number;
    description: string;
    videoUrl: string;
}

export interface SubscriptionRequest {
    subscriberId: string;
    channelId: string;
    tierId?: string; // Nullable
}

export interface UnsubscribeRequest {
    subscriberId: string;
    channelId: string;
}

export const subscriptionsApi = createApi({
    reducerPath: "subscriptionsApi",
    baseQuery: fetchBaseQuery({ baseUrl: `${API_PREFIX}/subscriptions`, credentials: "include", fetchFn: apiFetch }),
    endpoints: (builder) => ({
        getSubscribedChannels: builder.query<Channel[], string>({
            query: (userId) => `/${userId}`,
        }),
        // Backend trả chữ thường ("Subscribed successfully"), đọc như JSON sẽ báo lỗi dù đã đăng ký xong
        subscribe: builder.mutation<string, SubscriptionRequest>({
            query: (body) => ({
                url: "/subscribe",
                method: "POST",
                body,
                responseHandler: "text",
            }),
        }),
        unsubscribe: builder.mutation<string, UnsubscribeRequest>({
            query: (body) => ({
                url: "/unsubscribe",
                method: "POST",
                body,
                responseHandler: "text",
            }),
        }),
    }),
});

export const {
    useGetSubscribedChannelsQuery,
    useSubscribeMutation,
    useUnsubscribeMutation, } = subscriptionsApi;
