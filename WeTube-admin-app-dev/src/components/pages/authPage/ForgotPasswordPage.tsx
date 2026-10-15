import React, { useState } from 'react';
import { ArrowLeft, Mail, Shield, AlertCircle, CheckCircle, Phone, MessageCircle } from 'lucide-react';

interface ForgotPasswordPageProps {
  onBackToLogin: () => void;
}

/**
 * ForgotPasswordPage Component
 * - Allows the user to request a password reset by submitting their email.
 * - Displays a success message and contact information after submitting the request.
 * - Handles email validation and loading states during form submission.
 */
const ForgotPasswordPage: React.FC<ForgotPasswordPageProps> = ({ onBackToLogin }) => {
  const [email, setEmail] = useState('');
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // Handles form submission for password recovery request
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Simple validation checks for email input
    if (!email.trim()) {
      setError('Please enter your email address');
      return;
    }

    if (!email.includes('@')) {
      setError('Please enter a valid email address');
      return;
    }

    setIsLoading(true);
    setError('');

    // Simulate API call with a timeout
    setTimeout(() => {
      setIsSubmitted(true);
      setIsLoading(false);
    }, 1500);
  };

  // If the request is submitted, display the success message and contact information
  if (isSubmitted) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900 flex items-center justify-center p-4">
        <div className="absolute inset-0 bg-[var(--login-bg-pattern)] opacity-20"></div>
        
        <div className="relative w-full max-w-md">
          <div className="bg-white rounded-2xl shadow-2xl overflow-hidden">
            {/* Header */}
            <div className="bg-gradient-to-r from-green-600 to-green-700 px-8 py-8 text-center">
              <div className="flex items-center justify-center mb-4">
                <div className="bg-white bg-opacity-20 p-3 rounded-full">
                  <CheckCircle size={32} className="text-white" />
                </div>
              </div>
              <h1 className="text-2xl font-bold text-white mb-2">Request Submitted</h1>
              <p className="text-green-100 text-sm">We have received your request</p>
            </div>

            {/* Content */}
            <div className="px-8 py-8">
              <div className="text-center mb-6">
                <div className="bg-green-50 p-4 rounded-lg mb-4">
                  <CheckCircle size={48} className="text-green-600 mx-auto mb-3" />
                  <h3 className="text-lg font-semibold text-gray-900 mb-2">
                    Request Received
                  </h3>
                  <p className="text-gray-600 text-sm">
                    We have logged your password recovery request for the email: <strong>{email}</strong>
                  </p>
                </div>

                <div className="bg-blue-50 border border-blue-200 p-4 rounded-lg mb-6">
                  <AlertCircle size={20} className="text-blue-600 mx-auto mb-2" />
                  <p className="text-blue-800 text-sm font-medium mb-2">
                    Please contact the Admin to reset your password
                  </p>
                  <p className="text-blue-700 text-xs">
                    For security reasons, the password reset must be processed by the System Administrator
                  </p>
                </div>

                {/* Contact Information */}
                <div className="space-y-3">
                  <h4 className="font-medium text-gray-900 mb-3">Admin Contact Information:</h4>
                  
                  <div className="flex items-center justify-center space-x-3 p-3 bg-gray-50 rounded-lg">
                    <Mail size={18} className="text-gray-600" />
                    <div className="text-left">
                      <p className="text-sm font-medium text-gray-900">Email</p>
                      <p className="text-sm text-gray-600">admin@webtube.com</p>
                    </div>
                  </div>

                  <div className="flex items-center justify-center space-x-3 p-3 bg-gray-50 rounded-lg">
                    <Phone size={18} className="text-gray-600" />
                    <div className="text-left">
                      <p className="text-sm font-medium text-gray-900">Hotline</p>
                      <p className="text-sm text-gray-600">1900-WEBTUBE (932-8823)</p>
                    </div>
                  </div>

                  <div className="flex items-center justify-center space-x-3 p-3 bg-gray-50 rounded-lg">
                    <MessageCircle size={18} className="text-gray-600" />
                    <div className="text-left">
                      <p className="text-sm font-medium text-gray-900">Support Ticket</p>
                      <p className="text-sm text-gray-600">support.webtube.com</p>
                    </div>
                  </div>
                </div>

                <div className="mt-6 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
                  <p className="text-yellow-800 text-xs">
                    <strong>Note:</strong> Please provide the registered email ({email}) when contacting Admin for faster support.
                  </p>
                </div>
              </div>

              <button
                onClick={onBackToLogin}
                className="w-full flex items-center justify-center px-4 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors font-medium"
              >
                <ArrowLeft size={18} className="mr-2" />
                Back to Login
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-[var(--login-bg-pattern)] opacity-20"></div>
      
      <div className="relative w-full max-w-md">
        <div className="bg-white rounded-2xl shadow-2xl overflow-hidden">
          {/* Header */}
          <div className="bg-gradient-to-r from-orange-600 to-orange-700 px-8 py-8 text-center">
            <div className="flex items-center justify-center mb-4">
              <div className="bg-white bg-opacity-20 p-3 rounded-full">
                <Shield size={32} className="text-white" />
              </div>
            </div>
            <h1 className="text-2xl font-bold text-white mb-2">Password Recovery</h1>
            <p className="text-orange-100 text-sm">Restore Admin account access</p>
          </div>

          {/* Form */}
          <div className="px-8 py-8">
            <div className="text-center mb-6">
              <div className="bg-orange-50 p-3 rounded-full w-16 h-16 mx-auto mb-4 flex items-center justify-center">
                <Mail size={24} className="text-orange-600" />
              </div>
              <h2 className="text-xl font-semibold text-gray-900 mb-2">
                Request Password Reset
              </h2>
              <p className="text-gray-600 text-sm">
                Enter your email to send a request to Admin
              </p>
            </div>

            {/* Important Notice */}
            <div className="bg-blue-50 border border-blue-200 p-4 rounded-lg mb-6">
              <div className="flex items-start space-x-3">
                <AlertCircle size={20} className="text-blue-600 flex-shrink-0 mt-0.5" />
                <div>
                  <p className="text-blue-800 text-sm font-medium mb-1">
                    Important Notice
                  </p>
                  <p className="text-blue-700 text-xs">
                    For security reasons, the Admin password reset process needs to be handled manually by the System Administrator. 
                    Please contact Admin directly after submitting your request.
                  </p>
                </div>
              </div>
            </div>

            {error && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg flex items-center">
                <AlertCircle size={16} className="text-red-600 mr-2 flex-shrink-0" />
                <span className="text-red-700 text-sm">{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Admin Account Email
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Mail size={18} className="text-gray-400" />
                  </div>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => {
                      setEmail(e.target.value);
                      if (error) setError('');
                    }}
                    className={`w-full pl-10 pr-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent transition-colors ${
                      error ? 'border-red-300 bg-red-50' : 'border-gray-300'
                    }`}
                    placeholder="admin@webtube.com"
                    disabled={isLoading}
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="w-full bg-orange-600 text-white py-3 px-4 rounded-lg hover:bg-orange-700 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:ring-offset-2 transition-colors font-medium disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center"
              >
                {isLoading ? (
                  <>
                    <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white mr-2"></div>
                    Submitting Request...
                  </>
                ) : (
                  'Send Password Reset Request'
                )}
              </button>
            </form>

            <div className="mt-6 text-center">
              <button
                onClick={onBackToLogin}
                className="inline-flex items-center text-sm text-blue-600 hover:text-blue-700 font-medium"
                disabled={isLoading}
              >
                <ArrowLeft size={16} className="mr-1" />
                Back to Login
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ForgotPasswordPage;
