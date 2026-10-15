import React from "react";



const ReportStatus: React.FC = () => {
  return (
    <div className="bg-white text-black min-h-screen px-6 py-10 space-y-8 text-sm text-left">
      <div>
        <h1 className="text-2xl font-bold mb-2">Cảm ơn bạn đã báo cáo</h1>
        <p className="text-black max-w-3xl">
          Bất kỳ thành viên nào của cộng đồng YouTube cũng có thể gắn 
          cờ cho nội dung mà họ tin rằng vi phạm Nguyên tắc cộng đồng của chúng tôi. 
          Khi nội dung nào đó bị gắn cờ, nội dung đó sẽ không tự động bị gỡ bỏ. 
          Chúng tôi xem xét nội dung bị gắn cờ theo các nguyên tắc sau:
        </p>
        <ul className="list-disc list-inside mt-4 space-y-1 text-black">
          <li>Nội dung vi phạm <a href="#" className="text-blue-600 underline">Nguyên tắc cộng đồng</a> của chúng tôi sẽ bị xóa khỏi YouTube.</li>
          <li>Nội dung không phù hợp với tất cả khán giả nhỏ tuổi hơn có thể bị giới hạn độ tuổi người xem.</li>
          <li>Những báo cáo về nội dung đã bị người sáng tạo xóa sẽ không được xuất hiện.</li>
        </ul>
        <a
          href="#"
          className="block mt-4 text-blue-600 hover:underline"
        >
          Tìm hiểu thêm về cách báo cáo nội dung trên YouTube.
        </a>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-sm text-left border-t border-gray-600">
          <thead className="text-black uppercase">
            <tr>
              <th className="py-3 px-4">Loại</th>
              <th className="py-3 px-4">Nội dung</th>
              <th className="py-3 px-4">Lý do báo vi phạm</th>
              <th className="py-3 px-4">Trạng thái</th>
            </tr>
          </thead>
          <tbody className="text-black">
            <tr className="border-t border-black">
              <td className="py-3 px-4">🎞️ Video</td>
              <td className="py-3 px-4">
                <a href="#" className="text-blue-600 hover:underline">
                  Giả Nghèo Yêu Bạn Gái Bị Cắm Sừng - Tôi Quay Sang Cưới Tiểu Thư Tỷ Phú
                </a>
                <div className="text-black text-sm">Mề Truyện Anime</div>
              </td>
              <td className="py-3 px-4 text-black">
                Vấn đề về hình ảnh hoặc tiêu đề &gt; Nội dung rác hoặc gây hiểu lầm<br />
                <span className="text-xs text-black">3 thg 8, 2025</span>
              </td>
              <td className="py-3 px-4 text-green-400">Đang hoạt động</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default ReportStatus;
