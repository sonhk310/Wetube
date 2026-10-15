import type { GET_MANAGER_INFO_FAILURE, GET_MANAGER_INFO_REQUEST, GET_MANAGER_INFO_SUCCESS } from "../../constants/managerInfoConstans";

export interface managerData {
  id: string;
  name: string;
  email: string;
  phone: string;
  avatarUrl?: string;
  dateOfBirth: string;
  address: string;
  role: 'Manager' | 'Admin';
  gender: 'male' | 'female' | 'other';
  status: 'Pending' | 'Approved' | 'Rejected'| 'Banned';
  dailyReports?: number;      
  processedReports?: number;  
  unprocessedReports?: number;
  recentProcessedReports?: Report[];
  recentProcessedVideos?: Video[]; 
}
export interface Report {
  id: string;
  title: string;
  status: 'Processed';
  processedDate: string; 
  description: string;
}
export interface Video {
  id: string;
  title: string;
  status: 'Processed';
  processedDate: string; 
  url?: string;
}

export interface ManagerInfoState {
    loading: boolean;
    managerInfo: managerData | null;
    error: string | null;
}

interface GetManagerInfoRequestAction {
    type: typeof GET_MANAGER_INFO_REQUEST;
}

interface GetManagerInfoSuccessAction {
    type: typeof GET_MANAGER_INFO_SUCCESS;
    payload: managerData;
}

interface GetManagerInfoFailureAction {
    type: typeof GET_MANAGER_INFO_FAILURE;
    payload: string;
}

export type ManagerInfoActionTypes =
    | GetManagerInfoRequestAction
    | GetManagerInfoSuccessAction
    | GetManagerInfoFailureAction;