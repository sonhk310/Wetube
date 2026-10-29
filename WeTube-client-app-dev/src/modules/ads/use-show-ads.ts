import { useAuth } from "@/contexts/auth-context";

/** Khách và người chưa có Premium thấy quảng cáo; Premium không thấy. Chờ biết trạng thái đăng nhập rồi mới hiện. */
export function useShowAds() {
  const { isPremium, isLoading } = useAuth();
  return !isLoading && !isPremium;
}
