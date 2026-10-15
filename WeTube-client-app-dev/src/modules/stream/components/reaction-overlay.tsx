// "use client";
//
// import { useEffect, useState } from "react";
// import { motion, AnimatePresence } from "framer-motion";
//
// interface FloatingReaction {
//     id: string;
//     emoji: string;
//     x: number;
//     duration: number;
// }
//
// export function FloatingReactionsOverlay({ reaction }: { reaction: string | null }) {
//     const [reactions, setReactions] = useState<FloatingReaction[]>([]);
//
//     useEffect(() => {
//         if (!reaction) return;
//
//         const newReaction: FloatingReaction = {
//             id: Date.now().toString(),
//             emoji: reaction,
//             x: Math.random() * 100 - 50, // random lệch trái/phải
//             duration: 2 + Math.random() * 1.5, // random 2–3.5 giây
//         };
//
//         setReactions((prev) => [...prev, newReaction]);
//
//         const timer = setTimeout(() => {
//             setReactions((prev) => prev.filter((r) => r.id !== newReaction.id));
//         }, newReaction.duration * 1000);
//
//         return () => clearTimeout(timer);
//     }, [reaction]);
//
//     return (
//         <div className="absolute bottom-10 left-1/2 -translate-x-1/2 w-full max-w-[300px] h-0 pointer-events-none overflow-visible">
//             <AnimatePresence>
//                 {reactions.map((r) => (
//                     <motion.div
//                         key={r.id}
//                         initial={{ opacity: 0, y: 0, x: 0, scale: 0.8 }}
//                         animate={{
//                             opacity: [1, 1, 0],
//                             y: -200,
//                             x: r.x,
//                             scale: [1, 1.3, 1]
//                         }}
//                         exit={{ opacity: 0 }}
//                         transition={{ duration: r.duration, ease: "easeOut" }}
//                         className="absolute bottom-0 left-1/2 text-4xl"
//                     >
//                         {r.emoji}
//                     </motion.div>
//                 ))}
//             </AnimatePresence>
//         </div>
//     );
// }
