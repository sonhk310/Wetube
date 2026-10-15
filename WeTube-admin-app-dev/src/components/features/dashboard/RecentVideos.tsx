import React from "react";
import {
  MoreHorizontal,
  Eye,
  ThumbsUp,
  MessageCircle,
} from "lucide-react";

interface Video {
  id: string;
  title: string;
  thumbnail: string;
  views: string;
  likes: string;
  comments: string;
  publishedAt: string;
  duration: string;
  status: "published" | "processing" | "draft";
}

const RecentVideos: React.FC = () => {
  const videos: Video[] = [
    {
      id: "1",
      title: "How to Create Engaging YouTube Content in 2024",
      thumbnail:
        "https://images.pexels.com/photos/3992949/pexels-photo-3992949.jpeg?auto=compress&cs=tinysrgb&w=400",
      views: "125K",
      likes: "4.2K",
      comments: "384",
      publishedAt: "2 hours ago",
      duration: "12:45",
      status: "published",
    },
    {
      id: "2",
      title: "YouTube Analytics Deep Dive - Understanding Your Audience",
      thumbnail:
        "https://images.pexels.com/photos/3184292/pexels-photo-3184292.jpeg?auto=compress&cs=tinysrgb&w=400",
      views: "87K",
      likes: "3.1K",
      comments: "267",
      publishedAt: "1 day ago",
      duration: "18:23",
      status: "published",
    },
    {
      id: "3",
      title: "Building a Successful YouTube Channel from Scratch",
      thumbnail:
        "https://images.pexels.com/photos/3992930/pexels-photo-3992930.jpeg?auto=compress&cs=tinysrgb&w=400",
      views: "0",
      likes: "0",
      comments: "0",
      publishedAt: "Processing...",
      duration: "15:30",
      status: "processing",
    },
  ];

  const getStatusColor = (status: string) => {
    switch (status) {
      case "published":
        return "bg-green-100 text-green-800";
      case "processing":
        return "bg-yellow-100 text-yellow-800";
      case "draft":
        return "bg-gray-100 text-gray-800";
      default:
        return "bg-gray-100 text-gray-800";
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100">
      <div className="p-6 border-gray-100">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-gray-900">Recent Videos</h2>
          <button className="text-blue-600 hover:text-blue-700 text-sm font-medium">
            View All
          </button>
        </div>
      </div>

      <div className="p-6">
        <div className="space-y-4">
          {videos.map((video) => (
            <div
              key={video.id}
              className="flex items-center space-x-4 p-4 rounded-lg hover:bg-gray-50 transition-colors"
            >
              <div className="relative flex-shrink-0">
                <img
                  src={video.thumbnail}
                  alt={video.title}
                  className="w-24 h-16 object-cover rounded-lg"
                />
                <div className="absolute bottom-1 right-1 bg-black bg-opacity-75 text-white text-xs px-1 py-0.5 rounded">
                  {video.duration}
                </div>
              </div>

              <div className="flex-1 min-w-0">
                <h3 className="font-medium text-gray-900 truncate">
                  {video.title}
                </h3>
                <div className="flex items-center space-x-4 mt-1 text-sm text-gray-500">
                  <div className="flex items-center space-x-1">
                    <Eye className="w-4 h-4" />
                    <span>{video.views}</span>
                  </div>
                  <div className="flex items-center space-x-1">
                    <ThumbsUp className="w-4 h-4" />
                    <span>{video.likes}</span>
                  </div>
                  <div className="flex items-center space-x-1">
                    <MessageCircle className="w-4 h-4" />
                    <span>{video.comments}</span>
                  </div>
                  <span>•</span>
                  <span>{video.publishedAt}</span>
                </div>
              </div>

              <div className="flex items-center space-x-2">
                <span
                  className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(
                    video.status
                  )}`}
                >
                  {video.status.charAt(0).toUpperCase() + video.status.slice(1)}
                </span>
                <button className="p-1 text-gray-400 hover:text-gray-600 rounded">
                  <MoreHorizontal className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default RecentVideos;
