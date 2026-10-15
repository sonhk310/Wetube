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
import { useRouter } from 'next/navigation';

const AuthButton = () => {
    const { user, isLoading, login, logout } = useAuth();
    const router = useRouter();

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
                Profile
              </DropdownMenuItem>
              <DropdownMenuItem onSelect={logout}>Logout</DropdownMenuItem>
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
            Sign In
        </Button>
    );
};

export default AuthButton;