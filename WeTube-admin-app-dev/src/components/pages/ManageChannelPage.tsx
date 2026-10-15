import { useState } from "react";
import type {
  IChannel,
  IChannelFilters,
} from "../../types/channelTypes/channel";
import { FilterSortPanel } from '../manageChannel/FilterSortPanel';
import { SearchBar } from '../manageChannel/SearchBar';
import { ChannelList } from '../manageChannel/ChannelList';
import { FilterModal } from '../manageChannel/FilterModal';
import { ChannelDetailsModal } from '../manageChannel/ChannelDetailsModal';
import { DEFAULT_FILTERS } from '../../constants/defaultFilter';

export const ManageChannelPage = () => {
  const [filters, setFilters] = useState<IChannelFilters>({
    searchTerm: "",
    ...DEFAULT_FILTERS,
  });
  const [sort, setSort] = useState("");
  const [isFilterModalOpen, setIsFilterModalOpen] = useState(false);
  const [selectedChannelId, setSelectedChannelId] = useState<string | null>(
    null
  ); // Changed to store ID
  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false);

  const handleSearchTermChange = (newTerm: string) => {
    setFilters((prev) => ({ ...prev, searchTerm: newTerm }));
  };

  const handleSort = (newSort: string) => {
    setSort(newSort);
  };

  const handleShowChannelDetail = (channelId: string) => {
    // Now accepts channelId
    setSelectedChannelId(channelId);
    setIsDetailsModalOpen(true);
  };

  const handleClearAll = () => {
    setFilters({
      searchTerm: "",
      ...DEFAULT_FILTERS,
    });
    setSort("");
  };

  const handleApplyModalFilters = (newFilters: IChannelFilters) => {
    setFilters(newFilters);
  };

  const handleEditChannel = (channel: IChannel) => {
    console.log("Edit Channel:", channel);
    alert(`Editing channel: ${channel.channelName}`);
  };

  return (
    <>
      <div className="p-6 max-w-7xl mx-auto border border-gray-300 rounded-lg bg-gray-50 font-inter">

        <div className="mb-6">
          <FilterSortPanel
            onSort={handleSort}
            onClear={handleClearAll}
            onOpenFilterModal={() => setIsFilterModalOpen(true)}
          />
        </div>

        <div className="mb-6">
          <SearchBar
            currentSearchTerm={filters.searchTerm}
            onSearchChange={handleSearchTermChange}
          />
        </div>

        <ChannelList
          filters={filters}
          sort={sort}
          onShowDetail={handleShowChannelDetail} // Pass handler to ChannelList
          onEditChannel={handleEditChannel} // Pass handler to ChannelList
        />

        <ChannelDetailsModal
          isOpen={isDetailsModalOpen}
          onClose={() => setIsDetailsModalOpen(false)}
          channelId={selectedChannelId} // Pass only the selectedChannelId
        />
      </div>

      <FilterModal
        isOpen={isFilterModalOpen}
        onClose={() => setIsFilterModalOpen(false)}
        currentFilters={filters}
        onApplyFilters={handleApplyModalFilters}
      />
    </>
  );
};
