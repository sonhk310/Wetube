"use client";
import { useState } from "react";
import { ChevronDown } from "lucide-react";

const faqs = [
  {
    question: "Gói WeTube Premium gồm những gì?",
    answer: (
      <>
        <p>
          Khi là thành viên WeTube Premium, bạn xem video trên WeTube mà không bị
          quảng cáo làm gián đoạn.
        </p>
        <p className="mt-2">
          Bạn cũng có thể tham gia hội viên của từng kênh để nhận huy hiệu và
          nội dung dành riêng cho hội viên.
        </p>
      </>
    ),
  },
  {
    question: "Tôi thanh toán bằng cách nào?",
    answer: (
      <>
        <p>
          WeTube hỗ trợ thanh toán qua ví MoMo. Sau khi thanh toán xong, bạn sẽ
          được đưa về WeTube và tài khoản sẽ được nâng cấp.
        </p>
      </>
    ),
  },
  {
    question: "Gói Premium kéo dài bao lâu?",
    answer: (
      <>
        <p>
          Mỗi gói có thời hạn riêng (30, 90, 180 hoặc 365 ngày). Hết thời hạn,
          bạn có thể gia hạn bằng cách mua lại gói bất cứ lúc nào.
        </p>
      </>
    ),
  },
  {
    question: "WeTube Premium khác gì hội viên kênh?",
    answer: (
      <>
        <p>
          WeTube Premium áp dụng cho cả tài khoản của bạn (xem không quảng cáo).
          Hội viên kênh là khoản đóng góp cho một nhà sáng tạo cụ thể để nhận
          quyền lợi riêng của kênh đó.
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
                <div className="pl-1 pb-4 text-sm text-gray-700">
                  {faq.answer}
                </div>
              )}
            </div>
          ))}
        </div>

        <div className="mt-6 text-sm">
          Còn thắc mắc? Hãy dùng mục “Gửi phản hồi” ở thanh bên để liên hệ với chúng tôi.
        </div>
      </div>
    </section>
  );
}
