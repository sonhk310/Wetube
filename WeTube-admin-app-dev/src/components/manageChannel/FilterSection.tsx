import { memo } from 'react';
import type { IRangeSectionProps } from '../../types/channelTypes/channel';
import { DEFAULT_FILTERS } from '../../constants/defaultFilter';


export const RangeSection: React.FC<IRangeSectionProps> = memo(
  ({ title, minValue, maxValue, minKey, maxKey, ranges, onUpdate }) => {
    return (
      <div className="mb-4">
        <h3 className="text-base font-semibold text-gray-700 mb-2">{title}</h3>
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 mb-3">
          {ranges.map((range, index) => (
            <button
              key={`${minKey}-${index}`} // Unique key for each button
              onClick={() => {
                // Determine if the current range is already selected
                const isSelected =
                  minValue === range.min && maxValue === range.max;
                // Toggle logic: if selected, unselect (set to default), otherwise select the range
                onUpdate(
                  minKey,
                  maxKey,
                  isSelected ? DEFAULT_FILTERS[minKey] : range.min, // Use DEFAULT_FILTERS for unselecting
                  isSelected ? DEFAULT_FILTERS[maxKey] : range.max
                );
              }}
              className={`px-3 py-1.5 text-sm rounded-lg border transition duration-200 ease-in-out
                ${
                  minValue === range.min && maxValue === range.max
                    ? "bg-purple-600 text-white border-purple-700 shadow-md" // Styles for selected button
                    : "bg-gray-100 text-gray-700 border-gray-300 hover:bg-gray-200" // Styles for unselected button
                }`}
            >
              {range.label}
            </button>
          ))}
        </div>
        <div className="flex items-center space-x-2">
          <input
            type="number"
            placeholder="Min"
            // Display empty string if value is 0 (default min) for better UX
            value={minValue === DEFAULT_FILTERS[minKey] ? "" : minValue}
            onChange={(e) =>
              // Update the min value, ensuring it's a number or 0 if empty
              onUpdate(
                minKey,
                maxKey,
                Number(e.target.value) || DEFAULT_FILTERS[minKey],
                maxValue
              )
            }
            className="w-1/2 p-1.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
            aria-label={`${title} minimum value`} // Accessibility label
          />
          <span className="text-sm">-</span>
          <input
            type="number"
            placeholder="Max"
            // Display empty string if value is Infinity (default max) for better UX
            value={maxValue === DEFAULT_FILTERS[maxKey] ? "" : maxValue}
            onChange={(e) =>
              // Update the max value, ensuring it's a number or Infinity if empty
              onUpdate(
                minKey,
                maxKey,
                minValue,
                Number(e.target.value) || DEFAULT_FILTERS[maxKey]
              )
            }
            className="w-1/2 p-1.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
            aria-label={`${title} maximum value`} 
          />
        </div>
      </div>
    );
  }
);