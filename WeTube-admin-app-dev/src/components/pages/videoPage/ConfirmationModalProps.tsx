import React from 'react';
import { AlertTriangle, Check, X, Trash2 } from 'lucide-react';

interface ConfirmationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (reason?: string) => void;
  title: string;
  message: string;
  type: 'approve' | 'reject' | 'delete' | 'remove' | 'warning';
  confirmText?: string;
  cancelText?: string;
  showReasonInput?: boolean;
  reasonPlaceholder?: string;
}

const ConfirmationModal: React.FC<ConfirmationModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  type,
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  showReasonInput = false,
  reasonPlaceholder = 'Enter reason...'
}) => {
  const [reason, setReason] = React.useState('');
  const [reasonError, setReasonError] = React.useState('');

  if (!isOpen) return null;

  const getTypeConfig = () => {
    switch (type) {
      case 'approve':
        return {
          icon: Check,
          iconColor: 'text-green-600',
          iconBg: 'bg-green-100',
          confirmBg: 'bg-green-600 hover:bg-green-700',
          borderColor: 'border-green-200'
        };
      case 'reject':
        return {
          icon: X,
          iconColor: 'text-red-600',
          iconBg: 'bg-red-100',
          confirmBg: 'bg-red-600 hover:bg-red-700',
          borderColor: 'border-red-200'
        };
      case 'delete':
      case 'remove':
        return {
          icon: Trash2,
          iconColor: 'text-red-600',
          iconBg: 'bg-red-100',
          confirmBg: 'bg-red-600 hover:bg-red-700',
          borderColor: 'border-red-200'
        };
      case 'warning':
      default:
        return {
          icon: AlertTriangle,
          iconColor: 'text-yellow-600',
          iconBg: 'bg-yellow-100',
          confirmBg: 'bg-yellow-600 hover:bg-yellow-700',
          borderColor: 'border-yellow-200'
        };
    }
  };

  const config = getTypeConfig();
  const IconComponent = config.icon;

  const handleConfirm = () => {
    if (showReasonInput && !reason.trim()) {
      setReasonError('Please provide a reason for rejection');
      return;
    }
    onConfirm(reason.trim() || undefined);
    onClose();
    setReason('');
    setReasonError('');
  };

  const handleClose = () => {
    onClose();
    setReason('');
    setReasonError('');
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-2xl max-w-md w-full">
        <div className="p-6">
          {/* Icon */}
          <div className={`mx-auto flex items-center justify-center w-12 h-12 rounded-full ${config.iconBg} mb-4`}>
            <IconComponent className={`w-6 h-6 ${config.iconColor}`} />
          </div>

          {/* Title */}
          <h3 className="text-lg font-semibold text-gray-900 text-center mb-2">
            {title}
          </h3>

          {/* Message */}
          <p className="text-gray-600 text-center mb-6">
            {message}
          </p>

          {/* Reason Input */}
          {showReasonInput && (
            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Rejection reason <span className="text-red-500">*</span>
              </label>
              <textarea
                value={reason}
                onChange={(e) => {
                  setReason(e.target.value);
                  if (reasonError) setReasonError('');
                }}
                placeholder={reasonPlaceholder}
                rows={4}
                className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent resize-none ${
                  reasonError ? 'border-red-300 bg-red-50' : 'border-gray-300'
                }`}
              />
              {reasonError && (
                <p className="mt-1 text-sm text-red-600">{reasonError}</p>
              )}
              <p className="mt-1 text-xs text-gray-500">
                This reason will be sent to the user to explain why their video was rejected
              </p>
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex space-x-3">
            <button
              onClick={handleClose}
              className="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors font-medium"
            >
              {cancelText}
            </button>
            <button
              onClick={handleConfirm}
              className={`flex-1 px-4 py-2 text-white rounded-lg transition-colors font-medium ${config.confirmBg}`}
            >
              {confirmText}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ConfirmationModal;
