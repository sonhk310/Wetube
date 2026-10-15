import React from "react";
import {
  TrendingUp,
  TrendingDown,
  Eye,
  ThumbsUp,
  MessageCircle,
  DollarSign,
} from "lucide-react";

interface StatCardProps {
  title: string;
  value: string;
  change: string;
  changeType: "up" | "down";
  icon: React.ComponentType<React.SVGProps<SVGSVGElement>>;
  color: string;
}

const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  change,
  changeType,
  icon: Icon,
  color,
}) => (
  <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
    <div className="flex items-center justify-between">
      <div className="flex-1">
        <p className="text-sm text-gray-600 mb-1">{title}</p>
        <p className="text-2xl font-bold text-gray-900">{value}</p>
        <div className="flex items-center mt-2">
          {changeType === "up" ? (
            <TrendingUp className="w-4 h-4 text-green-500 mr-1" />
          ) : (
            <TrendingDown className="w-4 h-4 text-red-500 mr-1" />
          )}
          <span
            className={`text-sm font-medium ${
              changeType === "up" ? "text-green-600" : "text-red-600"
            }`}
          >
            {change}
          </span>
          <span className="text-sm text-gray-500 ml-1">vs last week</span>
        </div>
      </div>
      <div
        className={`w-12 h-12 rounded-lg flex items-center justify-center ${color}`}
      >
        <Icon className="w-6 h-6 text-white" />
      </div>
    </div>
  </div>
);

const DashboardStats: React.FC = () => {
  const stats = [
    {
      title: "Total Views",
      value: "2.4M",
      change: "+12.5%",
      changeType: "up" as const,
      icon: Eye,
      color: "bg-blue-500",
    },
    {
      title: "Subscribers",
      value: "47.2K",
      change: "+8.2%",
      changeType: "up" as const,
      icon: ThumbsUp,
      color: "bg-green-500",
    },
    {
      title: "Watch Time",
      value: "1.2M hrs",
      change: "+15.3%",
      changeType: "up" as const,
      icon: TrendingUp,
      color: "bg-purple-500",
    },
    {
      title: "Revenue",
      value: "$8,547",
      change: "+22.1%",
      changeType: "up" as const,
      icon: DollarSign,
      color: "bg-emerald-500",
    },
    {
      title: "Comments",
      value: "3.8K",
      change: "+5.7%",
      changeType: "up" as const,
      icon: MessageCircle,
      color: "bg-orange-500",
    },
    {
      title: "Avg. Duration",
      value: "4:32",
      change: "-2.1%",
      changeType: "down" as const,
      icon: TrendingDown,
      color: "bg-red-500",
    },
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
      {stats.map((stat, index) => (
        <StatCard key={index} {...stat} />
      ))}
    </div>
  );
};

export default DashboardStats;
