import React, { useCallback, useEffect, useMemo, useState } from "react";
import type {
  IChannel,
  IFilterModalProps,
  IChannelFilters,
} from "../../types/channelTypes/channel";
import {
  revenueRanges,
  viewsRanges,
  subscribersRanges,
  reportsRanges,
} from "../../constants/filterRanges";
import { useGetAllChannelsQuery } from "../../services/api/channelApi";
import { RangeSection } from "./FilterSection";
import { DEFAULT_FILTERS } from "../../constants/defaultFilter";
import { FaRegWindowClose } from "react-icons/fa";
import { StatusSection } from "./StatusSection";

const CALCULATION_DELAY_MS = 200;

export const FilterModal: React.FC<IFilterModalProps> = ({
  isOpen,
  onClose,
  currentFilters,
  onApplyFilters,
}) => {
  const [modalFilters, setModalFilters] =
    useState<Omit<IChannelFilters, "searchTerm">>(DEFAULT_FILTERS);
  const [resultsCount, setResultsCount] = useState<number>(0);
  const [isCalculatingCount, setIsCalculatingCount] = useState<boolean>(false);

  const { data: allChannels, isLoading: isLoadingChannels } =
    useGetAllChannelsQuery();

  useEffect(() => {
    if (isOpen) {
      document.body.classList.add("no-scroll");
    } else {
      document.body.classList.remove("no-scroll");
    }
    // Cleanup on unmount or when isOpen changes
    return () => {
      document.body.classList.remove("no-scroll");
    };
  }, [isOpen]);

  useEffect(() => {
    if (isOpen) {
      setModalFilters({
        minRevenue: currentFilters.minRevenue,
        maxRevenue: currentFilters.maxRevenue,
        minViews: currentFilters.minViews,
        maxViews: currentFilters.maxViews,
        minSubscribers: currentFilters.minSubscribers,
        maxSubscribers: currentFilters.maxSubscribers,
        minReports: currentFilters.minReports,
        maxReports: currentFilters.maxReports,
        status: currentFilters.status,
      });
    }
  }, [isOpen, currentFilters]);

  const isFilterActive = useMemo(() => {
    const {
      minRevenue,
      maxRevenue,
      minViews,
      maxViews,
      minSubscribers,
      maxSubscribers,
      minReports,
      maxReports,
      status,
    } = modalFilters;
    return (
      minRevenue > DEFAULT_FILTERS.minRevenue ||
      maxRevenue < DEFAULT_FILTERS.maxRevenue ||
      minViews > DEFAULT_FILTERS.minViews ||
      maxViews < DEFAULT_FILTERS.maxViews ||
      minSubscribers > DEFAULT_FILTERS.minSubscribers ||
      maxSubscribers < DEFAULT_FILTERS.maxSubscribers ||
      minReports > DEFAULT_FILTERS.minReports ||
      maxReports < DEFAULT_FILTERS.maxReports ||
      status.length > 0
    );
  }, [modalFilters]);

  const filterChannels = useCallback(
    (channels: IChannel[]): IChannel[] => {
      let filtered = [...channels];
      const {
        minRevenue,
        maxRevenue,
        minViews,
        maxViews,
        minSubscribers,
        maxSubscribers,
        minReports,
        maxReports,
        status,
      } = modalFilters;

      if (
        minRevenue > DEFAULT_FILTERS.minRevenue ||
        maxRevenue < DEFAULT_FILTERS.maxRevenue
      ) {
        filtered = filtered.filter(
          (channel) =>
            channel.revenue >= minRevenue && channel.revenue <= maxRevenue
        );
      }
      if (
        minViews > DEFAULT_FILTERS.minViews ||
        maxViews < DEFAULT_FILTERS.maxViews
      ) {
        filtered = filtered.filter(
          (channel) => channel.views >= minViews && channel.views <= maxViews
        );
      }
      if (
        minSubscribers > DEFAULT_FILTERS.minSubscribers ||
        maxSubscribers < DEFAULT_FILTERS.maxSubscribers
      ) {
        filtered = filtered.filter(
          (channel) =>
            channel.subscribers >= minSubscribers &&
            channel.subscribers <= maxSubscribers
        );
      }
      if (
        minReports > DEFAULT_FILTERS.minReports ||
        maxReports < DEFAULT_FILTERS.maxReports
      ) {
        filtered = filtered.filter(
          (channel) =>
            channel.reports >= minReports && channel.reports <= maxReports
        );
      }
      if (status.length > 0) {
        filtered = filtered.filter((channel) =>
          status.includes(channel.status)
        );
      }

      return filtered;
    },
    [modalFilters]
  );

  useEffect(() => {
    if (!isOpen) {
      setIsCalculatingCount(false);
      return;
    }

    if (!allChannels || isLoadingChannels) {
      setResultsCount(0);
      setIsCalculatingCount(false);
      return;
    }

    if (!isFilterActive) {
      setResultsCount(0);
      setIsCalculatingCount(false);
      return;
    }

    setIsCalculatingCount(true);
    const timer = setTimeout(() => {
      const filteredChannels = filterChannels(allChannels);
      setResultsCount(filteredChannels.length);
      setIsCalculatingCount(false);
    }, CALCULATION_DELAY_MS);

    return () => clearTimeout(timer);
  }, [isOpen, allChannels, isLoadingChannels, isFilterActive, filterChannels]);

  const handleApply = useCallback(() => {
    onApplyFilters({ ...currentFilters, ...modalFilters });
    onClose();
  }, [onApplyFilters, onClose, currentFilters, modalFilters]);

  const handleClearModalFilters = useCallback(() => {
    setModalFilters(DEFAULT_FILTERS);
    onApplyFilters({ ...currentFilters, ...DEFAULT_FILTERS });
  }, [onApplyFilters, currentFilters]);

  const updateFilterRange = useCallback(
    (
      minKey: keyof Omit<IChannelFilters, "searchTerm" | "status">,
      maxKey: keyof Omit<IChannelFilters, "searchTerm" | "status">,
      newMin: number,
      newMax: number
    ) => {
      setModalFilters((prev) => ({
        ...prev,
        [minKey]: newMin,
        [maxKey]: newMax,
      }));
    },
    []
  );

  const updateStatusFilter = useCallback((statuses: string[]) => {
    setModalFilters((prev) => ({
      ...prev,
      status: statuses,
    }));
  }, []);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 backdrop-blur-sm flex items-center justify-center z-[100] p-4"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div
        className="bg-white rounded-lg shadow-xl max-w-3xl w-full p-4 overflow-y-auto max-h-[90vh]"
        onClick={(e) => e.stopPropagation()}
      >
        <style>
          {`
            @keyframes spin {
              0% { transform: rotate(0deg); }
              100% { transform: rotate(360deg); }
            }
            .spinner {
              border: 2px solid rgba(255, 255, 255, 0.3);
              border-top: 2px solid #fff;
              border-radius: 50%;
              width: 16px;
              height: 16px;
              animation: spin 1s linear infinite;
              display: inline-block;
              vertical-align: middle;
              margin-right: 8px;
            }
            .no-scroll {
              overflow: hidden;
            }
          `}
        </style>
        <div className="flex justify-between items-center pb-3 mb-3">
          <h2 className="text-xl font-bold text-gray-800">Filter Options</h2>
          <button
            onClick={onClose}
            className="text-gray-500 hover:text-gray-900 text-xl font-bold"
            aria-label="Close filter modal"
          >
            <FaRegWindowClose
              className="w-5 h-5"
              aria-hidden="true"
            />
          </button>
        </div>

        <RangeSection
          title="Revenue"
          minValue={modalFilters.minRevenue}
          maxValue={modalFilters.maxRevenue}
          minKey="minRevenue"
          maxKey="maxRevenue"
          ranges={revenueRanges}
          onUpdate={updateFilterRange}
        />
        <RangeSection
          title="Views"
          minValue={modalFilters.minViews}
          maxValue={modalFilters.maxViews}
          minKey="minViews"
          maxKey="maxViews"
          ranges={viewsRanges}
          onUpdate={updateFilterRange}
        />
        <RangeSection
          title="Subscribers"
          minValue={modalFilters.minSubscribers}
          maxValue={modalFilters.maxSubscribers}
          minKey="minSubscribers"
          maxKey="maxSubscribers"
          ranges={subscribersRanges}
          onUpdate={updateFilterRange}
        />
        <RangeSection
          title="Reports"
          minValue={modalFilters.minReports}
          maxValue={modalFilters.maxReports}
          minKey="minReports"
          maxKey="maxReports"
          ranges={reportsRanges}
          onUpdate={updateFilterRange}
        />

        <StatusSection
          title="Status"
          selectedStatuses={modalFilters.status}
          onUpdate={updateStatusFilter}
        />

        {/* Action Buttons at the bottom of the modal */}
        <div className="flex justify-end pt-3">
          <div className="flex flex-col sm:flex-row gap-2">
            <button
              onClick={handleClearModalFilters}
              className="text-gray-700 bg-gray-200 hover:bg-gray-300 focus:ring-4 focus:outline-none focus:ring-gray-400 font-medium rounded-lg text-sm px-5 py-2.5 text-center inline-flex items-center w-full sm:w-auto"
            >
              Clear
            </button>
            <button
              onClick={handleApply}
              className="text-white bg-blue-600 hover:bg-blue-700 focus:ring-4 focus:outline-none focus:ring-blue-500 font-medium rounded-lg text-sm px-5 py-2.5 text-center inline-flex items-center w-full sm:w-auto"
              disabled={isLoadingChannels || isCalculatingCount}
            >
              {isCalculatingCount ? (
                <span className="spinner"></span>
              ) : isFilterActive ? (
                `${resultsCount} results`
              ) : (
                "Apply Filters"
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
