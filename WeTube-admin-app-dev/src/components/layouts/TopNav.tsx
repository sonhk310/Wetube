import React, { useState, useEffect, useRef } from 'react';
import { 
  Search, Bell, HelpCircle, Settings, User, LayoutDashboard, Video, Users, BarChart3, Menu, LogOut 
} from 'lucide-react';
import type { INavItem } from "../../types/navItemTypes";
import { useLocation } from 'react-router-dom';

interface TopNavProps {
  onMenuClick: () => void;
}

const navItems: INavItem[] = [
  { id: "dashboard", label: "Dashboard", icon: LayoutDashboard, to: "/" },
  { id: "manage-videos", label: "Manage Video", icon: Video, to: "/manage-videos" },
  { id: "manage-channels", label: "Manage Channel", icon: Settings, to: "/manage-channels" },
  { id: "manage-managers", label: "Manage Manager", icon: Users, to: "/manage-managers" },
  { id: "view-stats", label: "View Statistics", icon: BarChart3, to: "/view-stats" },
];

const TopNav: React.FC<TopNavProps> = ({ onMenuClick }) => {
  const location = useLocation();
  const [showDropdown, setShowDropdown] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleLogout = () => {
    console.log("User logged out!");
    setShowDropdown(false);
  };

  const renderNavItem = (item: INavItem) => {
    if (item.to !== location.pathname) return null;
    return (
      <div key={item.id} className="flex items-center space-x-2">
        <item.icon className="w-5 h-5" />
        <span className="hidden sm:inline">{item.label}</span>
      </div>
    );
  };

  return (
    <div className="bg-[#5D7B6F] border-b border-gray-200 px-4 lg:px-6 py-4 dark:bg-[#0b1f3a]">
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <button
            onClick={onMenuClick}
            className="lg:hidden p-2 text-gray-500 hover:text-white hover:bg-[#A4C3A2] rounded-lg transition-colors"
          >
            <Menu className="w-5 h-5" />
          </button>

          <h1 className="text-xl lg:text-2xl font-bold text-gray-200 dark:text-white">
            {navItems.map((item) => renderNavItem(item))}
          </h1>
        </div>

        <div className="flex items-center space-x-2 lg:space-x-4 ">
          <div className="relative hidden md:block">
            <Search className="w-5 h-5 absolute dark:text-nerual-900 left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="Search videos, analytics..."
              className="pl-10 pr-4 py-2 border border-gray-300 rounded-lg w-48 lg:w-64 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
          <button className="md:hidden p-2 text-white hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors">
            <Search className="w-5 h-5 " />
          </button>

          {/* Other buttons */}
          <div className="flex items-center space-x-1 lg:space-x-2">
            <button className="hidden sm:block p-2 text-white hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors">
              <HelpCircle className="w-5 h-5" />
            </button>

            <button className="hidden sm:block p-2 text-white hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors">
              <Settings className="w-5 h-5" />
            </button>

            <button className="p-2 text-white hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors relative">
              <Bell className="w-5 h-5" />
              <span className="absolute -top-1 -right-1 w-3 h-3 bg-red-500 rounded-full"></span>
            </button>

            {/* User Avatar + Dropdown */}
            <div
              className="relative pl-2 lg:pl-3 border-l border-gray-200"
              ref={dropdownRef}
            >
              <div
                onClick={() => setShowDropdown((prev) => !prev)}
                className="flex items-center space-x-2 lg:space-x-3 cursor-pointer"
              >
                <div className="bg-">
                  <img
                    src="/image/admin.png"
                    alt="Logo"
                    width={60}
                    height={60}
                  />
                </div>
                <div className="hidden lg:block">
                  <p className="text-sm font-medium text-white dark:text-white">
                    Admin
                  </p>
                  <p className="text-xs text-white dark:text-white">
                    Admin123@example.com
                  </p>
                </div>
              </div>

              {/* Dropdown menu */}
              {showDropdown && (
                <div className="absolute right-0 mt-2 w-40 bg-white border border-gray-200 rounded-lg shadow-lg z-50">
                  <button
                    onClick={handleLogout}
                    className="flex items-center w-full px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-lg"
                  >
                    <LogOut className="w-4 h-4 mr-2" /> Logout
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TopNav;
