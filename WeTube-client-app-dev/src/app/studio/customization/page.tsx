"use client";

// import { LayoutSectionData } from "@/types/channel";
// import { LayoutSectionList } from "@/components/channel/LayoutSectionList";
import { Button } from "@/components/ui/button";
// import { Switch } from "@/components/ui/switch";
// import { Label } from "@/components/ui/label";

// const initialSections: LayoutSectionData[] = [
//   { id: "s1", type: "FOR_YOU", title: "Dành cho bạn" },
//   { id: "s2", type: "VIDEOS", title: "Video" },
//   { id: "s3", type: "SHORTS", title: "Video ngắn" },
// ];

export default function ChannelLayoutPage() {
//   const [sections, setSections] =
//     useState<LayoutSectionData[]>(initialSections);
//   const [isHomepageEnabled, setIsHomepageEnabled] = useState(true);

  return (
    <div className="max-w-4xl mx-auto p-8 space-y-8">
      <section>
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-bold">Thẻ Trang chủ</h2>
            <p className="text-muted-foreground">
              Hiển thị thẻ Trang chủ để nêu bật và giới thiệu nội dung cho khán
              giả của bạn
            </p>
          </div>
          {/* <Switch
            id="homepage-toggle"
            checked={isHomepageEnabled}
            onCheckedChange={setIsHomepageEnabled}
          /> */}
        </div>
      </section>

      <hr />

      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-bold">Bố cục</h2>
            <p className="text-muted-foreground">
              Tùy chỉnh bố cục trang chủ của kênh bằng tối đa 12 phần kênh.
            </p>
          </div>
          <Button>+ Thêm phần kênh</Button>
        </div>

        {/* <LayoutSectionList
          sections={sections}
          setSections={setSections}
        /> */}
      </section>
    </div>
  );
}
