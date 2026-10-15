import VideoView from "@/modules/studio/ui/views/video-view";

export const dynamic = "force-dynamic";

interface VideoIdPageProps {
  params: Promise<{ videoId: string }>;
}

const VideoIdPage = async ({ params }: VideoIdPageProps) => {
  const { videoId } = await params;
  return <VideoView videoId={videoId} />;
};

export default VideoIdPage;
