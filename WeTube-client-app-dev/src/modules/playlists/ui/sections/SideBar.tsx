import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  FileText,
  MessageCircle,
  MessageSquare,
  Pause,
  Search,
  Settings,
  Trash2,
} from "lucide-react";

const Sidebar = () => {
  return (
    <div className=" p-8  border-gray-200 bg-white z-40">
      
      <div className="relative mb-8">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-500 h-4 w-4" />
        <Input
          placeholder="Tìm kiếm trong danh sách video của bạn"
          className="pl-10 bg-gray-50 border-gray-300 text-black placeholder-gray-500 focus:border-blue-500"
        />
      </div>

      <div className="space-y-4">
        <Button variant="ghost" className="w-full justify-start h-auto p-3 text-black hover:bg-gray-100 text-left">
          <Trash2 className="h-5 w-5 mr-3 flex-shrink-0" />
          <span>Xóa tất cả nhật ký xem</span>
        </Button>
        <Button variant="ghost" className="w-full justify-start h-auto p-3 text-black hover:bg-gray-100 text-left">
          <Pause className="h-5 w-5 mr-3 flex-shrink-0" />
          <span>Tạm dừng lưu nhật ký xem</span>
        </Button>
        <Button variant="ghost" className="w-full justify-start h-auto p-3 text-black hover:bg-gray-100 text-left">
          <Settings className="h-5 w-5 mr-3 flex-shrink-0" />
          <span>Quản lý toàn bộ nhật ký hoạt động</span>
        </Button>
      </div>

      <div className="mt-8 pt-6 border-t border-gray-200 space-y-2">
        <Button variant="ghost" className="w-full justify-start h-auto p-3 text-gray-600 hover:bg-gray-100 hover:text-black text-left">
          <MessageCircle className="h-4 w-4 mr-3 flex-shrink-0" />
          <span>Bình luận</span>
        </Button>
        <Button variant="ghost" className="w-full justify-start h-auto p-3 text-gray-600 hover:bg-gray-100 hover:text-black text-left">
          <FileText className="h-4 w-4 mr-3 flex-shrink-0" />
          <span>Bài đăng</span>
        </Button>
        <Button variant="ghost" className="w-full justify-start h-auto p-3 text-gray-600 hover:bg-gray-100 hover:text-black text-left">
          <MessageSquare className="h-4 w-4 mr-3 flex-shrink-0" />
          <span>Trò chuyện trực tiếp</span>
        </Button>
      </div>
    </div>
  );
};

export default Sidebar;
