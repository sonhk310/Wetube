import {  type FC } from 'react';
import type { ISearchBarProps } from '../../types/channelTypes/channel';

export const SearchBar: FC<ISearchBarProps> = ({
  currentSearchTerm,
  onSearchChange,
}) => {
  return (
    <div className="flex items-center justify-center p-4 bg-white rounded-lg shadow-sm">
      <input
        type="text"
        placeholder="Search channels by name or email..."
        className="flex-grow p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-600 text-gray-700"
        value={currentSearchTerm}
        onChange={(e) => onSearchChange(e.target.value)}
      />
    </div>
  );
};