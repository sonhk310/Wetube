import { Icon } from "@iconify/react";
import type { SaleItem } from "../../../../data/sales";

const SummaryCard = ({ item }: { item: SaleItem }) => {
  const {
    value,
    label,
    growth,
    bgColorClass,
    iconBgColorClass,
    icon,
    SvgIcon,
  } = item;

  return (
    <div className={`rounded-xl p-5 ${bgColorClass}`}>
      {/* Icon wrapper */}
      <div
        className={`${iconBgColorClass} h-10 rounded-full flex items-center justify-center mb-4`}
      >
        {SvgIcon ? (
          <SvgIcon className="w-6 h-6 text-white block" />
        ) : icon ? (
          <Icon icon={icon} className="w-5 h-5 text-white block" />
        ) : null}
      </div>

      {/* Text */}
      <h3 className="text-2xl font-bold text-gray-900 mb-1">{value}</h3>
      <p className="text-sm text-gray-700 mb-1">{label}</p>
      <p className="text-xs text-gray-500">Last day {growth}</p>
    </div>
  );
};

export default SummaryCard;
