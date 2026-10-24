"use client";

import { useEffect } from "react";
import { useProgressRouter } from "@/hooks/use-progress-router";
import { useAuth } from "@/contexts/auth-context";
import { Spinner } from "@/components/ui/spinner";

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { user, isLoading } = useAuth();
  const router = useProgressRouter();

  useEffect(() => {
    if (!isLoading && !user) {
      router.push("/");
    }
  }, [user, isLoading, router]);

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <Spinner className="size-8" />
      </div>
    );
  }

  if (user) {
    return <>{children}</>;
  }

  return null;
};

export default ProtectedRoute;
