import { useEffect, useRef, useState } from "react";
import type { IFilterSortPanelProps } from "../../types/channelTypes/channel";
import { FaFilter } from "react-icons/fa6";

export const FilterSortPanel: React.FC<IFilterSortPanelProps> = ({
  onSort,
  onClear,
  onOpenFilterModal,
}) => {
  const [currentSort, setCurrentSort] = useState("");
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node)
      ) {
        setIsDropdownOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleClearClick = () => {
    setCurrentSort("");
    onClear();
  };

  const handleSortChange = (sortValue: string) => {
    setCurrentSort(sortValue);
    onSort(sortValue);
    setIsDropdownOpen(false); // Close dropdown after selection
  };

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between p-4 bg-white rounded-lg shadow-sm space-y-4 sm:space-y-0">
      {/* Group Filter and Sort by buttons */}
      <div className="flex flex-col sm:flex-row gap-2">
        <button
          onClick={onOpenFilterModal}
          className="text-white bg-blue-600 hover:bg-blue-700 focus:ring-4 focus:outline-none focus:ring-blue-500 font-medium rounded-lg text-sm px-5 py-2.5 text-center inline-flex items-center dark:bg-blue-600 dark:hover:bg-blue-700 dark:focus:ring-blue-800 w-full sm:w-auto"
          aria-label="Open filter modal"
        >
          <FaFilter
            className="w-4 h-4"
            aria-hidden="true"
          />
        </button>

        {/* Dropdown Button */}
        <div
          className="relative"
          ref={dropdownRef}
        >
          <button
            id="dropdownDefaultButton"
            type="button"
            className="text-white bg-blue-600 hover:bg-blue-800 focus:ring-4 focus:outline-none focus:ring-blue-300 font-medium rounded-lg text-sm px-5 py-2.5 text-center inline-flex items-center dark:bg-blue-600 dark:hover:bg-blue-700 dark:focus:ring-blue-800 w-full sm:w-auto"
            onClick={() => setIsDropdownOpen(!isDropdownOpen)}
          >
            {currentSort
              ? {
                  "name-asc": "Name (A-Z)",
                  "name-desc": "Name (Z-A)",
                  "revenue-desc": "Revenue (High to Low)",
                  "revenue-asc": "Revenue (Low to High)",
                  "subscribers-desc": "Subscribers (High to Low)",
                  "subscribers-asc": "Subscribers (Low to High)",
                }[currentSort]
              : "Sort by..."}
            <svg
              className="w-2.5 h-2.5 ms-3"
              aria-hidden="true"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 10 6"
            >
              <path
                stroke="currentColor"
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="m1 1 4 4 4-4"
              />
            </svg>
          </button>

          {/* Dropdown Menu */}
          {isDropdownOpen && (
            <div
              id="dropdown"
              className="absolute left-1/2 transform -translate-x-1/2 z-20 bg-white divide-y divide-gray-100 rounded-lg shadow-sm w-44 mt-2"
            >
              <ul
                className="py-2 text-sm text-gray-700"
                aria-labelledby="dropdownDefaultButton"
              >
                <li>
                  <button
                    type="button"
                    className="block w-full text-left px-4 py-2 hover:bg-gray-100"
                    onClick={() => handleSortChange("")}
                  >
                    Sort by...
                  </button>
                </li>
                <li>
                  <button
                    type="button"
                    className="block w-full text-left px-4 py-2 hover:bg-gray-100"
                    onClick={() => handleSortChange("name-asc")}
                  >
                    Name (A-Z)
                  </button>
                </li>
                <li>
                  <button
                    type="button"
                    className="block w-full text-left px-4 py-2 hover:bg-gray-100"
                    onClick={() => handleSortChange("name-desc")}
                  >
                    Name (Z-A)
                  </button>
                </li>
                <li>
                  <button
                    type="button"
                    className="block w-full text-left px-4 py-2 hover:bg-gray-100"
                    onClick={() => handleSortChange("revenue-desc")}
                  >
                    Revenue (High to Low)
                  </button>
                </li>
                <li>
                  <button
                    type="button"
                    className="block w-full text-left px-4 py-2 hover:bg-gray-100"
                    onClick={() => handleSortChange("revenue-asc")}
                  >
                    Revenue (Low to High)
                  </button>
                </li>
                <li>
                  <button
                    type="button"
                    className="block w-full text-left px-4 py-2 hover:bg-gray-100"
                    onClick={() => handleSortChange("subscribers-desc")}
                  >
                    Subscribers (High to Low)
                  </button>
                </li>
                <li>
                  <button
                    type="button"
                    className="block w-full text-left px-4 py-2 hover:bg-gray-100"
                    onClick={() => handleSortChange("subscribers-asc")}
                  >
                    Subscribers (Low to High)
                  </button>
                </li>
              </ul>
            </div>
          )}
        </div>
      </div>

      <button
        onClick={handleClearClick}
        className="text-gray-700 bg-gray-200 hover:bg-gray-300 focus:ring-4 focus:outline-none focus:ring-gray-400 font-medium rounded-lg text-sm px-5 py-2.5 text-center inline-flex items-center w-full sm:w-auto"
      >
        Clear All
      </button>
    </div>
  );
};
