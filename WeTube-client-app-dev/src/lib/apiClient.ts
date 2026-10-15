import axios, {
  AxiosError,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from "axios";
import {API_PREFIX} from "@/constants/appConstant";

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || `${API_PREFIX}`,
  withCredentials: true,
});

interface FailedRequest {
  resolve: (value: AxiosResponse | PromiseLike<AxiosResponse>) => void;
  reject: (reason?: Error) => void;
}

let isRefreshing = false;
let failedQueue: FailedRequest[] = [];

const processQueue = (error: Error | null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(apiClient(prom as never));
    }
  });
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig;

    if (
      error.response?.status === 401 &&
      originalRequest.url !== "/auth/refresh-login"
    ) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(() => apiClient(originalRequest));
      }

      isRefreshing = true;

      try {
        await apiClient.post("/auth/refresh-login");
        processQueue(null);
        return apiClient(originalRequest);
      } catch (refreshError) {
        const errorToReject =
          refreshError instanceof Error
            ? refreshError
            : new Error(String(refreshError));
        
        processQueue(refreshError as Error);

        window.dispatchEvent(new Event("auth-failure"));
        return Promise.reject(errorToReject);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(
      error instanceof Error ? error : new Error("Unknown error")
    );
  }
);

export default apiClient;
