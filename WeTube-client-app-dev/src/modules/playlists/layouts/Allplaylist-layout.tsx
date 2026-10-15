import HomeNavbar from '@/modules/home/ui/components/home-navbar';
import HomeSidebar from '@/modules/home/ui/components/home-sidebar';
import React from 'react'

interface AllPlaylistlayoutProps {
    children: React.ReactNode;
}
export default function Allplaylistlayout({children}: AllPlaylistlayoutProps) {
  return (
            <div className="w-full">
                <HomeNavbar />
                    <HomeSidebar />
                    <main className="flex-1 overflow-y-auto">
                        {children}
                    </main>
           
            </div>  
  )
}
