"use client";

import { useAuth } from '@/contexts/auth-context';
import { Button } from "@/components/ui/button";
import Image from "next/image";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger
} from "@/components/ui/dropdown-menu";
import { useProgressRouter } from "@/hooks/use-progress-router";
import { ArrowLeftRight, Crown, LogOut, User } from "lucide-react";

const AuthButton = () => {
    const { user, isLoading, login, switchAccount, logout } = useAuth();
    const router = useProgressRouter();

    if (isLoading) {
        return <div className="h-10 w-24 bg-gray-200 rounded-full animate-pulse" />;
    }

    if (user) {
        return (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                className="relative h-10 w-10 rounded-full"
              >
                <Image
                  src={user.picture}
                  alt={user.name}
                  fill
                  className="rounded-full"
                />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem
                onSelect={(e) => {
                  e.preventDefault(); 
                  router.push(`/channel/${user.channelId}`); 
                }}
              >
                <User />
                Hồ sơ
              </DropdownMenuItem>
              {/* Người đang có Premium thấy nhãn Premium cạnh logo (kèm ngày hết hạn), không lặp lại ở đây */}
              {!user.premiumUntil && (
                <DropdownMenuItem onSelect={() => router.push("/otherservices/premium")}>
                  <Crown className="text-amber-500" />
                  Dùng Premium
                </DropdownMenuItem>
              )}
              <DropdownMenuItem onSelect={switchAccount}>
                <ArrowLeftRight />
                Chuyển tài khoản
              </DropdownMenuItem>
              <DropdownMenuItem onSelect={logout}>
                <LogOut />
                Đăng xuất
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        );
    }

    return (
        <Button
            variant="ghost"
            size="lg"
            className="rounded-full px-4 py-2 flex items-center gap-2 text-black shadow-md"
            onClick={login}
        >
            Đăng nhập
        </Button>
    );
};

export default AuthButton;