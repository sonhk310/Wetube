export interface IChannel {
  id: string;
  channelName: string;
  email: string;
  revenue: number;
  views: number;
  subscribers: number;
  reports: number;
  status: "Active" | "Banned" | "Strike" | "Warning";
  cover: string;
  avatar: string;
}

export interface ISearchBarProps {
  currentSearchTerm: string;
  onSearchChange: (term: string) => void;
}

export interface IFilterSortPanelProps {
  onSort: (sort: string) => void;
  onClear: () => void;
  onOpenFilterModal: () => void;
}

export interface IChannelListProps {
  filters: IChannelFilters;
  sort: string;
  onShowDetail: (channelId: string) => void; // New prop for showing details
  onEditChannel: (channel: IChannel) => void;
}

export interface IChannelFilters {
  searchTerm: string;
  minRevenue: number;
  maxRevenue: number;
  minViews: number;
  maxViews: number;
  minSubscribers: number;
  maxSubscribers: number;
  minReports: number;
  maxReports: number;
  status: string[];
}

export interface IFilterModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentFilters: IChannelFilters;
  onApplyFilters: (filters: IChannelFilters) => void;
}

export interface IChannelDetailsModalProps {
  isOpen: boolean;
  onClose: () => void;
  channelId: string | null;
}

export interface IRangeSectionProps {
  title: string;
  minValue: number;
  maxValue: number;
  minKey: keyof Omit<IChannelFilters, "searchTerm" | "status">;
  maxKey: keyof Omit<IChannelFilters, "searchTerm" | "status">;
  ranges: { label: string; min: number; max: number }[];
  onUpdate: (
    minKey: keyof Omit<IChannelFilters, "searchTerm" | "status">,
    maxKey: keyof Omit<IChannelFilters, "searchTerm" | "status">,
    min: number,
    max: number
  ) => void;
}
