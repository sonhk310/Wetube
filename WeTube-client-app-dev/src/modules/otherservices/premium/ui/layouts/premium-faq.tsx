"use client";
import { useState } from "react";
import { ChevronDown } from "lucide-react";

const faqs = [
  {
    question: "Gói WeTube Premium gồm những gì?",
    answer: (
      <>
        <p>Khi là thành viên WeTube Premium, bạn được:</p>
        <ul className="mt-2 list-disc space-y-1 pl-5">
          <li>Xem không quảng cáo: không thẻ quảng cáo, không quảng cáo chen trước hoặc giữa video.</li>
          <li>Tải video về máy bằng nút Tải xuống dưới video (tệp MP4).</li>
          <li>Phát nền trên điện thoại: tắt màn hình hoặc chuyển ứng dụng vẫn nghe tiếp.</li>
          <li>Nhãn Premium cạnh logo và vương miện cạnh tên ở bình luận, chat trực tiếp.</li>
        </ul>
      </>
    ),
  },
  {
    question: "Tôi thanh toán bằng cách nào?",
    answer: (
      <>
        <p>
          Bạn chọn MoMo (tính theo VND) hoặc PayPal (tính theo USD, quy đổi theo tỉ giá cố định của WeTube; số tiền
          USD hiện ở trang PayPal). Thanh toán xong, bạn được đưa về WeTube và Premium có ngay, không cần đăng nhập lại.
        </p>
      </>
    ),
  },
  {
    question: "Gói Premium kéo dài bao lâu? Có tự động gia hạn không?",
    answer: (
      <>
        <p>
          Có các gói 30 ngày, 90 ngày (3 tháng), 180 ngày (6 tháng) và 365 ngày (1 năm). Gói dài hơn có giá mỗi tháng
          rẻ hơn. WeTube không tự động gia hạn hay trừ tiền: hết hạn thì Premium tắt, bạn mua lại khi muốn.
        </p>
        <p className="mt-2">
          Mua thêm khi gói cũ còn hạn thì gói mới được cộng dồn vào sau ngày hết hạn, bạn không mất số ngày còn lại.
        </p>
      </>
    ),
  },
  {
    question: "Tôi dùng Premium trên thiết bị khác được không?",
    answer: (
      <>
        <p>
          Được. Premium gắn với tài khoản Google của bạn: đăng nhập đúng tài khoản đó trên bất kỳ thiết bị nào là có
          quyền lợi.
        </p>
      </>
    ),
  },
  {
    question: "Tải xuống hoạt động thế nào?",
    answer: (
      <>
        <p>
          Bấm nút Tải xuống dưới video để lưu tệp MP4 về thiết bị. Mỗi giờ tải được tối đa 30 video, và bạn chỉ tải được
          những video mình có quyền xem. Việc xem video trong WeTube vẫn cần kết nối internet.
        </p>
      </>
    ),
  },
  {
    question: "Phát nền hoạt động thế nào?",
    answer: (
      <>
        <p>
          Khi bạn tắt màn hình hoặc chuyển sang ứng dụng khác lúc đang xem, WeTube chuyển sang phát âm thanh của video
          để bạn nghe tiếp, và quay lại đúng chỗ khi bạn mở lại. Bạn bật hoặc tắt trong Cài đặt của trình phát (mục
          “Phát nền”). Kết quả có thể khác nhau tuỳ trình duyệt trên điện thoại; trên máy tính, video vốn vẫn phát khi
          bạn chuyển thẻ.
        </p>
      </>
    ),
  },
];

export default function PremiumFaq() {
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  const toggle = (index: number) => {
    setOpenIndex(openIndex === index ? null : index);
  };

  return (
    <section className="w-full max-w-7xl bg-gray-100 rounded-4xl p-6 sm:p-10 mx-auto my-12">
      <div className="text-left">
        <h2 className="text-4xl font-bold mb-6">Giải đáp thắc mắc</h2>

        <div className="divide-y divide-gray-300">
          {faqs.map((faq, index) => (
            <div key={index}>
              <button
                onClick={() => toggle(index)}
                className="w-full text-left py-5 flex justify-between items-center font-medium text-lg"
              >
                <span>{faq.question}</span>
                <ChevronDown
                  className={`w-5 h-5 transform ${
                    openIndex === index ? "rotate-180" : ""
                  }`}
                />
              </button>

              {openIndex === index && (
                <div className="pl-1 pb-4 text-sm font-normal text-gray-700">
                  {faq.answer}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
