// // components/BubbleCanvas.tsx
// "use client";

// import { Bubble } from "../types";
// import { BubbleItem } from "./BubbleItem";
// import { Viewport } from "../types";

// interface BubbleCanvasProps {
//   bubbles: Bubble[];
//   viewport: Viewport;
//   offset: { x: number; y: number };
//   gap?: number;
//   hideOnEdge?: boolean;
//   withAnimation?: boolean;
//   onClickBubble?: (bubble: Bubble) => void;
// }

// export const BubbleCanvas = ({
//   bubbles,
//   viewport,
//   offset,
//   gap = 24,
//   hideOnEdge = false,
//   withAnimation = true,
//   onClickBubble,
// }: BubbleCanvasProps) => {
//   return (
//     <div className="absolute inset-0">
//       {bubbles.map((bubble) => (
//         <BubbleItem
//           key={bubble.id}
//           bubble={bubble}
//           viewport={viewport}
//           offset={offset}
//           gap={gap}
//           hideOnEdge={hideOnEdge}
//           withAnimation={withAnimation}
//           onClick={onClickBubble}
//         />
//       ))}
//     </div>
//   );
// };
