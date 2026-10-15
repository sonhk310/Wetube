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
    baseQuery: fetchBaseQuery({ baseUrl: `${API_PREFIX}/subscriptions` }),
    endpoints: (builder) => ({
        getSubscribedChannels: builder.query<Channel[], string>({
            query: (userId) => `/${userId}`,
        }),
        subscribe: builder.mutation<string, SubscriptionRequest>({
            query: (body) => ({
                url: "/subscribe",
                method: "POST",
                body,
            }),
        }),
        unsubscribe: builder.mutation<string, UnsubscribeRequest>({
            query: (body) => ({
                url: "/unsubscribe",
                method: "POST",
                body,
            }),
        }),
    }),
});

export const {
    useGetSubscribedChannelsQuery,
    useSubscribeMutation,
    useUnsubscribeMutation, } = subscriptionsApi;
