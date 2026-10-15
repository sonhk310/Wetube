import React from "react";
import { useGetManagerInfoQuery, useGetManagersQuery } from "../../../services/api/managerApi";

interface Props {
  managerId: string | null;
}

const ManagerInfoPage: React.FC<Props> = ({ managerId }) => {
  const { data: managerData, isLoading: isManagerLoading, error: managerError } = useGetManagerInfoQuery(managerId!, { skip: !managerId });
  const { isLoading: isManagersLoading, error: managersError } = useGetManagersQuery();

  if (isManagerLoading || isManagersLoading)
    return <p className="text-center text-gray-700">Đang tải thông tin...</p>;
  if (managerError || managersError)
    return <p className="text-center text-red-600">Có lỗi xảy ra khi tải dữ liệu!</p>;

  return (
    <div className="p-6 bg-white rounded-xl shadow-md max-w-5xl mx-auto space-y-6">
      <h2 className="text-2xl font-bold mb-4 text-gray-900">Manager Information</h2>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 items-center">
        <div className="space-y-3 text-gray-700">
          <p><strong>Name:</strong> {managerData?.name || "N/A"}</p>
          <p><strong>Email:</strong> {managerData?.email || "N/A"}</p>
          <p><strong>Phone:</strong> {managerData?.phone || "N/A"}</p>
          <p><strong>Date of Birth:</strong> {managerData?.dateOfBirth || "N/A"}</p>
          <p><strong>Address:</strong> {managerData?.address || "N/A"}</p>
          <p><strong>Gender:</strong> {managerData?.gender || "N/A"}</p>
          <p><strong>Role:</strong> {managerData?.role || "N/A"}</p>
          <p><strong>Status:</strong> 
            <span className={`ml-2 font-semibold ${
              managerData?.status === "Approved" ? "text-green-600" :
              managerData?.status === "Rejected" ? "text-red-600" :
              managerData?.status === "Banned" ? "text-red-500" :
              "text-yellow-600"
            }`}>
              {managerData?.status}
            </span>
          </p>
          <p><strong>Daily Reports:</strong> {managerData?.dailyReports ?? 0}</p>
          <p><strong>Processed Reports:</strong> {managerData?.processedReports ?? 0}</p>
          <p><strong>Unprocessed Reports:</strong> {managerData?.unprocessedReports ?? 0}</p>
        </div>

        <div className="flex justify-center">
          <img
            src={managerData?.avatarUrl || "https://via.placeholder.com/150"}
            alt="Manager Avatar"
            className="w-40 h-40 rounded-full object-cover border-4 border-blue-200 shadow-md"
          />
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <h3 className="text-xl font-semibold text-gray-800 mb-3">Recent Processed Reports</h3>
          {managerData?.recentProcessedReports?.length ? (
            <ul className="space-y-2">
              {managerData.recentProcessedReports.slice(0, 3).map((report) => (
                <li key={report.id} className="p-3 bg-gray-100 rounded-md shadow-sm">
                  <p><strong>Title:</strong> {report.title}</p>
                  <p><strong>Status:</strong> {report.status}</p>
                  <p><strong>Date:</strong> {new Date(report.processedDate).toLocaleDateString()}</p>
                  {report.description && <p><strong>Description:</strong> {report.description}</p>}
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-gray-500">No processed reports.</p>
          )}
        </div>

        <div>
          <h3 className="text-xl font-semibold text-gray-800 mb-3">Recent Processed Videos</h3>
          {managerData?.recentProcessedVideos?.length ? (
            <ul className="space-y-2">
              {managerData.recentProcessedVideos.slice(0, 5).map((video) => (
                <li key={video.id} className="p-3 bg-gray-100 rounded-md shadow-sm">
                  <p><strong>Title:</strong> {video.title}</p>
                  <p><strong>Status:</strong> {video.status}</p>
                  <p><strong>Date:</strong> {new Date(video.processedDate).toLocaleDateString()}</p>
                  {video.url && (
                    <p><strong>Link:</strong> 
                      <a href={video.url} target="_blank" rel="noopener noreferrer" className="ml-1 text-blue-600 hover:underline">View</a>
                    </p>
                  )}
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-gray-500">No processed videos.</p>
          )}
        </div>
      </div>
    </div>
  );
};

export default ManagerInfoPage;
