import { Icon } from "@iconify/react"; 
import { sales } from "../../../../data/sales";
import SummaryCard from "./summaryCard";

const Summary = () => {
  return (
    <div className="bg-[#D0D4B8] rounded-lg shadow-sm dark:bg-[#746c6b] pt-6 pb-8 px-8">
      <div className="flex items-center justify-between mb-10 dark:text-white">
        <div>
          <h4 className="text-2xl font-semibold mb-1">Today's Summary</h4>
        </div>

        <button
          type="button"
          className="
            inline-flex items-center gap-2
            border border-primary-600 text-primary-600
            hover:bg-primary-50
            px-4 py-2 rounded-md text-sm font-medium
            transition-colors
          "
        >
          <Icon
            icon="solar:upload-linear"
            className="w-5 h-5 dark:text-white"
          />
          Export
        </button>
      </div>

      <div
        className="
          grid
          grid-cols-1
          sm:grid-cols-2
          md:grid-cols-4
          gap-8         
          xl:gap-5  
          text-center   
        "
      >
        {sales.map((item) => (
          <SummaryCard key={item.label} item={item} />
        ))}
      </div>
    </div>
  );
};

export default Summary;
