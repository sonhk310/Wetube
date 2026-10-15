import type EChartsReactCore from "echarts-for-react/lib/core";
import { MapChart, type MapSeriesOption } from "echarts/charts";
import {
  GeoComponent,
  type GeoComponentOption,
  TooltipComponent,
  type TooltipComponentOption,
} from "echarts/components";
import * as echarts from "echarts/core";
import { CanvasRenderer } from "echarts/renderers";
import { type MutableRefObject, useMemo } from "react";
import ReactEchart from "../../../base/ReactEhart";

import colors from "tailwindcss/colors";
import world from "../../../../assets/json/world.json";
import { type SalesMappingDataItem } from "../../../../data/sales-mapping-data";

/* ----------------------------------------- */
/*  Đăng ký module & bản đồ                  */
/* ----------------------------------------- */
echarts.use([TooltipComponent, GeoComponent, MapChart, CanvasRenderer]);

echarts.registerMap("world", world as any);

type EChartsOption = echarts.ComposeOption<
  TooltipComponentOption | GeoComponentOption | MapSeriesOption
>;

interface SalesMappingChartProps {
  salesMappingChartRef: MutableRefObject<EChartsReactCore | null>;
  data: SalesMappingDataItem[];
  style?: { height?: number; width?: number };
  minZoomLevel: number;
  maxZoomLevel: number;
  className?: string; // tuỳ chọn – thay cho sx
}

const SalesMappingChart = ({
  salesMappingChartRef,
  data,
  style,
  minZoomLevel,
  maxZoomLevel,
  className,
}: SalesMappingChartProps) => {
  /* ----------------------------------------- */
  /*  Cấu hình option ECharts                  */
  /* ----------------------------------------- */
  const option = useMemo<EChartsOption>(
    () => ({
      tooltip: {
        trigger: "item",
        showDelay: 0,
        transitionDuration: 0.2,
        formatter: (params) => {
          if (Array.isArray(params)) {
            // If params is an array, show the first item's name and value
            const p = params[0];
            return `${p.name} : ${p.value ? p.value : 0}`;
          } else {
            // If params is an object
            return `${params.name} : ${params.value ? params.value : 0}`;
          }
        },
      },

      series: [
        {
          type: "map",
          map: "world",
          data,
          roam: true,
          scaleLimit: { min: minZoomLevel, max: maxZoomLevel },
          left: 0,
          right: 0,
          label: { show: false },
          selectedMode: false,
          itemStyle: {
            areaColor: colors.slate[600], // tương đương grey.A700
            borderColor: "#ffffff",
            borderWidth: 0.2,
          },
          emphasis: { disabled: true },
        },
      ],
    }),
    [data, minZoomLevel, maxZoomLevel]
  );

  /* ----------------------------------------- */
  /*  Render                                   */
  /* ----------------------------------------- */
  return (
    <ReactEchart
      echarts={echarts}
      option={option}
      ref={salesMappingChartRef}
      style={style}
      className={className}
    />
  );
};

export default SalesMappingChart;
