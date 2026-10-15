import {Separator} from "@/components/ui/separator";
import {Sidebar, SidebarContent} from "@/components/ui/sidebar";
import {ExploreSection} from "./explore-section";
import {InfoSection} from "./info-section";
import {MainSection} from "./main-section";
import {OtherServices} from "./otherservices-section";
import {PersonalSection} from "./personal-section";
import {SubscriptionsSection} from "./subscriptions-section";

const HomeSidebar = () => {
    return (
        <Sidebar className="pt-16 z-40 " collapsible="icon">
            <SidebarContent className="no-scrollbar bg-background">
                <MainSection/>
                <Separator/>
                <PersonalSection/>
                <Separator/>
                <SubscriptionsSection/>
                <Separator/>
                <ExploreSection/>
                <Separator/>
                <OtherServices/>
                <Separator />
                <InfoSection />
            </SidebarContent>
        </Sidebar>
    );
};

export default HomeSidebar;
