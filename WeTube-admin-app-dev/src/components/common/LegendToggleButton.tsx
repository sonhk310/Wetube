import { Icon } from "@iconify/react";

interface LegendToggleButtonProps {
  
  name: "normal users" | "pro users";
  icon: string;  
  colorClass: string; 
  legend: Record<string, boolean>;
  onToggle: (name: "normal users" | "pro users") => void;
}

const LegendToggleButton = ({
  name,
  icon,
  colorClass,
  legend,
  onToggle,
}: LegendToggleButtonProps) => {
  const active = legend[name];

  return (
    <button
      type="button"
      onClick={() => onToggle(name)}
      className={`
        flex items-center gap-2 text-sm capitalize
        transition-opacity select-none
        ${active ? "" : "opacity-60"}
      `}
    >
      <Icon icon={icon} className={`w-4 h-4 ${colorClass}`} />
      <span>{name}</span>
    </button>
  );
};

export default LegendToggleButton;
