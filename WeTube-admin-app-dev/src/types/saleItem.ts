// types/sale-item.ts
import type { SVGProps, JSX } from "react";

export interface SaleItem {
  label: string;
  value: string;
  growth: string;

  /* lớp màu nền thẻ */
  bgColorClass: string;

  /* lớp màu nền icon */
  iconBgColorClass: string;

  /* Icon “thuần” từ Iconify/React-Icons… */
  icon?: string;

  /* SVG React component tự viết */
  SvgIcon?: (props: SVGProps<SVGSVGElement>) => JSX.Element;
}
