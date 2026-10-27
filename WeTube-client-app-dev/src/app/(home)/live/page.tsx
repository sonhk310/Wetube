import type { Metadata } from "next";
import LiveListView from "@/modules/live/ui/live-list-view";

export const metadata: Metadata = {
  title: "Đang phát trực tiếp - WeTube",
};

export default function LivePage() {
  return <LiveListView />;
}
