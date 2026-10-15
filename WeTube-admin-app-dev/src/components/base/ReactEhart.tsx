import { forwardRef } from "react";
import type { EChartsReactProps } from "echarts-for-react";
import ReactEChartsCore from "echarts-for-react/lib/core";


export type ReactEchartProps = EChartsReactProps


const ReactEchart = forwardRef<null | ReactEChartsCore, ReactEchartProps>(
  ({ option, ...rest }, ref) => {
    return (
      <ReactEChartsCore
        ref={ref}
        option={{
          ...option,
          tooltip: {
            ...(option?.tooltip ?? {}),
            confine: true,
          },
        }}
        {...rest} /* bao gồm className, style, echarts, ... */
      />
    );
  }
);

ReactEchart.displayName = "ReactEchart";

export default ReactEchart;
