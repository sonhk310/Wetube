import type { IFetchBaseQueryError, ISerializedError } from '../types/errorTypes';

function isFetchBaseQueryError(error: unknown): error is IFetchBaseQueryError {
  return (
    typeof error === "object" &&
    error !== null &&
    "status" in error &&
    (typeof (error as IFetchBaseQueryError).status === "number" ||
      typeof (error as IFetchBaseQueryError).status === "string")
  );
}

function isSerializedError(error: unknown): error is ISerializedError {
  return (
    typeof error === "object" &&
    error !== null &&
    "message" in error &&
    typeof (error as ISerializedError).message === "string"
  );
}

export const getErrorMessage = (error: unknown): string => {
  if (isFetchBaseQueryError(error)) {
    if (
      error.data &&
      typeof error.data === "object" &&
      "message" in error.data &&
      typeof (error.data as { message: string }).message === "string"
    ) {
      return (error.data as { message: string }).message;
    } else if (typeof error.error === "string") {
      return error.error;
    } else if (error.status) {
      return `API Error: Status ${error.status}`;
    }
  } else if (error instanceof Error) {
    return error.message;
  } else if (isSerializedError(error)) {
    return error.message!;
  }

  return "An unknown error occurred.";
};