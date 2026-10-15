import VideosPerformance from "../features/sections/videos-performance/VideosPerformance";
import Summary from "../features/sections/today-views/summary";
import TopChannels from "../features/sections/top-channels/TopChannels";
import TotalRevenueChart from "../features/sections/total-revenue/TotalRevenueChart";
import UsersInsightsChart from "../features/sections/users-insights/UsersInsightsChart";
import SalesMapping from "../features/sections/users-mapping/SalesMapping";
import { useEffect, useState } from "react";
import Loading from "../common/Loading";

const Dashboard = () => {
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => setLoading(false), 2000); 
    return () => clearTimeout(timer);
  }, []);

  return loading ? (
    <Loading />
  ) : (
    <div className="grid grid-cols-10 gap-6 w-full rounded-xl bg-gradient-to-br from-green-300 via-blue-200 to-yellow-100 w-full dark:bg-[#034c5f]">
      <div className="col-span-full rounded-xl ">
        <Summary />
      </div>

      <div className="col-span-6 rounded-xl ">
        <UsersInsightsChart />
      </div>

      <div className="col-span-4 rounded-xl ">
        <TopChannels />
      </div>

      <div className="col-span-full rounded-xl">
        <TotalRevenueChart />
      </div>

      <div className="col-span-full rounded-xl">
        <SalesMapping />
      </div>

      <div className="col-span-full rounded-xl">
        <VideosPerformance />
      </div>
    </div>
  );
};

export default Dashboard;
