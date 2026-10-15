// // components/BubbleItem.tsx
// "use client";

// import { motion } from "framer-motion";
// import Image from "next/image";
// import { Bubble, Viewport } from "../types";

// interface BubbleItemProps {
//   bubble: Bubble;
//   viewport: Viewport;
//   offset: { x: number; y: number };
//   gap?: number;
//   hideOnEdge?: boolean;
//   withAnimation?: boolean;
//   onClick?: (bubble: Bubble) => void;
// }

// export const BubbleItem = ({
//   bubble,
//   viewport,
//   offset,
//   gap = 24,
//   hideOnEdge = false,
//   withAnimation = true,
//   onClick,
// }: BubbleItemProps) => {
//   const centerX = viewport.width / 2;
//   const centerY = viewport.height / 2;

//   const dx = bubble.x - centerX;
//   const dy = bubble.y - centerY;
//   const distance = Math.sqrt(dx * dx + dy * dy);
//   const radius = Math.min(centerX, centerY) - gap;
//   const isOutside = distance > radius;

//   if (hideOnEdge && isOutside) return null;

//   const translate = `translate(${bubble.x + offset.x}px, ${
//     bubble.y + offset.y
//   }px)`;
//   const scale = isOutside ? 0.75 : 1;

//   return (
//     <motion.div
//       initial={false}
//       animate={{
//         transform: translate + ` scale(${scale})`,
//         zIndex: isOutside ? 1 : 10,
//       }}
//       transition={{ duration: withAnimation ? 0.5 : 0 }}
//       className="absolute cursor-pointer"
//       onClick={() => onClick?.(bubble)}
//     >
//       <Image
//         src={bubble.icon}
//         alt={bubble.name}
//         width={64}
//         height={64}
//         className="rounded-full border border-white shadow-md"
//       />
//     </motion.div>
//   );
// };
