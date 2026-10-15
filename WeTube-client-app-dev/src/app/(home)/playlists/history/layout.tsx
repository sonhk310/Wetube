import ProtectedRoute from '@/components/ProtectedRoute';
import HistoryLayout from "@/modules/playlists/layouts/HistoryLayout";

export const dynamic = "force-dynamic"; // nếu bạn dùng SSR hoặc cần dynamic routing

interface LayoutProps {
  children: React.ReactNode;
}

export default function Layout({ children }: LayoutProps) {
  return (
    <ProtectedRoute>
      <HistoryLayout>{children}</HistoryLayout>
    </ProtectedRoute>
  );
}
