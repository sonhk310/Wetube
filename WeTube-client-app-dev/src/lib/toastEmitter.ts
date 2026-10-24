import { toast, type ExternalToast } from "sonner";

/**
 * Hiện toast (Sonner của shadcn, giữa cạnh dưới màn hình; <Toaster /> gắn trong app/layout.tsx).
 * Giữ nguyên các hàm cũ để nơi gọi không phải đổi.
 */
export const toastEmitter = {
    loading: (message: string, options?: ExternalToast) => toast.loading(message, options),

    success: (message: string, options?: ExternalToast) => toast.success(message, options),

    error: (message: string, options?: ExternalToast) => toast.error(message, options),

    info: (message: string, options?: ExternalToast) => toast.info(message, options),

    /** Đổi toast đang "loading" thành thành công / lỗi. */
    updateSuccess: (toastId: string | number, message: string) => toast.success(message, { id: toastId }),

    updateError: (toastId: string | number, message: string) => toast.error(message, { id: toastId }),
};
