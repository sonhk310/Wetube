// Import action type constants (make sure these are string literals in the constants file)
import type {
  GET_AUTH_INFO_FAILURE,
  GET_AUTH_INFO_REQUEST,
  GET_AUTH_INFO_SUCCESS,
} from "../../constants/authInfoConstans";

// Represents a user's basic authentication information
export interface AuthInfo {
  email: string;
  role: string;
}

// Represents the state for authentication info
export interface AuthInfoState {
  loading: boolean;
  userInfo: AuthInfo | null;
  error: string;
}

export interface AuthMessageError {
  email: string;
  password: string;
}

export interface AuthRegex {
  email: RegExp;
  password: RegExp;
}

// Action: Start fetching auth info
interface GetAuthInfoRequestAction {
  type: typeof GET_AUTH_INFO_REQUEST;
}

// Action: Auth info fetch succeeded
interface GetAuthInfoSuccessAction {
  type: typeof GET_AUTH_INFO_SUCCESS;
  payload: AuthInfo;
}

// Action: Auth info fetch failed
interface GetAuthInfoFailureAction {
  type: typeof GET_AUTH_INFO_FAILURE;
  payload: string; // error message
}

// Union type for all possible auth actions
export type AuthInfoActionTypes =
  | GetAuthInfoRequestAction
  | GetAuthInfoSuccessAction
  | GetAuthInfoFailureAction;
