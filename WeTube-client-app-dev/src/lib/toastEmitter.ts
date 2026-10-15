import { toast, ToastOptions, Bounce } from "react-toastify";

const defaultOptions: ToastOptions = {
    position: "top-center",
    hideProgressBar: false,
    closeOnClick: true,
    pauseOnHover: true,
    draggable: true,
    theme: "light",
    transition: Bounce,
};

export const toastEmitter = {
    loading: (message: string, options: ToastOptions = {}) =>
        toast.loading(message, {
            ...defaultOptions,
            autoClose: false,
            ...options,
        }),

    success: (message: string, options: ToastOptions = {}) =>
        toast.success(message, {
            ...defaultOptions,
            autoClose: 2000,
            ...options,
        }),

    error: (message: string, options: ToastOptions = {}) =>
        toast.error(message, {
            ...defaultOptions,
            autoClose: 2000,
            ...options,
        }),

    info: (message: string, options: ToastOptions = {}) =>
        toast.info(message, {
            ...defaultOptions,
            autoClose: 2000,
            ...options,
        }),

    updateSuccess: (toastId:  string | number, message: string, options: ToastOptions = {}) =>
        toast.update(toastId, {
            render: message,
            type: "success",
            isLoading: false,
            autoClose: 2000,
            ...defaultOptions,
            ...options,
        }),

    updateError: (toastId:  string | number, message: string, options: ToastOptions = {}) =>
        toast.update(toastId, {
            render: message,
            type: "error",
            isLoading: false,
            autoClose: 2000,
            ...defaultOptions,
            ...options,
        }),
};
