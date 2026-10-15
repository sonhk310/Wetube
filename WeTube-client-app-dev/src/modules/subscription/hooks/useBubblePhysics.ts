// // hooks/useBubblePhysics.ts
// "use client";

// import { useEffect, useRef, useState } from "react";
// import { Bubble, Viewport } from "../types";

// export const useBubblePhysics = (
//   initialBubbles: Bubble[],
//   viewport: Viewport,
//   gap: number = 24
// ) => {
//   const [bubbles, setBubbles] = useState<Bubble[]>(initialBubbles);
//   const animationRef = useRef<number | null>(null);

//   useEffect(() => {
//     const updatePositions = () => {
//       setBubbles((prev) => {
//         const centerX = viewport.width / 2;
//         const centerY = viewport.height / 2;
//         const radius = Math.min(centerX, centerY) - gap;

//         return prev.map((bubble) => {
//           const dx = bubble.x - centerX;
//           const dy = bubble.y - centerY;
//           const dist = Math.sqrt(dx * dx + dy * dy);

//           let newX = bubble.x + bubble.vx;
//           let newY = bubble.y + bubble.vy;

//           // Bounce off the sphere edge
//           if (dist > radius) {
//             const angle = Math.atan2(dy, dx);
//             newX = centerX + Math.cos(angle) * radius;
//             newY = centerY + Math.sin(angle) * radius;
//             bubble.vx *= -0.7;
//             bubble.vy *= -0.7;
//           }

//           // Apply gravity toward center
//           const gravity = 0.03;
//           const ax = (centerX - bubble.x) * gravity;
//           const ay = (centerY - bubble.y) * gravity;

//           const vx = (bubble.vx + ax) * 0.95;
//           const vy = (bubble.vy + ay) * 0.95;

//           return {
//             ...bubble,
//             x: newX,
//             y: newY,
//             vx,
//             vy,
//           };
//         });
//       });

//       animationRef.current = requestAnimationFrame(updatePositions);
//     };

//     animationRef.current = requestAnimationFrame(updatePositions);
//     return () => {
//       if (animationRef.current) cancelAnimationFrame(animationRef.current);
//     };
//   }, [viewport, gap]);

//   return { bubbles, setBubbles };
// };
