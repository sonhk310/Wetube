import { type ReactNode } from "react";

interface Props {
  children: ReactNode;
  colSpan?: 1 | 2 | 3 | 4;
  rowSpan?: 1 | 2 | 3 | 4;
  className?: string;
}

export default function DashboardCard({
  children,
  colSpan = 1,
  rowSpan = 1,
  className = "",
}: Props) {
  return (
    <div
      className={`
        rounded-2xl bg-white dark:bg-slate-800 shadow-sm p-6 flex flex-col
        col-span-${colSpan} row-span-${rowSpan}
        ${className}
      `}
    >
      {children}
    </div>
  );
}
