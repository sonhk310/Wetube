import ChannelLayout from "@/modules/channel/layout/channelLayout";

export const dynamic = "force-dynamic";

interface LayoutProps {
    children: React.ReactNode;
}

export default function Layout({ children }: LayoutProps) {
    return (
        <ChannelLayout>
            {children}
        </ChannelLayout>
    )
}