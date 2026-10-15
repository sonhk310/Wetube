import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import type { managerData } from "../../types/managerTypes/managerInfo";

export const managerApi = createApi({
  reducerPath: "managerApi",
  baseQuery: fetchBaseQuery({
    baseUrl: "https://687076897ca4d06b34b6db6f.mockapi.io/api/v1/",
  }),
  tagTypes: ["ManagerInfo", "ManagerVideos", "Managers"],
  endpoints: (builder) => ({
    // Get list of managers
    getManagers: builder.query<managerData[], void>({
      query: () => "Manager",
      providesTags: (result) =>
        result
          ? [
              ...result.map(({ id }) => ({ type: "ManagerInfo" as const, id })),
              { type: "Managers" },
            ]
          : [{ type: "Managers" }],
    }),

    // Get single manager info
    getManagerInfo: builder.query<managerData, string>({
      query: (id) => `Manager/${id}`,
      providesTags: (_result, _error, id) => [
        { type: "ManagerInfo", id },
        { type: "Managers" },
      ],
    }),

    // Add manager
    addManager: builder.mutation<managerData, Partial<managerData>>({
      query: (data) => ({
        url: "Manager",
        method: "POST",
        body: data,
      }),
      invalidatesTags: [{ type: "Managers" }],
    }),

    // Update manager info
    updateManagerInfo: builder.mutation<
      managerData,
      { id: string; data: Partial<managerData> }
    >({
      query: ({ id, data }) => ({
        url: `Manager/${id}`,
        method: "PUT",
        body: data,
      }),
      invalidatesTags: (_result, _error, { id }) => [
        { type: "ManagerInfo", id },
        { type: "Managers" },
      ],
    }),

    // Delete manager
    deleteManager: builder.mutation<{ success: boolean }, string>({
      query: (id) => ({
        url: `Manager/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: (_result, _error, id) => [
        { type: "ManagerInfo", id },
        { type: "Managers" },
      ],
    }),

    // Approve manager
    approveManager: builder.mutation<void, string>({
      query: (id) => ({
        url: `/Manager/${id}`,
        method: "PATCH",
        body: { status: "Approved" },
      }),
      invalidatesTags: (_result, _error, id) => [
        { type: "ManagerInfo", id },
        { type: "Managers" },
      ],
    }),

    // Reject manager
    rejectManager: builder.mutation<void, string>({
      query: (id) => ({
        url: `/Manager/${id}`,
        method: "PATCH",
        body: { status: "Rejected" },
      }),
      invalidatesTags: (_result, _error, id) => [
        { type: "ManagerInfo", id },
        { type: "Managers" },
      ],
    }),
  }),
});

export const {
  useGetManagersQuery,
  useAddManagerMutation,
  useDeleteManagerMutation,
  useGetManagerInfoQuery,
  useUpdateManagerInfoMutation,
  useApproveManagerMutation,
  useRejectManagerMutation,
} = managerApi;
