"use client";

import { useMemo, useState } from "react";
import { FilterCarousel } from "@/components/filter-carousel";
import { useGetPopularTagsQuery } from "@/app/api/videoApi";
import VideoGrid from '../components/video/videoGrid';
import TagVideoGrid from '../components/video/tagVideoGrid';

type HomeViewProps = {
    categoryId : string;
}

export default function HomeView({categoryId }: HomeViewProps) {
  const [selectedCategory, setSelectedCategory] = useState<string | null>(categoryId || null);

  // Chip lấy từ các thẻ thật đang được dùng nhiều nhất trong video công khai
  const { data: popularTags, isLoading } = useGetPopularTagsQuery();
  const categories = useMemo(
    () => (popularTags ?? []).map((tag) => ({ value: tag.name, label: `#${tag.name}` })),
    [popularTags]
  );

  return (
    <div>
      <div className="p-6">
        <FilterCarousel
          value={selectedCategory}
          onSelect={(value) => setSelectedCategory(value)}
          data={categories}
          isLoading={isLoading}
        />
      </div>
      {selectedCategory ? <TagVideoGrid key={selectedCategory} tag={selectedCategory} /> : <VideoGrid />}
    </div>
  );
}
