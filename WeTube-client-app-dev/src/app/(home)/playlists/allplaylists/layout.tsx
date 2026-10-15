import ProtectedRoute from '@/components/ProtectedRoute';
import Allplaylistlayout from "@/modules/playlists/layouts/Allplaylist-layout";

export const dynamic = "force-dynamic";

interface LayoutProps {
  children: React.ReactNode;
}
export default function Layout({ children }: LayoutProps) {
  return (
    <ProtectedRoute>
      <Allplaylistlayout>{children}</Allplaylistlayout>
    </ProtectedRoute>
  );
}
