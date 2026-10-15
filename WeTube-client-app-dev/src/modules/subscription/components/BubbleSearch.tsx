// // components/BubbleSearch.tsx
// "use client";

// import { ChangeEvent } from "react";

// interface BubbleSearchProps {
//   value: string;
//   onChange: (value: string) => void;
// }

// export const BubbleSearch = ({ value, onChange }: BubbleSearchProps) => {
//   const handleInputChange = (e: ChangeEvent<HTMLInputElement>) => {
//     onChange(e.target.value);
//   };

//   return (
//     <div className="absolute top-4 left-4 z-40">
//       <input
//         type="text"
//         value={value}
//         onChange={handleInputChange}
//         placeholder="Search channels..."
//         className="px-4 py-2 rounded-md bg-white/80 backdrop-blur text-sm w-[200px] shadow"
//       />
//     </div>
//   );
// };
