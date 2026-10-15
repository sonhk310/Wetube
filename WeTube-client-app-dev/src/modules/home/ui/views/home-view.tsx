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
    { value: "music", label: "Music" },
    { value: "games", label: "Games" },
    { value: "news", label: "News" },
    { value: "sports", label: "Sports" },
    { value: "education", label: "Education" },
    { value: "entertainment", label: "Entertainment" },
    { value: "technology", label: "Technology" },
    { value: "lifestyle", label: "Lifestyle" },
    { value: "travel", label: "Travel" },
    { value: "food", label: "Food" },
    { value: "fashion", label: "Fashion" },
    { value: "finance", label: "Finance" },
    { value: "health", label: "Health" },
    { value: "science", label: "Science" },
    { value: "automotive", label: "Automotive" },
    { value: "movies", label: "Movies" },
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
