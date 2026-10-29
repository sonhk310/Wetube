import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";
import { noticeDialog } from "@/lib/dialogs";

/** Gói Basic (cùng id với SubPackSeeder ở backend). */
export const BASIC_PACK_ID = "550e8400-e29b-41d4-a716-446655440000";

/** MoMo sandbox từ chối mọi đơn (resultCode 99) từ 29/10, lỗi phía MoMo: ẩn nút MoMo. MoMo chạy lại thì đặt true. */
export const MOMO_ENABLED = false;
export const MOMO_DOWN_NOTE = "MoMo tạm thời không hoạt động do lỗi từ phía MoMo, vui lòng thanh toán bằng PayPal.";

async function bodyOf(res: Response): Promise<{ message?: string; [key: string]: unknown }> {
  try {
    return await res.json();
  } catch {
    return {};
  }
}

/** Mua gói bằng MoMo: chuyển sang trang thanh toán của MoMo, hoặc hiện hộp thoại nói rõ lý do MoMo từ chối. */
export async function payWithMomo(subPackId: string, userId?: string): Promise<void> {
  try {
    const res = await apiFetch(`${API_PREFIX}/payment/momo/create`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ userId, subPackId }),
    });
    const data = await bodyOf(res);
    if (!res.ok) {
      await noticeDialog({
        title: "Không tạo được thanh toán MoMo",
        message: data.message || "Vui lòng đăng nhập lại và thử lại sau.",
      });
      return;
    }
    if (typeof data.payUrl === "string" && data.payUrl) {
      window.location.href = data.payUrl;
      return;
    }
    await noticeDialog({
      title: "MoMo chưa tạo được đơn thanh toán",
      message:
        `MoMo từ chối đơn${data.resultCode !== undefined ? ` (mã ${String(data.resultCode)})` : ""}` +
        `${data.message ? `: ${data.message}` : "."}\nBạn có thể thử lại sau hoặc thanh toán bằng PayPal.`,
    });
  } catch {
    await noticeDialog({ title: "Không tạo được thanh toán MoMo", message: "Không kết nối được máy chủ, vui lòng thử lại." });
  }
}

/** Mua gói bằng PayPal: chuyển sang trang chấp thuận của PayPal, hoặc hiện hộp thoại nói rõ lý do lỗi. */
export async function payWithPaypal(subPackId: string): Promise<void> {
  try {
    const res = await apiFetch(`${API_PREFIX}/payment/paypal/create`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ subPackId }),
    });
    const data = await bodyOf(res);
    if (res.ok && typeof data.approveUrl === "string" && data.approveUrl) {
      window.location.href = data.approveUrl;
      return;
    }
    await noticeDialog({
      title: "Không tạo được thanh toán PayPal",
      message:
        res.status === 401
          ? "Bạn cần đăng nhập để mua Premium."
          : data.message || "Vui lòng thử lại sau.",
    });
  } catch {
    await noticeDialog({ title: "Không tạo được thanh toán PayPal", message: "Không kết nối được máy chủ, vui lòng thử lại." });
  }
}
