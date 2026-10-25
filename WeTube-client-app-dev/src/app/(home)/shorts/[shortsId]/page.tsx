import ShortsView from "@/modules/shorts/ui/views/shorts-view";

export default async function ShortsDetailPage({ params }: { params: Promise<{ shortsId: string }> }) {
  const { shortsId } = await params;
  return <ShortsView startId={shortsId} />;
}
