// import React from "react";
// import { Bubble } from "./types";
// import BubbleSearch from "@/modules/subscription/components/BubbleSearch";
// import BubbleCanvas from "@/modules/subscription/components/BubbleCanvas";
// import BubbleDetail from "@/modules/subscription/components/BubbleDetail";

// interface VideoBubblePackProps {
//   data: Bubble[];
// }

// export function VideoBubblePack({ data }: VideoBubblePackProps) {
//   const [filteredBubbles, setFilteredBubbles] = React.useState<Bubble[]>(
//     data || []
//   );

//   React.useEffect(() => {
//     if (!Array.isArray(data)) {
//       console.warn("Expected array for data but got", data);
//       return;
//     }
//     setFilteredBubbles(data);
//   }, [data]);

//   return (
//     <div className="relative w-full h-full">
//       <BubbleSearch allBubbles={data} onSearchResult={setFilteredBubbles} />
//       <BubbleCanvas bubbles={filteredBubbles} />
//       <BubbleDetail />
//     </div>
//   );
// }
