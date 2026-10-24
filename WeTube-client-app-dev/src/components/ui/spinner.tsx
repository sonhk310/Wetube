import { Loader2 } from "lucide-react"

import { cn } from "@/lib/utils"

/** Vòng xoay dùng chung cho chỗ nhỏ (dropdown, hộp thoại, nút); nội dung trang dùng Skeleton. */
function Spinner({ className, ...props }: React.ComponentProps<"svg">) {
  return (
    <Loader2
      role="status"
      aria-label="Đang tải"
      className={cn("size-5 animate-spin text-gray-500", className)}
      {...props}
    />
  )
}

export { Spinner }
