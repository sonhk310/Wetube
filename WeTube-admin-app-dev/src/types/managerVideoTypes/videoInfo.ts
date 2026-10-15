import type { GET_VIDEO_INFO_FAILURE, GET_VIDEO_INFO_SUCCESS, GET_VIDEO_INFO_REQUEST } from "../../constants/videoInfoConstans";

export interface VideoInfo {
    id: string;
    user_id: string;
    title: string;
    description: string;
    video_url: string;
    thumbnail_url: string;
    duration_seconds: number;
    upload_date: string;
    visibility: string;
    status: 'pending' | 'approved' | 'rejected';
    tags: string[];
    stats: string;
    admin_id?: string;
    reports?: number;
}

export interface VideoInfoState {
    loading: boolean;
    videoInfo: VideoInfo | null;
    error: string | null;
}

interface GetVideoInfoRequestAction {
    type: typeof GET_VIDEO_INFO_REQUEST
}

interface GetVideoInfoSuccessAction {
    type: typeof GET_VIDEO_INFO_SUCCESS;
    payload: VideoInfo;
}

interface GetVideoInfoFailureAction {
    type: typeof GET_VIDEO_INFO_FAILURE;
    payload: string;
}

export type VideoInfoActionTypes = 
    | GetVideoInfoSuccessAction
    | GetVideoInfoRequestAction
    | GetVideoInfoFailureAction;
