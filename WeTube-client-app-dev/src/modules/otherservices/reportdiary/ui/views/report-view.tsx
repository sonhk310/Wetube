import React from 'react'
import ReportStatus from '../layout/report-center';
import SupportPanel from '@/modules/otherservices/support/ui/layout/support-users';

function Reportview() {
  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white flex flex-col items-center text-center px-4 py-20">
      <ReportStatus/>
      <SupportPanel/>
    </div>
  )
}

export default Reportview;
