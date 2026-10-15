// app/channel/layout.tsx

import ChannelHeader from "@/modules/channel/ui/channelHeader";
import ChannelTabs from "@/modules/channel/ui/channelTabs";

export default function ChannelLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col gap-4 px-4 py-4">
      <ChannelHeader />
      <ChannelTabs />
      <main>{children}</main>
    </div>
  );
}
