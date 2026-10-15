"use client";

import * as React from "react";
import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from "recharts";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  type ChartConfig,
  ChartContainer,
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

interface apiChartItem {
  date: string;
  ads: number;
  pro: number;
  mem: number;
}

function formatTimestampToDateString(timestamp: number | string) {
  const date = new Date(Number(timestamp) * 1000);
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function useChartData() {
  const [chartData, setChartData] = React.useState<apiChartItem[]>([]);
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
        const mappedData: apiChartItem[] = result.map((item: apiChartItem) => ({
          date: formatTimestampToDateString(item.date),
          ads: Number(item.ads),
          pro: Number(item.pro),
          mem: Number(item.mem),
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
  ads: { label: "WeTubeAds", color: "hsl(var(--chart-1))" },
  pro: { label: "WeTubePro", color: "hsl(var(--chart-2))" },
  mem: { label: "Memberships", color: "hsl(var(--chart-3))" },
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
        acc.ads += item.ads;
        acc.pro += item.pro;
        acc.mem += item.mem;
        return acc;
      },
      { ads: 0, pro: 0, mem: 0 }
    );
  }, [filteredData]);

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;

  const maxValue = Math.max(
    ...filteredData.flatMap(
      (item: { ads: number; pro: number; mem: number }) => [
        item.ads,
        item.pro,
        item.mem,
      ]
    )
  );

  return (
    <Card className="bg-[#D0D4B8] dark:bg-[#746c6b]">
      <CardHeader>
        <CardTitle>Total Revenue</CardTitle>
        <div className="flex gap-6 mt-2 text-sm text-muted-foreground">
          <div className="flex flex-col items-start">
            <span className="font-medium text-foreground">
              ${totalByType.ads.toLocaleString()}
            </span>
            <span className="text-xs">WeTubeAds</span>
          </div>
          <div className="flex flex-col items-start">
            <span className="font-medium text-foreground">
              ${totalByType.pro.toLocaleString()}
            </span>
            <span className="text-xs">WeTubePro</span>
          </div>
          <div className="flex flex-col items-start">
            <span className="font-medium text-foreground">
              ${totalByType.mem.toLocaleString()}
            </span>
            <span className="text-xs">Memberships</span>
          </div>
        </div>
        <Select value={timeRange} onValueChange={setTimeRange}>
          <SelectTrigger
            className="hidden w-[160px] rounded-lg sm:ml-auto sm:flex"
            aria-label="Select a value"
          >
            <SelectValue placeholder="Last 3 months" />
          </SelectTrigger>
          <SelectContent className="rounded-xl bg-[#D0D4B8] hover:bg-[] dark:bg-[]">
            <SelectItem value="90d" className="rounded-lg ">
              Last 3 months
            </SelectItem>
            <SelectItem value="30d" className="rounded-lg">
              Last 30 days
            </SelectItem>
            <SelectItem value="7d" className="rounded-lg">
              Last 7 days
            </SelectItem>
          </SelectContent>
        </Select>
      </CardHeader>
      <CardContent>
        <ChartContainer config={chartConfig}>
          <BarChart accessibilityLayer data={filteredData}>
            <CartesianGrid vertical={false} />
            <XAxis
              dataKey="date"
              tickLine={false}
              tickMargin={10}
              axisLine={false}
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
              tickFormatter={(value) => `$${value}`}
            />
            <ChartTooltip
              cursor={false}
              content={
                <ChartTooltipContent
                  labelFormatter={(value) => {
                    return new Date(value).toLocaleDateString("en-US", {
                      month: "short",
                      day: "numeric",
                    });
                  }}
                  indicator="dot"
                />
              }
            />
            <Bar dataKey="ads" fill="#6366F1" radius={4} />
            <Bar dataKey="pro" fill="#EF4444" radius={4} />
            <Bar dataKey="mem" fill="#42e35dff" radius={4} />
          </BarChart>
        </ChartContainer>
        <div className="flex justify-center gap-4 mt-2">
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded-sm bg-[#6366F1]" />
            <span>WeTubeAds</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded-sm bg-[#EF4444]" />
            <span>WeTubePro</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded-sm bg-[#42e35dff]" />
            <span>Memberships</span>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
