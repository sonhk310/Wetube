import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import type { AuthInfo } from "../../types/authManager/authInfo";

/**
 * RTK Query API slice for authentication-related requests
 */
export const authApi = createApi({
  reducerPath: "authApi",

  baseQuery: fetchBaseQuery({
    baseUrl: "https://68762e68814c0dfa653b06a0.mockapi.io",
  }),

  tagTypes: ["authInfos", "auth"],

  endpoints: (builder) => ({
    // Changed to use GET method to fetch admins and filter by email/password
    Auth: builder.query<AuthInfo[], { email: string; password: string }>({
      query: ({ email, password }) => ({
        url: `/admins?email=${email}&password=${password}`,
        method: "GET",
      }),
    }),
  }),
});

// Export query hook (changed from mutation to query)
export const {
  useAuthQuery,
} = authApi;