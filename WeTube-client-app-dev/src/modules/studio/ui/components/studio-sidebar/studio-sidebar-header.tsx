import {
  SidebarHeader,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "@/components/ui/sidebar";
import { Skeleton } from "@/components/ui/skeleton";
import UserAvatar from "@/components/user-avatar";
import { useAuth } from "@/contexts/auth-context";
import Link from "next/link";

export const StudioSidebarHeader = () => {
  const { state } = useSidebar();

  const { user, isLoading } = useAuth();

  if (isLoading || !user) {
    return (
      <SidebarHeader className="flex items-center justify-center pb-4">
        <Skeleton className="size-[112px] rounded-full" />
        <div className="flex flex-col items-center mt-2 gap-y-2">
          <Skeleton className="w-[80px] h-4" />
          <Skeleton className="w-[100px] h-4" />
        </div>
      </SidebarHeader>
    );
  }

  if (state === "collapsed") {
    return (
      <SidebarMenuItem>
        <SidebarMenuButton
          tooltip="Hồ sơ của bạn"
          asChild
        >
          <Link
            prefetch
            href={`/channel/${user.channelId}`}
          >
            <UserAvatar
              imageUrl={user.picture}
              name={user.name}
              size="sm"
            />
            <span className="text-sm">Hồ sơ của bạn</span>
          </Link>
        </SidebarMenuButton>
      </SidebarMenuItem>
    );
  }

  return (
    <SidebarHeader className="flex items-center justify-center pb-4">
      <Link
        prefetch
        href={`/channel/${user.channelId}`}
      >
        <UserAvatar
          imageUrl={user?.picture}
          name={user?.name ?? "Người dùng"}
          className="size-[112px] hover:opacity-80 transition-opacity"
        />
      </Link>
      <div className="flex flex-col items-center mt-2 gap-y-2">
        <p className="text-sm font-medium">Hồ sơ của bạn</p>
        <p className="text-xs text-muted-foreground">{user.name}</p>
      </div>
    </SidebarHeader>
  );
};
