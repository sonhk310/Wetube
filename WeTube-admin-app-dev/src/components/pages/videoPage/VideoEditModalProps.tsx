import React, { useState } from 'react';
import { X, Save, Tag, Plus, Trash2 } from 'lucide-react';
import type { VideoInfo } from '../../../types/managerVideoTypes/videoInfo';

interface VideoEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  video: VideoInfo | null;
  onSave: (updatedVideo: VideoInfo) => void;
}

const VideoEditModal: React.FC<VideoEditModalProps> = ({ isOpen, onClose, video, onSave }) => {
  const [editedVideo, setEditedVideo] = useState(video);
  const [newTag, setNewTag] = useState('');

  React.useEffect(() => {
    setEditedVideo(video);
  }, [video]);

  if (!isOpen || !editedVideo) return null;

  const handleSave = () => {
    onSave(editedVideo);
    onClose();
  };

  const addTag = () => {
    if (newTag.trim() && !editedVideo.tags.includes(newTag.trim())) {
      setEditedVideo({
        ...editedVideo,
        tags: [...editedVideo.tags, newTag.trim()]
      });
      setNewTag('');
    }
  };

  const removeTag = (tagToRemove: string) => {
    setEditedVideo({
      ...editedVideo,
      tags: editedVideo.tags.filter(tag => tag !== tagToRemove)
    });
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      addTag();
    }
  };

  return (
    <div className="fixed inset-0 bg-opacity-50 backdrop-blur-md flex items-center justify-center p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-3xl w-full p-4 overflow-y-auto max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200">
          <h2 className="text-2xl font-bold text-gray-900">Edit Video</h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X size={24} className="text-gray-500" />
          </button>
        </div>

        <div className="p-6 space-y-6">
          {/* Video Preview */}
          <div className="flex items-center space-x-4">
            <img
              src={editedVideo.thumbnail_url}
              alt={editedVideo.title}
              className="w-32 h-20 object-cover rounded-lg"
            />
            <div>
              <p className="text-sm text-gray-500">Video ID: {editedVideo.id}</p>
              <p className="text-sm text-gray-500">Duration: {editedVideo.duration_seconds}</p>
              <p className="text-sm text-gray-500">Uploader: {editedVideo.user_id}</p>
            </div>
          </div>

          {/* Title */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Video Title
            </label>
            <input
              type="text"
              value={editedVideo.title}
              onChange={(e) => setEditedVideo({ ...editedVideo, title: e.target.value })}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="Enter video title..."
            />
          </div>

          {/* Description */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Video Description
            </label>
            <textarea
              value={editedVideo.description || ''}
              onChange={(e) => setEditedVideo({ ...editedVideo, description: e.target.value })}
              rows={4}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="Enter video description..."
            />
          </div>

          {/* Status */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Status
            </label>
            <select
              value={editedVideo.status}
              onChange={(e) =>
                setEditedVideo({
                  ...editedVideo,
                  status: e.target.value as 'pending' | 'approved' | 'rejected',
                })
              }

              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="pending">Pending</option>
              <option value="approved">Approved</option>
              <option value="rejected">Rejected</option>
            </select>
          </div>

          {/* Tags */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Tags
            </label>
            <div className="space-y-3">
              {/* Add new tag */}
              <div className="flex space-x-2">
                <input
                  type="text"
                  value={newTag}
                  onChange={(e) => setNewTag(e.target.value)}
                  onKeyPress={handleKeyPress}
                  className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="Add a new tag..."
                />
                <button
                  onClick={addTag}
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center"
                >
                  <Plus size={16} className="mr-1" />
                  Add
                </button>
              </div>

              {/* Existing tags */}
              <div className="flex flex-wrap gap-2">
                {editedVideo.tags.map((tag, index) => (
                  <div key={index} className="inline-flex items-center px-3 py-1 bg-blue-100 text-blue-800 text-sm rounded-full">
                    <Tag size={12} className="mr-1" />
                    {tag}
                    <button
                      onClick={() => removeTag(tag)}
                      className="ml-2 text-blue-600 hover:text-blue-800"
                    >
                      <Trash2 size={12} />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Thumbnail URL */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Thumbnail URL
            </label>
            <input
              type="url"
              value={editedVideo.thumbnail_url}
              onChange={(e) => setEditedVideo({ ...editedVideo, thumbnail_url: e.target.value })}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="https://example.com/thumbnail.jpg"
            />
          </div>

          {/* Action Buttons */}
          <div className="flex space-x-3 pt-4 border-t border-gray-200">
            <button
              onClick={onClose}
              className="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              className="flex-1 bg-blue-600 text-white py-2 px-4 rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center"
            >
              <Save size={16} className="mr-2" />
              Save Changes
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default VideoEditModal;
