"use client";

import { useState } from "react";
import { CartesianGrid, Line, LineChart, ReferenceArea, XAxis, YAxis } from "recharts";
import { useGetChannelAnalyticsQuery } from "@/app/api/analyticsApi";
import { Button } from "@/components/ui/button";
import { ChartConfig, ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { cn, formatCompact } from "@/lib/utils";
import { AnalyticsRange, AnalyticsTotals } from "@/types/analytics";

type Metric = keyof AnalyticsTotals;

const METRICS: { key: Metric; label: string; tracked: boolean }[] = [
  { key: "views", label: "Lượt xem", tracked: true },
  { key: "watchSeconds", label: "Thời gian xem (giờ)", tracked: true },
  { key: "subscribers", label: "Người đăng ký mới", tracked: false },
  { key: "comments", label: "Bình luận", tracked: false },
];

const RANGES: { value: AnalyticsRange; label: string }[] = [
  { value: 7, label: "7 ngày qua" },
  { value: 28, label: "28 ngày qua" },
  { value: 90, label: "90 ngày qua" },
];

const LINE_COLOR = "#065fd4";

const hours = (seconds: number) => seconds / 3600;
const formatHours = (h: number) => h.toLocaleString("vi-VN", { maximumFractionDigits: 1 });
/** "2026-10-24" -> "24/10" (trục ngày); kèm năm -> "24/10/2026". */
const formatDay = (date: string, withYear = false) => {
  const [y, m, d] = date.split("-");
  return withYear ? `${d}/${m}/${y}` : `${d}/${m}`;
};
const previousDay = (date: string) => {
  const d = new Date(`${date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() - 1);
  return d.toISOString().slice(0, 10);
};

function formatMetric(metric: Metric, value: number) {
  return metric === "watchSeconds" ? formatHours(hours(value)) : formatCompact(value);
}

export default function AnalyticsSection() {
  const [range, setRange] = useState<AnalyticsRange>(28);
  const [metric, setMetric] = useState<Metric>("views");
  const { data, isLoading, isError, refetch } = useGetChannelAnalyticsQuery(range);

  const current = METRICS.find((m) => m.key === metric)!;
  const chartData = data?.daily.map((d) => ({
    date: d.date,
    value: metric === "watchSeconds" ? hours(d.watchSeconds) : d[metric],
  }));
  const chartConfig = { value: { label: current.label, color: LINE_COLOR } } satisfies ChartConfig;
  // Lượt xem / thời gian xem chỉ có từ ngày bắt đầu ghi
  const untracked = data && current.tracked && data.trackingSince > data.from;

  return (
    <div className="flex flex-col gap-y-6 pt-2.5">
      <div className="flex flex-wrap items-start justify-between gap-3 px-4">
        <div>
          <h1 className="text-2xl font-bold">Số liệu phân tích của kênh</h1>
          {data && (
            <p className="text-xs text-muted-foreground">
              {formatDay(data.from, true)} – {formatDay(data.to, true)}
            </p>
          )}
        </div>
        <Select
          value={String(range)}
          onValueChange={(v) => {
            if (v) setRange(Number(v) as AnalyticsRange);
          }}
        >
          <SelectTrigger className="w-40">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {RANGES.map((r) => (
              <SelectItem key={r.value} value={String(r.value)}>
                {r.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {isError ? (
        <div className="flex flex-col items-center gap-3 px-4 py-10 text-sm text-muted-foreground">
          Không tải được số liệu phân tích.
          <Button variant="outline" className="rounded-full" onClick={() => refetch()}>
            Thử lại
          </Button>
        </div>
      ) : (
        <div className="mx-4 overflow-hidden rounded-xl border">
          <div className="grid grid-cols-2 border-b md:grid-cols-4">
            {METRICS.map((m) => (
              <button
                key={m.key}
                type="button"
                aria-pressed={metric === m.key}
                onClick={() => setMetric(m.key)}
                className={cn(
                  "border-b-2 px-4 py-3 text-left transition-colors hover:bg-gray-50",
                  metric === m.key ? "border-[#065fd4] bg-gray-50" : "border-transparent"
                )}
              >
                <p className="text-xs text-muted-foreground">{m.label}</p>
                {data ? (
                  <p className="mt-1 text-2xl font-semibold">{formatMetric(m.key, data.totals[m.key])}</p>
                ) : (
                  <Skeleton className="mt-2 h-7 w-16" />
                )}
              </button>
            ))}
          </div>

          <div className="p-4">
            {isLoading || !chartData ? (
              <Skeleton className="aspect-[3/1] w-full" />
            ) : (
              <ChartContainer config={chartConfig} className="aspect-auto h-64 w-full">
                <LineChart data={chartData} margin={{ left: 4, right: 12, top: 8 }}>
                  <CartesianGrid vertical={false} />
                  <XAxis
                    dataKey="date"
                    tickLine={false}
                    axisLine={false}
                    tickMargin={8}
                    minTickGap={24}
                    tickFormatter={(d: string) => formatDay(d)}
                  />
                  <YAxis
                    width={40}
                    tickLine={false}
                    axisLine={false}
                    allowDecimals={metric === "watchSeconds"}
                    tickFormatter={(v: number) =>
                      metric === "watchSeconds" ? formatHours(v) : formatCompact(v)
                    }
                  />
                  {untracked && (
                    <ReferenceArea
                      x1={data.from}
                      x2={previousDay(data.trackingSince)}
                      fill="#e5e5e5"
                      fillOpacity={0.6}
                    />
                  )}
                  <ChartTooltip
                    content={
                      <ChartTooltipContent
                        labelFormatter={(_, payload) =>
                          payload?.[0] ? formatDay(String(payload[0].payload.date), true) : ""
                        }
                        formatter={(value) => (
                          <span>
                            {current.label}:{" "}
                            <b>
                              {metric === "watchSeconds"
                                ? formatHours(Number(value))
                                : Number(value).toLocaleString("vi-VN")}
                            </b>
                          </span>
                        )}
                      />
                    }
                  />
                  <Line
                    dataKey="value"
                    type="monotone"
                    stroke="var(--color-value)"
                    strokeWidth={2}
                    dot={false}
                  />
                </LineChart>
              </ChartContainer>
            )}
            {untracked && (
              <p className="mt-2 text-xs text-muted-foreground">
                Chưa ghi dữ liệu {current.label.toLowerCase()} trước {formatDay(data.trackingSince, true)}.
              </p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
