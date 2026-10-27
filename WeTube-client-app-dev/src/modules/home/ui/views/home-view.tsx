"use client";

import { useMemo, useState } from "react";
import { FilterCarousel } from "@/components/filter-carousel";
import { useGetHomeChipsQuery } from "@/app/api/homeApi";
import { useAuth } from "@/contexts/auth-context";
import VideoGrid from '../components/video/videoGrid';
import ChipVideoGrid from '../components/video/chipVideoGrid';

type HomeViewProps = {
    categoryId : string;
}

/** Mã ngẫu nhiên ngắn, đổi mỗi lần mở trang chủ (backend dùng để xáo nhẹ thứ tự gợi ý). */
const newSeed = () => Math.random().toString(36).slice(2, 10);

export default function HomeView({categoryId }: HomeViewProps) {
  const [selectedCategory, setSelectedCategory] = useState<string | null>(categoryId || null);
  // Cùng seed trong suốt một lần xem trang chủ: cuộn tải thêm không bị xáo, mở lại trang thì đổi
  const [seed] = useState(newSeed);
  const { user, isLoading: isAuthLoading } = useAuth();

  // Chip do backend quyết định: Kênh đăng ký, Mới tải lên, Chưa xem, Đã xem + chủ đề; chip không có video thì không có.
  // Chưa có lịch sử xem (hoặc chưa đăng nhập) thì không hiện hàng chip.
  const { data: chips, isLoading } = useGetHomeChipsQuery({ seed }, { skip: !user?.sub });
  const categories = useMemo(() => (chips ?? []).map((chip) => ({ value: chip.id, label: chip.label })), [chips]);
  const showChips = isAuthLoading || (!!user?.sub && (isLoading || categories.length > 0));

  const renderGrid = () => {
    if (!selectedCategory) return <VideoGrid seed={seed} />;
    return <ChipVideoGrid key={selectedCategory} chip={selectedCategory} seed={seed} />;
  };

  return (
    <div>
      {showChips && (
        <div className="px-6 pt-3 pb-5">
          <FilterCarousel
            value={selectedCategory}
            onSelect={(value) => setSelectedCategory(value)}
            data={categories}
            isLoading={isAuthLoading || isLoading}
          />
        </div>
      )}
      {renderGrid()}
    </div>
  );
}
