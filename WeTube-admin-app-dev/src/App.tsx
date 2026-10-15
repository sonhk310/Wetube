import { Route, Routes, Navigate, useLocation } from "react-router-dom";
import { useEffect, useState } from "react";
import Sidebar from "./components/layouts/Sidebar";
import TopNav from "./components/layouts/TopNav";
import { ManageChannelPage } from "./components/pages/ManageChannelPage";
import { ManageManagersPage } from "./components/pages/ManageManagersPage";
import { ViewStatsPage } from "./components/pages/ViewStatsPage";
import DashboardPage from "./components/pages/DashboardPage";
import VideoManagement from './components/pages/videoPage/VideoManagement';
import LoginPage from './components/pages/authPage/LoginPage';
import RequireRole from './components/pages/authPage/RequireRole';

function App() {
  const location = useLocation();
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);

  useEffect(() => {
    const storedUser = localStorage.getItem("user");
    if (storedUser) {
      setIsAuthenticated(true);
    }
  }, []);

  // Close sidebar when route changes on mobile
  useEffect(() => {
    setSidebarOpen(false);
  }, [location.pathname]);

  if (!isAuthenticated && location.pathname !== "/login") {
    return <Navigate to="/login" replace />;
  }

  if (isAuthenticated && location.pathname === "/login") {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {isAuthenticated && (
        <>
          {/* Sidebar */}
          <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />

          {/* Top Navigation */}
          <div className="fixed top-0 left-0 lg:left-64 right-0 bg-white z-30">
            <TopNav onMenuClick={() => setSidebarOpen(true)} />
          </div>
        </>
      )}

      {/* Main content */}
      <div
        className={`${isAuthenticated ? "pt-16 lg:ml-64 min-h-screen" : ""}`}
      >
        <div className="p-4 lg:p-6 bg-gradient-to-br from-green-300 via-blue-200 to-yellow-100 w-full dark:bg-gradient-to-br from-blue-300 via-blue-200 to-yellow-100">
          <Routes>
            <Route
              path="/login"
              element={<LoginPage onLogin={() => setIsAuthenticated(true)} />}
            />
            <Route
              path="/"
              element={
                <RequireRole allowRoles={["admin"]}>
                  <DashboardPage />
                </RequireRole>
              }
            />
            <Route path="/manage-videos" element={<VideoManagement />} />
            <Route path="/manage-channels" element={<ManageChannelPage />} />
            <Route
              path="/manage-managers"
              element={
                <RequireRole allowRoles={["admin"]}>
                  <ManageManagersPage />
                </RequireRole>
              }
            />
            <Route path="/view-stats" element={<ViewStatsPage />} />
          </Routes>
        </div>
      </div>
    </div>
  );
}

export default App;