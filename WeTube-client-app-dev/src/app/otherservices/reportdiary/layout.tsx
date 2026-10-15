import ReportLayout from "@/modules/otherservices/reportdiary/ui/layout/report-layout";


export const dynamic = "force-dynamic";

interface LayoutProps {
    children: React.ReactNode;
}

export default function Layout({children }: LayoutProps ) {
    return (
        <ReportLayout>
            {children}
        </ReportLayout>
    )
}