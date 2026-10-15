import { Separator } from "@/components/ui/separator";
import SportsBanner from "./banner";
import LiveVideoList from "./liveSports";
import HighlightList from "./highlight";

const Sports = () => {
    return (
        <div>
            <SportsBanner />
            <br/>
            <LiveVideoList />
            <Separator />
            <HighlightList/>
        </div>

    );
};

export default Sports;
