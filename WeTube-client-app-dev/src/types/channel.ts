export interface ChannelInfo {
  id: string;
  name: string;
  handle: string;
  subscriberCount: number;
  pictureUrl: string;
  bannerUrl: string;
}

export interface ChannelSection {
  id: string;
  title: string;
  orderPosition: number;
  videoCount: number;
  videos: any[] | null; 
}

export interface ChannelStructure {
  success: boolean;
  message: string;
  categories: ChannelSection[];
}

export interface UpdateCategoryOrderRequest {
  categories: {
    id: string;
    orderPosition: number;
  }[];
}
