import React from 'react';
import ConfirmationModal from './ConfirmationModalProps';
import { X, Calendar, User, Tag, Clock, Flag } from 'lucide-react';
import type { VideoInfo } from '../../../types/managerVideoTypes/videoInfo';

interface VideoDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  video: VideoInfo | null;
}

const VideoDetailModal: React.FC<VideoDetailModalProps> = ({ isOpen, onClose, video }) => {
  const [confirmationModal, setConfirmationModal] = React.useState<{
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
    onConfirm: () => {},
    showReasonInput: false,
    reasonPlaceholder: ''
  });

  if (!isOpen || !video) return null;

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
      case 'pending': return 'Pending Approval';
      case 'approved': return 'Approved';
      case 'rejected': return 'Violation';
      default: return status;
    }
  };

  const handleApproveVideo = () => {
    setConfirmationModal({
      isOpen: true,
      type: 'approve',
      title: 'Approve Video',
      message: `Are you sure you want to approve the video "${video.title}"? The video will be publicly visible on the platform.`,
      onConfirm: () => {
        // Logic to approve video
        console.log('Video approved:', video.id);
      }
    });
  };

  const handleRejectVideo = () => {
    setConfirmationModal({
      isOpen: true,
      type: 'reject',
      title: 'Reject Video',
      message: `Are you sure you want to reject the video "${video.title}"? The video will be marked as a violation and will not be displayed.`,
      onConfirm: (reason) => {
        // Logic to reject video
        console.log('Video rejected:', video.id, 'Reason:', reason);
      },
      showReasonInput: true,
      reasonPlaceholder: 'e.g., Contains inappropriate content, violates community guidelines...'
    });
  };

  const handleRemoveVideo = () => {
    setConfirmationModal({
      isOpen: true,
      type: 'remove',
      title: 'Take Down Video',
      message: `Are you sure you want to take down the video "${video.title}"? It will no longer be publicly visible.`,
      onConfirm: () => {
        // Logic to take down video
        console.log('Video removed:', video.id);
      }
    });
  };

  const handleDeleteVideo = () => {
    setConfirmationModal({
      isOpen: true,
      type: 'delete',
      title: 'Permanently Delete Video',
      message: `Are you sure you want to permanently delete the video "${video.title}"? This action cannot be undone.`,
      onConfirm: () => {
        // Logic to delete video
        console.log('Video deleted:', video.id);
        onClose();
      }
    });
  };

  const closeConfirmationModal = () => {
    setConfirmationModal(prev => ({ ...prev, isOpen: false }));
  };

  return (
    <>
      <div className="fixed inset-0 bg-opacity-50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
        <div className="bg-white rounded-xl shadow-3xl max-w-4xl w-full max-h-[90vh] overflow-y-auto">
          {/* Header */}
          <div className="flex items-center justify-between p-6 border-b border-gray-200">
            <h2 className="text-2xl font-bold text-gray-900">Video Details</h2>
            <button
              onClick={onClose}
              className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
            >
              <X size={24} className="text-gray-500" />
            </button>
          </div>

          <div className="p-6">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Video Preview */}
              <div className="space-y-4">
                <div className="relative">
                  <img 
                    src={video.thumbnail_url} 
                    alt={video.title}
                    className="w-full h-64 object-cover rounded-lg"
                  />
                  <div className="absolute bottom-3 right-3 bg-black bg-opacity-75 text-white px-2 py-1 rounded text-sm">
                    {video.duration_seconds}
                  </div>
                  <div className={`absolute top-3 left-3 px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(video.status)}`}>
                    {getStatusText(video.status)}
                  </div>
                </div>

                {/* Reports */}
                {video.reports && video.reports > 0 && (
                  <div className="bg-red-50 border border-red-200 p-4 rounded-lg">
                    <div className="flex items-center space-x-2">
                      <Flag className="text-red-600" size={20} />
                      <div>
                        <p className="text-sm text-red-600 font-medium">Reported Violations</p>
                        <p className="text-lg font-bold text-red-900">{video.reports} report(s)</p>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {/* Video Information */}
              <div className="space-y-6">
                <div>
                  <h3 className="text-xl font-bold text-gray-900 mb-2">{video.title}</h3>
                  <p className="text-gray-600">
                    {video.description || 'This is a detailed tutorial video about React Hooks. It helps you understand how to use useState, useEffect, and other hooks in React. The content is clearly explained with practical examples.'}
                  </p>
                </div>

                {/* Upload Info */}
                <div className="space-y-3">
                  <div className="flex items-center space-x-3">
                    <User className="text-gray-500" size={18} />
                    <div>
                      <p className="text-sm text-gray-500">Uploader</p>
                      <p className="font-medium text-gray-900">{video.user_id}</p>
                    </div>
                  </div>
                  <div className="flex items-center space-x-3">
                    <Calendar className="text-gray-500" size={18} />
                    <div>
                      <p className="text-sm text-gray-500">Upload Date</p>
                      <p className="font-medium text-gray-900">{new Date(video.upload_date).toLocaleDateString('en-GB')}</p>
                    </div>
                  </div>
                  <div className="flex items-center space-x-3">
                    <Clock className="text-gray-500" size={18} />
                    <div>
                      <p className="text-sm text-gray-500">Duration</p>
                      <p className="font-medium text-gray-900">{video.duration_seconds}</p>
                    </div>
                  </div>
                </div>

                {/* Tags */}
                <div>
                  <p className="text-sm text-gray-500 mb-2">Tags</p>
                  <div className="flex flex-wrap gap-2">
                    {video.tags.map((tag, index) => (
                      <span key={index} className="inline-flex items-center px-3 py-1 bg-blue-100 text-blue-800 text-sm rounded-full">
                        <Tag size={12} className="mr-1" />
                        {tag}
                      </span>
                    ))}
                  </div>
                </div>

                {/* Action Buttons */}
                <div className="flex space-x-3 pt-4 border-t border-gray-200">
                  {video.status === 'pending' && (
                    <>
                      <button 
                        onClick={handleApproveVideo}
                        className="flex-1 bg-green-600 text-white py-2 px-4 rounded-lg hover:bg-green-700 transition-colors"
                      >
                        Approve
                      </button>
                      <button 
                        onClick={handleRejectVideo}
                        className="flex-1 bg-red-600 text-white py-2 px-4 rounded-lg hover:bg-red-700 transition-colors"
                      >
                        Reject
                      </button>
                    </>
                  )}
                  {video.status === 'approved' && (
                    <button 
                      onClick={handleRemoveVideo}
                      className="flex-1 bg-orange-600 text-white py-2 px-4 rounded-lg hover:bg-orange-700 transition-colors"
                    >
                      Take Down
                    </button>
                  )}
                  <button 
                    onClick={handleDeleteVideo}
                    className="flex-1 bg-red-600 text-white py-2 px-4 rounded-lg hover:bg-red-700 transition-colors"
                  >
                    Delete Permanently
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

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
          confirmationModal.type === 'reject' ? 'Reject' :
          confirmationModal.type === 'delete' ? 'Delete Permanently' :
          'Take Down'
        }
      />
    </>
  );
};

export default VideoDetailModal;
