import HomeLayout from "@/modules/home/ui/components/video/layouts/home-layout";

interface LayoutProps {
  children: React.ReactNode;
}

/** Trang xem một buổi phát trực tiếp dùng cùng khung với trang xem video (thanh bên thu gọn). */
export default function Layout({ children }: LayoutProps) {
  return <HomeLayout drawerSidebar>{children}</HomeLayout>;
}
