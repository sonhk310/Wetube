import TopChannels from "../features/sections/top-channels/TopChannels";
import TotalRevenueChart from "../features/sections/total-revenue/TotalRevenueChart";
import UsersInsightsChart from "../features/sections/users-insights/UsersInsightsChart";

export const ViewStatsPage = () => {
  return (
    <div className="grid grid-cols-10 gap-6 w-full rounded-xl bg-gradient-to-br from-green-300 via-blue-200 to-yellow-100 w-full dark:bg-[#034c5f]">
      
      <div className="col-span-6 rounded-xl ">
        <UsersInsightsChart />
      </div>

      <div className="col-span-4 rounded-xl ">
        <TopChannels />
      </div>

      <div className="col-span-full rounded-xl">
        <TotalRevenueChart />
      </div>
    </div>
  );
};
