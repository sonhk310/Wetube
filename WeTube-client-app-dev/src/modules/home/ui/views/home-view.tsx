"use client";

import { useState } from "react";
import { FilterCarousel } from "@/components/filter-carousel";
import VideoGrid from '../components/video/videoGrid';

type HomeViewProps = {
    categoryId : string;
}

export default function HomeView({categoryId }: HomeViewProps) {
  const [selectedCategory, setSelectedCategory] = useState<string | null>(categoryId );

  const categories = [
    { value: "music", label: "Âm nhạc" },
    { value: "games", label: "Trò chơi" },
    { value: "news", label: "Tin tức" },
    { value: "sports", label: "Thể thao" },
    { value: "education", label: "Giáo dục" },
    { value: "entertainment", label: "Giải trí" },
    { value: "technology", label: "Công nghệ" },
    { value: "lifestyle", label: "Phong cách sống" },
    { value: "travel", label: "Du lịch" },
    { value: "food", label: "Ẩm thực" },
    { value: "fashion", label: "Thời trang" },
    { value: "finance", label: "Tài chính" },
    { value: "health", label: "Sức khỏe" },
    { value: "science", label: "Khoa học" },
    { value: "automotive", label: "Ô tô" },
    { value: "movies", label: "Phim" },
    ];
    
  return (
    <div>
      <div className="p-6">
        <FilterCarousel
          value={selectedCategory}
          onSelect={(value) => setSelectedCategory(value)}
          data={categories}
          isLoading={false}
        />
      </div>
      <VideoGrid />
    </div>
  );
}
