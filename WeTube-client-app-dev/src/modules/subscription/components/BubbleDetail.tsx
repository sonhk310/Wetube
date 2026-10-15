// // components/BubbleDetail.tsx
// "use client";

// import { useEffect, useRef, useState } from "react";
// import { Bubble } from "../types";
// import Image from "next/image";

// interface BubbleDetailProps {
//   channel: Bubble | null;
//   onClose: () => void;
// }

// export const BubbleDetail = ({ channel, onClose }: BubbleDetailProps) => {
//   const videoRef = useRef<HTMLVideoElement>(null);
//   const [playing, setPlaying] = useState(false);

//   useEffect(() => {
//     if (!channel || !videoRef.current) return;
//     const video = videoRef.current;
//     video.currentTime = 0;
//     video.play();
//     setPlaying(true);

//     const timeout = setTimeout(() => {
//       video.pause();
//       video.currentTime = 0;
//       video.play();
//     }, 10000);

//     return () => clearTimeout(timeout);
//   }, [channel]);

//   if (!channel) return null;

//   return (
//     <div className="absolute inset-0 flex items-center justify-center z-50">
//       <div className="relative w-[500px] h-[300px] rounded-xl overflow-hidden shadow-lg backdrop-blur bg-black/50">
//         <video
//           ref={videoRef}
//           src={channel.videoUrl}
//           className="absolute inset-0 w-full h-full object-cover"
//           muted
//           loop
//         />

//         <div className="absolute bottom-4 left-4 text-white bg-black/50 px-4 py-2 rounded-xl max-w-[80%]">
//           <div className="flex items-center gap-2">
//             <Image
//               src={channel.icon}
//               alt={channel.name}
//               width={40}
//               height={40}
//               className="rounded-full border border-white"
//             />
//             <div>
//               <div className="font-semibold text-lg">{channel.name}</div>
//               <div className="text-sm opacity-80">
//                 {channel.subscribers} subscribers
//               </div>
//             </div>
//           </div>
//           <p className="text-sm mt-2 opacity-90 line-clamp-3">
//             {channel.description}
//           </p>
//         </div>

//         <button
//           className="absolute top-2 right-2 bg-white/30 hover:bg-white/50 text-white px-2 py-1 rounded"
//           onClick={onClose}
//         >
//           ✕
//         </button>
//       </div>
//     </div>
//   );
// };
