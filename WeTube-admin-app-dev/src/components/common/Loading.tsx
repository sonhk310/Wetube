import { Loader2 } from "lucide-react";

export default function Loading() {
  return (
    <div className="flex items-center justify-center h-screen w-full bg-white">
      <Loader2 className="w-10 h-10 text-blue-500 animate-spin" />
    </div>
  );
}
