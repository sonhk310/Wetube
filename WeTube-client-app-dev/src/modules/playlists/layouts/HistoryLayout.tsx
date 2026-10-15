import HistorySidebar from "@/modules/playlists/ui/sections/SideBar";
interface HistoryLayoutProps {
  children: React.ReactNode;
}

export default function HistoryLayout({ children }: HistoryLayoutProps) {
  return (
    <div className="flex flex-col md:flex-row ">
      {/* Main content */}

      <main className="no-scrollbar w-full lg:w-2/3 lg:h-[92vh] lg:overflow-y-auto">
        {children}
      </main>

      {/* Desktop: Sidebar cố định bên trái */} 
      <div className="hidden lg:block lg:w-1/3 lg:h-full">
        <HistorySidebar />
      </div>
    </div>
  );
}
