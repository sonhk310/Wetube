import { useRef } from "react";
import type EChartsReactCore from "echarts-for-react/lib/core";

import SalesMappingChart from "./SalesMappingChart";
import { getSalesMappingData } from "../../../../data/sales-mapping-data";

const SalesMapping = () => {
  const chartRef = useRef<EChartsReactCore | null>(null);
  const salesMappingData = getSalesMappingData();

  return (
    /* Paper → div Tailwind */
    <div className="bg-[#D0D4B8] rounded-lg shadow-sm p-6 dark:bg-[#746c6b]">
      {/* Typography h4 */}
      <h4 className="text-2xl font-semibold text-primary-700 mb-5 dark:text-white">
        Users Mapping by Country
      </h4>

      {/* Chart, bọc thêm padding ngang giống sx={{ px: 3 }}  */}
      <div className="px-3">
        <SalesMappingChart
          salesMappingChartRef={chartRef}
          data={salesMappingData}
          style={{ height: 500 }}
          minZoomLevel={0.75}
          maxZoomLevel={1.1}
        />
      </div>
    </div>
  );
};

export default SalesMapping;
