import type React from "react";
import type { IChannelDetailsModalProps } from "../../types/channelTypes/channel";
import { useGetChannelInfoQuery } from "../../services/api/channelApi";
import { getErrorMessage } from "../../utils/helpers";
import { useEffect } from 'react';

export const ChannelDetailsModal: React.FC<IChannelDetailsModalProps> = ({
  isOpen,
  onClose,
  channelId,
}) => {
  const {
    data: channel,
    error,
    isLoading,
    isFetching,
  } = useGetChannelInfoQuery(channelId!, {
    skip: !channelId,
  });

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = "hidden"; // Disable scrolling
    } else {
      document.body.style.overflow = ""; // Re-enable scrolling
    }

    // Cleanup function to ensure scroll is re-enabled when component unmounts or isOpen becomes false
    return () => {
      document.body.style.overflow = "";
    };
  }, [isOpen]);

  if (!isOpen) return null; // Only render if modal is open

  // Loading state
  let modalContent;
  if (isLoading || isFetching) {
    modalContent = (
      <div className="flex flex-col items-center justify-center p-6 text-gray-700">
        <style>
          {`
            @keyframes spin {
              0% { transform: rotate(0deg); }
              100% { transform: rotate(360deg); }
            }
            .big-spinner {
              border: 6px solid rgba(55, 55, 224, 0.3); 
              border-top: 6px solid #3942beff; 
              border-radius: 50%;
              width: 50px;
              height: 50px;
              animation: spin 1s linear infinite;
              display: inline-block;
              vertical-align: middle;
            }
          `}
        </style>
        <div className="big-spinner mb-4"></div>
      </div>
    );
  } else if (error) {
    const errorMessage = getErrorMessage(error);
    modalContent = (
      <div className="flex flex-col items-center justify-center p-6 text-red-600">
        <p className="text-lg font-semibold mb-4">
          Error loading channel details:
        </p>
        <p className="text-sm text-center mb-6">{errorMessage}</p>
        <button
          onClick={onClose}
          className="px-6 py-3 bg-blue-600 text-white font-semibold rounded-lg shadow-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 transition duration-300 ease-in-out"
        >
          Close
        </button>
      </div>
    );
  } else if (!channel) {
    modalContent = (
      <div className="flex flex-col items-center justify-center p-6 text-gray-700">
        <p className="text-lg font-semibold mb-4">Channel details not found.</p>
        <button
          onClick={onClose}
          className="px-6 py-3 bg-blue-600 text-white font-semibold rounded-lg shadow-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 transition duration-300 ease-in-out"
        >
          Close
        </button>
      </div>
    );
  } else {
    // Display channel details
    modalContent = (
      <>
        <div className="flex justify-between items-center border-b border-gray-300 pb-2 mb-4">
          <h2 className="text-xl font-semibold text-gray-800">
            Channel Details
          </h2>
          <button
            onClick={onClose}
            className="text-gray-500 hover:text-gray-700 text-2xl font-bold"
          >
            &times;
          </button>
        </div>
        <div className="grid grid-cols-1 gap-2 text-gray-700">
          <div className="border border-gray-300 p-2 rounded-md">
            <strong>Name:</strong> {channel.channelName}
          </div>
          <div className="border border-gray-300 p-2 rounded-md">
            <strong>Email:</strong> {channel.email}
          </div>
          <div className="border border-gray-300 p-2 rounded-md">
            <strong>Revenue:</strong> {channel.revenue.toLocaleString("en-US")}{" "}
            USD
          </div>
          <div className="border border-gray-300 p-2 rounded-md">
            <strong>Views:</strong> {channel.views.toLocaleString()}
          </div>
          <div className="border border-gray-300 p-2 rounded-md">
            <strong>Subscribers:</strong> {channel.subscribers.toLocaleString()}
          </div>
          <div className="border border-gray-300 p-2 rounded-md">
            <strong>Reports:</strong> {channel.reports}
          </div>
          <div className="border border-gray-300 p-2 rounded-md">
            <strong>Status:</strong> {channel.status}
          </div>
        </div>
        <div className="flex justify-end mt-6">
          <button
            onClick={onClose}
            className="px-6 py-3 bg-blue-600 text-white font-semibold rounded-lg shadow-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 transition duration-300 ease-in-out"
          >
            Close
          </button>
        </div>
      </>
    );
  }

  // Display channel details
  return (
    <div
      className="fixed inset-0 bg-opacity-40 backdrop-blur-sm flex items-center justify-center z-50 p-4"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div
        className="max-w-lg w-full p-6 rounded-lg bg-white shadow-2xl"
        onClick={(e) => e.stopPropagation()} 
      >
        {modalContent}
      </div>
    </div>
  );
};
