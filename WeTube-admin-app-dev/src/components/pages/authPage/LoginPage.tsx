import React, { useState, useEffect } from 'react';
import { Eye, EyeOff, Shield, Video, Lock, User, AlertCircle, CheckCircle } from 'lucide-react';
import { useAuthQuery } from '../../../services/api/authApi';
import ForgotPasswordPage from './ForgotPasswordPage';
import { REGEX } from '../../../constants/AuthFormConstant';

interface LoginPageProps {
  onLogin: (data: { email: string; role: string }) => void;
}

const LoginPage: React.FC<LoginPageProps> = ({ onLogin }) => {
  const [showForgotPassword, setShowForgotPassword] = useState(false);
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState<{ email?: string; password?: string; general?: string }>({});
  const [shouldAuth, setShouldAuth] = useState(false);
  const [touched, setTouched] = useState<{ email: boolean; password: boolean }>({ email: false, password: false });

  const { data, isLoading, error } = useAuthQuery(
    { email: formData.email, password: formData.password },
    { skip: !shouldAuth }
  );

  useEffect(() => {
    if (!shouldAuth) return;

    if (data) {
      if (data.length > 0) {
        const user = data[0];
        onLogin({ email: user.email, role: user.role });
        localStorage.setItem('user', JSON.stringify(user));
      } else {
        setErrors({ general: 'Incorrect email or password' });
      }
      setShouldAuth(false);
    }
  }, [data, onLogin, shouldAuth]);

  useEffect(() => {
    if (shouldAuth && error) {
      console.error('Login error:', error);
      setErrors({ general: 'Login failed. Please try again.' });
      setShouldAuth(false);
    }
  }, [error, shouldAuth]);

  // Validate individual field
  const validateField = (name: string, value: string) => {
    if (!value.trim()) {
      return name === 'email' ? 'Please enter your email' : 'Please enter your password';
    }

    if (!REGEX[name as keyof typeof REGEX]?.test(value)) {
      if (name === 'email') {
        return 'Invalid email format';
      }
    }
    return '';
  };

  // Check if form is valid
  const isFormValid = () => {
    const emailError = validateField('email', formData.email);
    const passwordError = validateField('password', formData.password);
    return !emailError && !passwordError && formData.email.trim() && formData.password.trim();
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    
    // Clear general error when user starts typing
    if (errors.general) {
      setErrors(prev => ({ ...prev, general: undefined }));
    }

    // Real-time validation if field has been touched
    if (touched[name as keyof typeof touched]) {
      const fieldError = validateField(name, value);
      setErrors(prev => ({ ...prev, [name]: fieldError }));
    }
  };

  const handleBlur = (e: React.FocusEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setTouched(prev => ({ ...prev, [name]: true }));
    
    const fieldError = validateField(name, value);
    setErrors(prev => ({ ...prev, [name]: fieldError }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    // Mark all fields as touched
    setTouched({ email: true, password: true });
    
    // Validate all fields
    const emailError = validateField('email', formData.email);
    const passwordError = validateField('password', formData.password);
    
    const newErrors: typeof errors = {};
    if (emailError) newErrors.email = emailError;
    if (passwordError) newErrors.password = passwordError;

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    // If validation passes, proceed with authentication
    setErrors({});
    setShouldAuth(true);
  };

  const handleForgotPassword = () => {
    setErrors({});
    setShowForgotPassword(true);
  };

  // Password requirement checker
  const getPasswordRequirements = () => {
    const password = formData.password;
    return [
      { label: 'At least 8 characters', valid: password.length >= 8 },
      { label: 'Contains lowercase letter', valid: /(?=.*[a-z])/.test(password) },
      { label: 'Contains uppercase letter', valid: /(?=.*[A-Z])/.test(password) },
      { label: 'Contains a number', valid: /(?=.*\d)/.test(password) },
      { label: 'Contains a special character', valid: /(?=.*[!@#$%^&*(),.?":{}|<>_\-\\[\]\/~`+=])/.test(password) }
    ];
  };

  if (showForgotPassword) {
    return <ForgotPasswordPage onBackToLogin={() => setShowForgotPassword(false)} />;
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900 flex items-center justify-center p-4">
      <div className="relative w-full max-w-md">
        <div className="bg-white rounded-2xl shadow-2xl overflow-hidden">
          
          {/* Header */}
          <div className="bg-gradient-to-r from-blue-600 to-blue-700 px-8 py-8 text-center">
            <div className="flex items-center justify-center mb-4">
              <div className="bg-white bg-opacity-20 p-3 rounded-full">
                <Video size={32} className="text-white" />
              </div>
            </div>
            <h1 className="text-2xl font-bold text-white mb-2">WebTube Admin</h1>
            <p className="text-blue-100 text-sm">Video platform admin system</p>
          </div>

          {/* Body */}
          <div className="px-8 py-8">
            <div className="flex items-center justify-center mb-6">
              <div className="bg-blue-50 p-3 rounded-full">
                <Shield size={24} className="text-blue-600" />
              </div>
            </div>

            <h2 className="text-xl font-semibold text-gray-900 text-center mb-6">Log in to the system</h2>

            {/* General Error */}
            {errors.general && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg flex items-center">
                <AlertCircle size={16} className="text-red-600 mr-2" />
                <span className="text-red-700 text-sm">{errors.general}</span>
              </div>
            )}

            {/* Form */}
            <div className="space-y-6">
              {/* Email */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Email <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <User size={18} className="text-gray-400" />
                  </div>
                  <input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    className={`w-full pl-10 pr-4 py-3 border rounded-lg transition-colors focus:outline-none focus:ring-2 ${
                      errors.email 
                        ? 'border-red-300 bg-red-50 focus:ring-red-500 focus:border-red-500' 
                        : 'border-gray-300 focus:ring-blue-500 focus:border-blue-500'
                    }`}
                    placeholder="Enter your email"
                    disabled={isLoading}
                  />
                  {formData.email && !errors.email && touched.email && (
                    <CheckCircle size={18} className="absolute right-3 top-1/2 transform -translate-y-1/2 text-green-500" />
                  )}
                </div>
                {errors.email && (
                  <div className="flex items-center mt-2 text-red-600 text-sm">
                    <AlertCircle className="w-4 h-4 mr-1" />
                    {errors.email}
                  </div>
                )}
              </div>

              {/* Password */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Password <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Lock size={18} className="text-gray-400" />
                  </div>
                  <input
                    type={showPassword ? 'text' : 'password'}
                    name="password"
                    value={formData.password}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    className={`w-full pl-10 pr-12 py-3 border rounded-lg transition-colors focus:outline-none focus:ring-2 ${
                      errors.password 
                        ? 'border-red-300 bg-red-50 focus:ring-red-500 focus:border-red-500' 
                        : 'border-gray-300 focus:ring-blue-500 focus:border-blue-500'
                    }`}
                    placeholder="Enter your password"
                    disabled={isLoading}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute inset-y-0 right-0 pr-3 flex items-center hover:text-gray-600"
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
                {errors.password && (
                  <div className="flex items-center mt-2 text-red-600 text-sm">
                    <AlertCircle className="w-4 h-4 mr-1" />
                    {errors.password}
                  </div>
                )}

                {/* Password Requirements */}
                {formData.password && touched.password && (
                  <div className="mt-3 p-3 bg-gray-50 rounded-lg">
                    <p className="text-xs font-medium text-gray-700 mb-2">Password requirements:</p>
                    <div className="space-y-1">
                      {getPasswordRequirements().map((req, index) => (
                        <div key={index} className={`flex items-center text-xs ${req.valid ? 'text-green-600' : 'text-red-500'}`}>
                          <div className={`w-2 h-2 rounded-full mr-2 ${req.valid ? 'bg-green-500' : 'bg-red-500'}`}></div>
                          {req.label}
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Options */}
              <div className="flex items-center justify-between">
                <label className="flex items-center">
                  <input type="checkbox" className="h-4 w-4 text-blue-600 rounded border-gray-300 focus:ring-blue-500" />
                  <span className="ml-2 text-sm text-gray-600">Remember me</span>
                </label>
                <button
                  type="button"
                  onClick={handleForgotPassword}
                  className="text-sm text-blue-600 hover:text-blue-700 font-medium transition-colors"
                >
                  Forgot password?
                </button>
              </div>

              {/* Submit */}
              <button
                type="submit"
                onClick={handleSubmit}
                disabled={!isFormValid() || isLoading}
                className={`w-full py-3 px-4 rounded-lg font-medium transition-all duration-200 ${
                  isFormValid() && !isLoading
                    ? 'bg-blue-600 hover:bg-blue-700 text-white shadow-lg hover:shadow-xl transform hover:-translate-y-0.5'
                    : 'bg-gray-300 text-gray-500 cursor-not-allowed'
                }`}
              >
                {isLoading ? (
                  <div className="flex items-center justify-center">
                    <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white mr-2"></div>
                    Logging in...
                  </div>
                ) : (
                  'Login'
                )}
              </button>

              {/* Form Status Indicator */}
              {formData.email && formData.password && (
                <div className="text-center">
                  <div className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-medium ${
                    isFormValid() 
                      ? 'bg-green-100 text-green-800' 
                      : 'bg-orange-100 text-orange-800'
                  }`}>
                    {isFormValid() ? (
                      <>
                        <CheckCircle size={14} className="mr-1" />
                        Form is valid
                      </>
                    ) : (
                      <>
                        <AlertCircle size={14} className="mr-1" />
                        Please check your information
                      </>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="text-center mt-6 text-white text-sm opacity-75">
          © 2024 WebTube
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
