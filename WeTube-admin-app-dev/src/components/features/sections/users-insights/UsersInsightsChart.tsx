"use client";

import * as React from "react";
import { Area, AreaChart, CartesianGrid, XAxis, YAxis } from "recharts";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  type ChartConfig,
  ChartContainer,
  ChartLegend,
  ChartLegendContent,
  ChartTooltip,
  ChartTooltipContent,
} from "@/components/ui/chart";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface ApiChartItem {
  date: string;
  normal: number;
  pro: number;
}

function formatTimestampToDateString(timestamp: number | string) {
  const date = new Date(Number(timestamp) * 1000);
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function useChartData() {
  const [chartData, setChartData] = React.useState<ApiChartItem[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    async function fetchChartData() {
      try {
        const response = await fetch(
          "https://687076887ca4d06b34b6db53.mockapi.io/api/v1/totalRevenue"
        );
        if (!response.ok) throw new Error("Failed to fetch data");

        const result = await response.json();
        const mappedData: ApiChartItem[] = result.map((item: ApiChartItem) => ({
          date: formatTimestampToDateString(item.date),
          normal: Number(item.normal),
          pro: Number(item.pro),
        }));

        setChartData(mappedData);
      } catch (err) {
        if (err instanceof Error) {
          setError(err.message);
        } else {
          setError("Unknown error");
        }
      } finally {
        setLoading(false);
      }
    }
    fetchChartData();
  }, []);

  return { chartData, loading, error };
}

const chartConfig = {
  normal: { label: "Normal Users", color: "var(--chart-1)" },
  pro: { label: "Pro Users", color: "var(--chart-2)" },
} satisfies ChartConfig;

export default function TotalRevenue() {
  const { chartData, loading, error } = useChartData();
  const [timeRange, setTimeRange] = React.useState("90d");
  const referenceDate = React.useMemo(() => {
    if (chartData.length === 0) return new Date();

    const lastItem = chartData[chartData.length - 1];
    return new Date(lastItem.date);
  }, [chartData]);

  const filteredData = React.useMemo(() => {
    let daysToSubtract = 90;
    if (timeRange === "30d") {
      daysToSubtract = 30;
    } else if (timeRange === "7d") {
      daysToSubtract = 7;
    }

    const startDate = new Date(referenceDate);
    startDate.setDate(referenceDate.getDate() - daysToSubtract);

    return chartData.filter((item) => {
      const date = new Date(item.date);
      return date >= startDate;
    });
  }, [chartData, timeRange, referenceDate]);

  const totalByType = React.useMemo(() => {
    return filteredData.reduce(
      (acc, item) => {
        acc.normal += item.normal;
        acc.pro += item.pro;
        return acc;
      },
      { normal: 0, pro: 0 }
    );
  }, [filteredData]);

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;

  const maxValue = Math.max(
    ...filteredData.flatMap((item: { normal: number; pro: number }) => [
      item.normal,
      item.pro,
    ])
  );

  return (
    <Card className="pt-0 bg-[#D0D4B8] dark:bg-[#746c6b]">
      <CardHeader className="flex items-center gap-2 space-y-0 border-b py-5 sm:flex-row">
        <CardTitle className="grid flex-1 gap-1 text-xl">Users</CardTitle>
        <div className="flex gap-6 mt-2 text-sm text-muted-foreground">
          <div className="flex flex-col items-start">
            <span className="font-medium text-foreground">
              {totalByType.normal.toLocaleString()}
            </span>
            <span className="text-xs">Normal Users</span>
          </div>
          <div className="flex flex-col items-start">
            <span className="font-medium text-foreground">
              {totalByType.pro.toLocaleString()}
            </span>
            <span className="text-xs">Pro Users</span>
          </div>
        </div>
        <Select
          value={timeRange}
          onValueChange={setTimeRange}
        >
          <SelectTrigger
            className="hidden w-[160px] rounded-lg sm:ml-auto sm:flex"
            aria-label="Select a value"
          >
            <SelectValue placeholder="Last 3 months" />
          </SelectTrigger>
          <SelectContent className="rounded-xl">
            <SelectItem
              value="90d"
              className="rounded-lg"
            >
              Last 3 months
            </SelectItem>
            <SelectItem
              value="30d"
              className="rounded-lg"
            >
              Last 30 days
            </SelectItem>
            <SelectItem
              value="7d"
              className="rounded-lg"
            >
              Last 7 days
            </SelectItem>
          </SelectContent>
        </Select>
      </CardHeader>
      <CardContent className="px-2 pt-4 sm:px-6 sm:pt-6">
        <ChartContainer
          config={chartConfig}
          className="aspect-auto h-[250px] w-full"
        >
          <AreaChart data={filteredData}>
            <defs>
              <linearGradient
                id="fillNormal"
                x1="0"
                y1="0"
                x2="0"
                y2="1"
              >
                <stop
                  offset="5%"
                  stopColor="var(--color-normal)"
                  stopOpacity={0.8}
                />
                <stop
                  offset="95%"
                  stopColor="var(--color-normal)"
                  stopOpacity={0.1}
                />
              </linearGradient>
              <linearGradient
                id="fillPro"
                x1="0"
                y1="0"
                x2="0"
                y2="1"
              >
                <stop
                  offset="5%"
                  stopColor="var(--color-pro)"
                  stopOpacity={0.8}
                />
                <stop
                  offset="95%"
                  stopColor="var(--color-pro)"
                  stopOpacity={0.1}
                />
              </linearGradient>
            </defs>
            <CartesianGrid vertical={false} />
            <XAxis
              dataKey="date"
              tickLine={false}
              axisLine={false}
              tickMargin={8}
              minTickGap={32}
              tickFormatter={(value) => {
                const date = new Date(value);
                return date.toLocaleDateString("en-US", {
                  month: "short",

                  day: "numeric",
                });
              }}
            />

            <YAxis
              tickLine={false}
              tickMargin={10}
              axisLine={false}
              domain={[0, maxValue]}
              tickFormatter={(value) => `${value}`}
            />

            <ChartTooltip
              cursor={false}
              content={
                <ChartTooltipContent
                  labelFormatter={(value: number) => {
                    return new Date(value).toLocaleDateString("en-US", {
                      month: "short",
                      day: "numeric",
                    });
                  }}
                  indicator="dot"
                />
              }
            />
            <Area
              dataKey="pro"
              type="natural"
              fill="url(#fillPro)"
              stroke="var(--color-pro)"
              stackId="a"
            />
            <Area
              dataKey="normal"
              type="natural"
              fill="url(#fillNormal)"
              stroke="var(--color-normal)"
              stackId="a"
            />
            <ChartLegend content={<ChartLegendContent />} />
          </AreaChart>
        </ChartContainer>
      </CardContent>
    </Card>
  );
}
