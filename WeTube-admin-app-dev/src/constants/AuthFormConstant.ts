import type { AuthMessageError, AuthRegex } from "@/types/authManager/authInfo";

export const MESSAGE_ERROR: AuthMessageError = {
    email: "Email error",
    password: "Password error",
}

export const REGEX: AuthRegex = {
    email: /^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$/,
    password: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@])[A-Za-z\d@$!%*?&]{8,}$/,
}