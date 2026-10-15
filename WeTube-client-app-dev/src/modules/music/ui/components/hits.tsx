"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import Image from 'next/image';

const hitList = [
  {
    title: "The Hit List",
    artist: "Benson Boone",
    img: "https://res.cloudinary.com/hits-photos-archive/image/upload/v1736884727/legacy-migration/legacy-hitsdd_photo_gal__photo_1625848077.jpg",
    description: "Tuyển tập những bản hit đỉnh nhất...",
    href: "/playlist/the-hit-list",
  },
  {
    title: "Summer Vibes",
    artist: "Various Artists",
    img: "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQToz0-jcbfVzn7YKeaGysb4j9LSJJ358bCVUC6PH8VMXcsfHtuKEu9RcMZsLJcpKQjIrg&usqp=CAU",
    description: "Những bản nhạc chill cho mùa hè...",
    href: "/playlist/summer-vibes",
  },
  {
    title: "Top EDM",
    artist: "Martin Garrix",
    // img: "/images/hitlist.jpg",
    img: "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQToz0-jcbfVzn7YKeaGysb4j9LSJJ358bCVUC6PH8VMXcsfHtuKEu9RcMZsLJcpKQjIrg&usqp=CAU",
    description: "Dành cho những ai yêu thích nhạc điện tử.",
    href: "/playlist/top-edm",
  },
  {
    title: "Lo-fi Beats",
    artist: "Lo-fi Girl",
    // img: "/images/hitlist.jpg",
    img: "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQToz0-jcbfVzn7YKeaGysb4j9LSJJ358bCVUC6PH8VMXcsfHtuKEu9RcMZsLJcpKQjIrg&usqp=CAU",
    description: "Nhạc học bài, thư giãn...",
    href: "/playlist/lofi-beats",
  },
  {
    title: "Chill Acoustic",
    artist: "Ed Sheeran",
    // img: "/images/hitlist.jpg",
    img: "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQToz0-jcbfVzn7YKeaGysb4j9LSJJ358bCVUC6PH8VMXcsfHtuKEu9RcMZsLJcpKQjIrg&usqp=CAU",
    description: "Những bản acoustic êm dịu và chill.",
    href: "/playlist/chill-acoustic",
  },
  {
    title: "Workout Energy",
    artist: "Various",
    // img: "/images/hitlist.jpg",
    img: "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQToz0-jcbfVzn7YKeaGysb4j9LSJJ358bCVUC6PH8VMXcsfHtuKEu9RcMZsLJcpKQjIrg&usqp=CAU",
    description: "Tăng năng lượng cho buổi tập luyện.",
    href: "/playlist/workout-energy",
  },
  {
    title: "V-Pop Now",
    artist: "AMEE",
    // img: "/images/hitlist.jpg",
    img: "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQToz0-jcbfVzn7YKeaGysb4j9LSJJ358bCVUC6PH8VMXcsfHtuKEu9RcMZsLJcpKQjIrg&usqp=CAU",
    description: "Nhạc Việt thịnh hành nhất hôm nay.",
    href: "/playlist/vpop-now",
  },
  {
    title: "K-Pop Fire",
    artist: "BTS, BLACKPINK",
    // img: "/images/hitlist.jpg",
    img: "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQToz0-jcbfVzn7YKeaGysb4j9LSJJ358bCVUC6PH8VMXcsfHtuKEu9RcMZsLJcpKQjIrg&usqp=CAU",
    description: "Bùng nổ cùng nhạc Hàn Quốc.",
    href: "/playlist/kpop-fire",
  },
];

export default function HitMusic() {
  const [isExpanded, setIsExpanded] = useState(false);
  const [isMobile, setIsMobile] = useState(false);
  const [displayCount, setDisplayCount] = useState(4);

  useEffect(() => {
    const checkMobile = () => {
      const isMobileDevice = window.innerWidth < 640;
      setIsMobile(isMobileDevice);

      let newDisplayCount;
      if (isExpanded) {
        newDisplayCount = isMobileDevice ? 6 : hitList.length;
      } else {
        newDisplayCount = isMobileDevice ? 2 : 4;
      }
      setDisplayCount(newDisplayCount);
    };

    checkMobile();
    window.addEventListener("resize", checkMobile);
    return () => window.removeEventListener("resize", checkMobile);
  }, [isExpanded, isMobile]);

  const visibleList = hitList.slice(0, displayCount);

  return (
    <section className="px-4 sm:px-6 md:px-8 py-6">
      <h2 className="text-2xl font-bold mb-6 text-black">Today Biggest Hits</h2>

      <div className="grid grid-cols-2 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-5">
        {visibleList.map((item) => (
          <Link
            href={item.href}
            key={item.href}
          >
            <div className="bg-neutral-900 rounded-lg overflow-hidden hover:scale-[1.02] transition-transform duration-300 cursor-pointer shadow-md">
              <div className="relative aspect-square">
                {/* <img
                  src={item.img}
                  alt={`Playlist cover: ${item.title}`}
                  className="w-full h-full object-cover"
                /> */}
                <Image
                  src={item.img}
                  alt={`Playlist cover: ${item.title}`}
                  fill 
                  sizes="(max-width: 640px) 50vw, (max-width: 768px) 33vw, 25vw" 
                  className="object-cover" 
                />
              </div>
              <div className="p-4">
                <h3 className="text-white font-semibold text-base">
                  {item.title}
                </h3>
                <p className="text-sm text-neutral-400">{item.artist}</p>
                <p className="text-xs text-neutral-500 mt-1 line-clamp-2">
                  {item.description}
                </p>
              </div>
            </div>
          </Link>
        ))}
      </div>

      {((isMobile && hitList.length > 2) ||
        (!isMobile && hitList.length > 4)) && (
        <div className="flex justify-center mt-8">
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="px-5 py-2 rounded-md bg-neutral-700 text-white hover:bg-neutral-600 transition-colors"
          >
            {isExpanded ? "Ẩn bớt" : "Hiển thị thêm"}
          </button>
        </div>
      )}
    </section>
  );
}
