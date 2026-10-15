import { API_PREFIX } from "@/constants/appConstant";
import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";

export type InteractionType = "VIEW" | "LIKE" | "COMMENT" | "SHARE";

export interface SaveInteractionRequest {
  userId: string;
  videoId: string;
  type: InteractionType;
}

export interface SaveInteractionResponse {
  message: string;
}

export const interactionApi = createApi({
  reducerPath: "interactionApi",
  baseQuery: fetchBaseQuery({
    baseUrl: `${API_PREFIX}/interactions`,
    credentials: "include",
  }),
  endpoints: (builder) => ({
    saveInteraction: builder.mutation<SaveInteractionResponse, SaveInteractionRequest>({
      query: (body) => ({
        url: "",
        method: "POST",
        body,
        headers: { "Content-Type": "application/json" },
        responseHandler: async (response) => response.text(),
      }),
    }),
  }),
});

export const { useSaveInteractionMutation } = interactionApi;
