import Image from 'next/image';

export default function FeaturedHero() {
  return (
    <div className="flex flex-col md:flex-row gap-4 pb-2 pt-2">
      <div className="w-full md:w-1/2 relative rounded-lg overflow-hidden aspect-video">
        <Image
          src="https://i.ytimg.com/vi/sn44SGVacMw/hqdefault.jpg?sqp=-oaymwEnCNACELwBSFryq4qpAxkIARUAAIhCGAHYAQHiAQoIGBACGAY4AUAB&rs=AOn4CLAHPEu29Y95T8aaM3en1bz2kR5ZcQ"
          alt="Video nổi bật"
          fill
          sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
          className="object-cover"
        />
      </div>
      <div className="flex flex-col gap-2 w-full md:w-1/2">
        <h2 className="text-xl font-bold leading-snug">
          PHÂN TÍCH XU HƯỚNG GIÁ VÀNG TUẦN TỪ NGÀY 25/11 - 29/11/2024
        </h2>
        <p className="text-sm text-gray-400">
          Đức Anh Trader (Thần Rùa) • 5,2 N lượt xem • 8 tháng trước
        </p>
        <p className="text-sm text-gray-500 line-clamp-3">
          Group Zalo cộng đồng Đức Anh Trader: https://zalo.me/g/vsuryu848
          #phantichthitruong #duubaogiavang...
        </p>
      </div>
    </div>
  );
}
