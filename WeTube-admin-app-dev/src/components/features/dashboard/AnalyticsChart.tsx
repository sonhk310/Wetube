import { TrendingUp } from "lucide-react";

function AnalyticsChart() {
  //Dumb data
  const chartData = [
    { day: "Mon", views: 45000, watchTime: 2800 },
    { day: "Tue", views: 52000, watchTime: 3200 },
    { day: "Wed", views: 48000, watchTime: 2900 },
    { day: "Thu", views: 61000, watchTime: 3800 },
    { day: "Fri", views: 55000, watchTime: 3400 },
    { day: "Sat", views: 67000, watchTime: 4200 },
    { day: "Sun", views: 58000, watchTime: 3600 },
  ];

  const maxViews = Math.max(...chartData.map((d) => d.views));

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100">
      <div className="p-6 border-b border-gray-100">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-semibold text-gray-900">
              Views & Watch Time
            </h2>
            <p className="text-sm text-gray-500">Last 7 days performance</p>
          </div>
          <div className="flex items-center space-x-2 text-green-600">
            <TrendingUp className="w-4 h-4" />
            <span className="text-sm font-medium">+12.5%</span>
          </div>
        </div>
      </div>

      <div className="p-6">
        <div className="h-64 flex items-end justify-between space-x-2">
          {chartData.map((data, index) => (
            <div key={index} className="flex-1 flex flex-col items-center">
              <div className="w-full flex flex-col items-center space-y-1">
                <div
                  className="w-full bg-blue-500 rounded-t-lg transition-all duration-500 ease-out"
                  style={{ height: `${(data.views / maxViews) * 180}px` }}
                ></div>
                <div
                  className="w-full bg-blue-300 rounded-b-lg transition-all duration-500 ease-out"
                  style={{ height: `${(data.watchTime / 5000) * 60}px` }}
                ></div>
              </div>
              <span className="text-xs text-gray-500 mt-2">{data.day}</span>
            </div>
          ))}
        </div>

        <div className="flex items-center justify-center space-x-6 mt-6 pt-6 border-t border-gray-100">
          <div className="flex items-center space-x-2">
            <div className="w-3 h-3 bg-blue-500 rounded-full"></div>
            <span className="text-sm text-gray-600">Views</span>
          </div>
          <div className="flex items-center space-x-2">
            <div className="w-3 h-3 bg-blue-300 rounded-full"></div>
            <span className="text-sm text-gray-600">Watch Time (hrs)</span>
          </div>
        </div>
      </div>
    </div>
  );
}

export default AnalyticsChart;
