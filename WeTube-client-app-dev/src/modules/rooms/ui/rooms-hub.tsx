"use client";

import { Link2, Plus, Users } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/contexts/auth-context";
import { useProgressRouter } from "@/hooks/use-progress-router";
import { parseRoomCode } from "@/lib/roomSync";
import { toastEmitter } from "@/lib/toastEmitter";
import { createRoom } from "../lib/createRoom";

/** /rooms: tạo phòng trống hoặc vào phòng bằng mã / liên kết mời. */
export default function RoomsHub() {
  const router = useProgressRouter();
  const { user, isLoading, login } = useAuth();
  const [input, setInput] = useState("");
  const [creating, setCreating] = useState(false);

  const create = async () => {
    setCreating(true);
    const code = await createRoom();
    setCreating(false);
    if (code) router.push(`/rooms/${code}`);
  };

  const join = (e: React.FormEvent) => {
    e.preventDefault();
    const code = parseRoomCode(input);
    if (!code) {
      toastEmitter.error("Mã phòng hoặc liên kết mời không hợp lệ");
      return;
    }
    router.push(`/rooms/${code}`);
  };

  return (
    <section className="mx-auto max-w-3xl px-6 py-10">
      <h1 className="flex items-center gap-2 text-3xl font-bold">
        <Users className="size-8" /> Phòng xem chung
      </h1>
      <p className="mt-2 text-gray-600">
        Cùng bạn bè xem một video, ai bấm phát, dừng hay tua thì cả phòng đi theo, kèm khung trò chuyện. Mẹo: mở video
        bất kỳ rồi bấm nút &quot;Xem chung&quot; để tạo phòng với đúng video và đúng giây đang xem.
      </p>

      {!isLoading && !user ? (
        <div className="mt-8 rounded-xl border p-6">
          <p className="mb-4">Bạn cần đăng nhập để tạo hoặc vào phòng xem chung.</p>
          <Button onClick={login}>Đăng nhập</Button>
        </div>
      ) : (
        <div className="mt-8 grid gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-3 rounded-xl border p-6">
            <h2 className="text-lg font-semibold">Tạo phòng mới</h2>
            <p className="flex-1 text-sm text-gray-600">
              Tạo phòng trống rồi gửi liên kết mời cho bạn bè. Bạn thêm video vào hàng chờ sau.
            </p>
            <Button onClick={create} disabled={creating || isLoading}>
              <Plus className="size-4" /> {creating ? "Đang tạo..." : "Tạo phòng"}
            </Button>
          </div>

          <form onSubmit={join} className="flex flex-col gap-3 rounded-xl border p-6">
            <h2 className="text-lg font-semibold">Vào phòng</h2>
            <p className="text-sm text-gray-600">Dán liên kết mời hoặc nhập mã phòng (dạng abc-defg-hij).</p>
            <Input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Liên kết hoặc mã phòng"
              aria-label="Liên kết hoặc mã phòng"
            />
            <Button type="submit" variant="outline" disabled={!input.trim() || isLoading}>
              <Link2 className="size-4" /> Vào phòng
            </Button>
          </form>
        </div>
      )}
    </section>
  );
}
