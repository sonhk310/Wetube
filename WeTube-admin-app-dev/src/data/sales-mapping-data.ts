import colors from "tailwindcss/colors";

export interface SalesMappingDataItem {
  name: string;
  value: number;
  itemStyle: {
    areaColor: string; // mã màu Hex/RGB
  };
}

export const getSalesMappingData = (): SalesMappingDataItem[] => [
  {
    name: "Brazil",
    value: 19_000,
    itemStyle: { areaColor: colors.red[500] }, // bg-red-500
  },
  {
    name: "United States",
    value: 11_000,
    itemStyle: { areaColor: colors.orange[500] }, // bg-orange-500
  },
  {
    name: "China",
    value: 41_000,
    itemStyle: { areaColor: colors.purple[300] }, // secondary.light ≈ purple-300
  },
  {
    name: "Saudi Arabia",
    value: 7_000,
    itemStyle: { areaColor: colors.green[700] }, // green.dark ≈ green-700
  },
  {
    name: "Dem. Rep. Congo",
    value: 27_000,
    itemStyle: { areaColor: colors.sky[400] }, // info.light ≈ sky-400
  },
  {
    name: "Indonesia",
    value: 27_000,
    itemStyle: { areaColor: colors.green[800] }, // green.darker ≈ green-800
  },
];
