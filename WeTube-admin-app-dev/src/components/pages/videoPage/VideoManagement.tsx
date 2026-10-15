import React, { useState } from 'react';
import VideoDetailModal from './VideoDetailModalProps';
import VideoEditModal from './VideoEditModalProps';
import ConfirmationModal from './ConfirmationModalProps';

import {
  Check,
  X,
  Eye,
  Edit,
  Trash2,
  Filter,
  Tag,
  Clock,
  AlertTriangle
} from 'lucide-react';
import type { VideoInfo } from '../../../types/managerVideoTypes/videoInfo';
import { useGetVideosQuery, useUpdateVideoMutation, useDeleteVideoMutation } from '../../../services/api/videoApi';


const VideoManagement: React.FC = () => {
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const { data: videos = [] } = useGetVideosQuery();

  const [selectedVideo, setSelectedVideo] = useState<VideoInfo | null>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);

  const [confirmationModal, setConfirmationModal] = useState<{
    isOpen: boolean;
    type: 'approve' | 'reject' | 'delete' | 'remove';
    title: string;
    message: string;
    onConfirm: (reason?: string) => void;
    showReasonInput?: boolean;
    reasonPlaceholder?: string;
  }>({
    isOpen: false,
    type: 'approve',
    title: '',
    message: '',
    onConfirm: () => { },
    showReasonInput: false,
    reasonPlaceholder: ''
  });

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'pending': return 'bg-yellow-100 text-yellow-800';
      case 'approved': return 'bg-green-100 text-green-800';
      case 'rejected': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'pending': return 'Chờ duyệt';
      case 'approved': return 'Đã phê duyệt';
      case 'rejected': return 'Vi phạm';
      default: return status;
    }
  };

  const filteredVideos = filterStatus === 'all'
    ? videos
    : videos.filter(video => video.status === filterStatus);

  const handleViewVideo = (video: VideoInfo) => {
    setSelectedVideo(video);
    setIsDetailModalOpen(true);
  };

  const handleEditVideo = (video: VideoInfo) => {
    setSelectedVideo(video);
    setIsEditModalOpen(true);
  };


  const [updateVideo] = useUpdateVideoMutation();

  const handleSaveVideo = async (updatedVideo: VideoInfo) => {
    try {
      await updateVideo({
        id: updatedVideo.id,
        data: {
          title: updatedVideo.title,
          description: updatedVideo.description,
          thumbnail_url: updatedVideo.thumbnail_url,
          duration_seconds: updatedVideo.duration_seconds,
          upload_date: updatedVideo.upload_date,
          tags: updatedVideo.tags,
          status: updatedVideo.status
        }
      }).unwrap();
      setIsEditModalOpen(false);
    } catch (error) {
      console.error("Error while updating video:", error);
    }
  };



  const handleApproveVideo = (video: VideoInfo) => {
    setConfirmationModal({
      isOpen: true,
      type: 'approve',
      title: 'Video Approval',
      message: `Are you sure you want to approve the video? "${video.title}"? The video will be displayed publicly on the platform.`,
      onConfirm: async () => {
        try {
          await updateVideo({
            id: video.id,
            data: {
              status: 'approved'
            }
          }).unwrap();
          console.log(`Video ${video.id} has been approved`);
        } catch (error) {
          console.error('Error approving video:', error);
        }
      }


    });
  };


  const handleRejectVideo = (video: VideoInfo) => {
    setConfirmationModal({
      isOpen: true,
      type: 'reject',
      title: 'Reject video',
      message: `Are you sure you want to reject the video? "${video.title}"? The video will be marked as a violation and will not be displayed.`,
      onConfirm: async (reason) => {
        try {
          await updateVideo({
            id: video.id,
            data: { status: 'rejected' },
          }).unwrap();
          console.log(`Video ${video.id} rejected for the following reasons: ${reason}`);
        } catch (error) {
          console.error('Error when rejecting video:', error);
        }
      },
      showReasonInput: true,
      reasonPlaceholder: 'For example: Video contains inappropriate content, violates community regulations...'
    });
  };

  const [deleteVideo] = useDeleteVideoMutation();

  const handleDeleteVideo = (video: VideoInfo) => {
    setConfirmationModal({
      isOpen: true,
      type: 'delete',
      title: 'Delete video permanently',
      message: `Are you sure you want to permanently delete the video? "${video.title}"? This action cannot be undone.`,
      onConfirm: async () => {
        try {
          await deleteVideo(video.id).unwrap();
          console.log(`Video ${video.id} has been deleted.`);
        } catch (error) {
          console.error('Error while deleting video:', error);
        }

      }
    });
  };

  const handleRemoveVideo = (video: VideoInfo) => {
    setConfirmationModal({
      isOpen: true,
      type: 'remove',
      title: 'Take down video',
      message: `Are you sure you want to take down the video? "${video.title}"? The video will no longer be publicly visible.`,
      onConfirm: async () => {
        try {
          await updateVideo({
            id: video.id,
            data: { ...video, status: 'rejected' }
          }).unwrap();

          console.log(`Video ${video.id} has been taken down`);
        } catch (error) {
          console.error("Error removing video:", error);
        }
      }
    });
  };


  const closeConfirmationModal = () => {
    setConfirmationModal(prev => ({ ...prev, isOpen: false }));
  };

  return (
    <div className="flex-1 overflow-y-auto p-6">

      {/* Filters */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mt-6">
        <div className="flex flex-wrap items-center gap-4">
          <div className="flex items-center space-x-2">
            <Filter size={18} className="text-gray-500" />
            <span className="text-sm font-medium text-gray-700">Filter by status:</span>
          </div>
          <div className="flex flex-wrap gap-2">
            {[
              { key: 'all', label: 'All', count: videos.length },
              { key: 'pending', label: 'Pending approval', count: videos.filter(v => v.status === 'pending').length },
              { key: 'approved', label: 'Approved', count: videos.filter(v => v.status === 'approved').length },
              { key: 'rejected', label: 'Violate', count: videos.filter(v => v.status === 'rejected').length }
            ].map(filter => (
              <button
                key={filter.key}
                onClick={() => setFilterStatus(filter.key)}
                className={`px-3 py-1 rounded-full text-sm transition-colors ${filterStatus === filter.key
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
              >
                {filter.label} ({filter.count})
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Videos Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6 mt-6">
        {filteredVideos.map((video) => (
          <div key={video.id} className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden hover:shadow-md transition-shadow">
            <div className="relative">
              <img
                src={video.thumbnail_url}
                alt={video.title}
                className="w-full h-48 object-cover"
              />
              <div className="absolute bottom-2 right-2 bg-black bg-opacity-75 text-white px-2 py-1 rounded text-xs">
                {video.duration_seconds}
              </div>
              <div className={`absolute top-2 left-2 px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(video.status)}`}>
                {getStatusText(video.status)}
              </div>
            </div>

            <div className="p-4">
              <h3 className="font-semibold text-gray-900 mb-2 line-clamp-2">{video.title}</h3>

              <div className="flex items-center text-sm text-gray-500 mb-3">
                <Clock size={14} className="mr-1" />
                <span>{video.upload_date}</span>            
              </div>
              <div className="flex items-center text-sm text-gray-600 mb-3">
                <span>Uploaded by: {video.user_id}</span>
              </div>

              <div className="flex flex-wrap gap-1 mb-4">
                {video.tags.map((tag, index) => (
                  <span key={index} className="inline-flex items-center px-2 py-1 bg-blue-100 text-blue-800 text-xs rounded-full">
                    <Tag size={12} className="mr-1" />
                    {tag}
                  </span>
                ))}
              </div>

              <div className="flex justify-between items-center">
                <div className="flex space-x-2">
                  {video.status === 'pending' && (
                    <>
                      <button
                        onClick={() => handleApproveVideo(video)}
                        className="flex items-center px-3 py-1 bg-green-600 text-white rounded text-sm hover:bg-green-700 transition-colors"
                      >
                        <Check size={14} className="mr-1" />
                        Browse
                      </button>
                      <button
                        onClick={() => handleRejectVideo(video)}
                        className="flex items-center px-3 py-1 bg-red-600 text-white rounded text-sm hover:bg-red-700 transition-colors"
                      >
                        <X size={14} className="mr-1" />
                        Refuse
                      </button>
                    </>
                  )}
                  {video.status === 'approved' && (
                    <button
                      onClick={() => handleRemoveVideo(video)}
                      className="flex items-center px-3 py-1 bg-orange-600 text-white rounded text-sm hover:bg-orange-700 transition-colors"
                    >
                      <X size={14} className="mr-1" />
                      Take down
                    </button>
                  )}
                </div>

                <div className="flex space-x-2">
                  <button
                    onClick={() => handleViewVideo(video)}
                    className="p-2 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded transition-colors"
                    title="See details"
                  >
                    <Eye size={16} />
                  </button>
                  <button
                    onClick={() => handleEditVideo(video)}
                    className="p-2 text-gray-500 hover:text-green-600 hover:bg-green-50 rounded transition-colors"
                    title="Edit"
                  >
                    <Edit size={16} />
                  </button>
                  <button
                    onClick={() => handleDeleteVideo(video)}
                    className="p-2 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded transition-colors"
                    title="Delete video"
                  >
                    <Trash2 size={16} />
                  </button>
                  <button
                    className="p-2 text-gray-500 hover:text-orange-600 hover:bg-orange-50 rounded transition-colors"
                    title="Report a violation"
                  >
                    <AlertTriangle size={16} />
                  </button>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Modals */}
      <VideoDetailModal
        isOpen={isDetailModalOpen}
        onClose={() => setIsDetailModalOpen(false)}
        video={selectedVideo}
      />

      <VideoEditModal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        video={selectedVideo}
        onSave={handleSaveVideo}
      />

      <ConfirmationModal
        isOpen={confirmationModal.isOpen}
        onClose={closeConfirmationModal}
        onConfirm={confirmationModal.onConfirm}
        title={confirmationModal.title}
        message={confirmationModal.message}
        type={confirmationModal.type}
        showReasonInput={confirmationModal.showReasonInput}
        reasonPlaceholder={confirmationModal.reasonPlaceholder}
        confirmText={
          confirmationModal.type === 'approve' ? 'Approve' :
            confirmationModal.type === 'reject' ? 'Refuse' :
              confirmationModal.type === 'delete' ? 'Permanently Deleted' :
                'Take down'
        }
      />
    </div>
  );
};

export default VideoManagement;