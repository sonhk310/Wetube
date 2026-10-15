export interface IFetchBaseQueryError {
  status?: number;
  data?: { message?: string };
  error?: string;
}

export interface ISerializedError {
  message?: string;
  name?: string;
  stack?: string;
  code?: string;
}
