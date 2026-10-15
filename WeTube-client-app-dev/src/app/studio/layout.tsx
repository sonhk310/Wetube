import ProtectedRoute from "@/components/ProtectedRoute";
import StudioLayout from "@/modules/studio/ui/layouts/studio-layout";

export default function Layout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <ProtectedRoute>
      <StudioLayout>{children}</StudioLayout>
    </ProtectedRoute>
  );
}
