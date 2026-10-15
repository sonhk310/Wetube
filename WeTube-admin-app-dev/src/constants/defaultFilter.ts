import type { IChannelFilters } from '../types/channelTypes/channel';

export const DEFAULT_FILTERS: Omit<IChannelFilters, "searchTerm"> = {
  minRevenue: 0,
  maxRevenue: Infinity,
  minViews: 0,
  maxViews: Infinity,
  minSubscribers: 0,
  maxSubscribers: Infinity,
  minReports: 0,
  maxReports: Infinity,
  status: [],
};
