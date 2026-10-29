import { useAppDialogStore, type AppDialogRequest } from "@/store/zustand/useAppDialogStore";

interface ConfirmOptions {
  title: string;
  message?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  /** Hành động phá huỷ (xoá, ẩn...): nút xác nhận màu đỏ. */
  destructive?: boolean;
}

interface NoticeOptions {
  title: string;
  message?: string;
  buttonLabel?: string;
}

const MENU_POLL_MS = 40;
const MENU_WAIT_MAX_MS = 1000;

/**
 * Hiện hộp thoại, nhưng nếu đang có menu (vd. menu "..." của bình luận, vừa bấm "Xoá") thì chờ menu đóng hẳn.
 * Radix khoá chuột của cả trang (body pointer-events: none) cho mỗi lớp menu / hộp thoại; mở hộp thoại khi menu chưa
 * đóng xong rồi đóng bằng Esc làm trang bị khoá luôn, không bấm được gì nữa.
 */
function present(request: AppDialogRequest) {
  const started = Date.now();
  const tick = () => {
    const menuOpen = document.querySelector('[role="menu"]') !== null;
    if (menuOpen && Date.now() - started < MENU_WAIT_MAX_MS) {
      window.setTimeout(tick, MENU_POLL_MS);
      return;
    }
    useAppDialogStore.getState().push(request);
  };
  tick();
}

/**
 * Thay cho window.confirm: hộp thoại theo design system (<AppDialogHost /> gắn trong app/layout.tsx).
 * Trả true khi bấm xác nhận, false khi bấm Huỷ, nhấn Esc hoặc bấm ra ngoài. Chỉ gọi trong trình xử lý sự kiện.
 */
export function confirmDialog(options: ConfirmOptions): Promise<boolean> {
  return new Promise((resolve) => {
    present({
      kind: "confirm",
      title: options.title,
      message: options.message,
      confirmLabel: options.confirmLabel ?? "Xác nhận",
      cancelLabel: options.cancelLabel ?? "Huỷ",
      destructive: options.destructive ?? false,
      resolve,
    });
  });
}

/** Thay cho window.alert: hộp thoại thông báo một nút, đóng xong thì Promise hoàn tất. */
export function noticeDialog(options: NoticeOptions): Promise<void> {
  return new Promise((resolve) => {
    present({
      kind: "notice",
      title: options.title,
      message: options.message,
      confirmLabel: options.buttonLabel ?? "Đóng",
      cancelLabel: "",
      destructive: false,
      resolve: () => resolve(),
    });
  });
}
