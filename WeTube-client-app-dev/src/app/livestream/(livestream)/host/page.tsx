import { redirect } from "next/navigation";
import HostPageImpl from "../../../../modules/stream/views/host/page.client";

interface PageProps {
  searchParams: Promise<{
    at: string | undefined;
    rt: string | undefined;
  }>;
}

export default async function HostPage({ searchParams }: PageProps) {
  const { at, rt } = await searchParams;

  if (!at || !rt) {
    redirect("/");
  }

  const serverUrl = process.env
    .LIVEKIT_WS_URL!.replace("wss://", "https://")
    .replace("ws://", "http://");

  return (
    <HostPageImpl
      authToken={at}
      roomToken={rt}
      serverUrl={serverUrl}
    />
  );
}
