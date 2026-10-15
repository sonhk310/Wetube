import React from 'react';
import { Shield } from 'lucide-react';

interface RequireRoleProps {
  allowRoles: string[];
  children: React.ReactNode;
}

const RequireRole: React.FC<RequireRoleProps> = ({ allowRoles, children }) => {
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  const userRole = user.role || 'user';

  if (!allowRoles.includes(userRole)) {
    return (
      <div className="min-h-[400px] flex items-center justify-center">
        <div className="text-center">
          <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <Shield className="w-8 h-8 text-red-600" />
          </div>
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Access Denied</h2>
          <p className="text-gray-600 mb-4">
            You don't have permission to access this page.
          </p>
          <p className="text-sm text-gray-500">
            Required role: {allowRoles.join(', ')} | Your role: {userRole}
          </p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
};

export default RequireRole;