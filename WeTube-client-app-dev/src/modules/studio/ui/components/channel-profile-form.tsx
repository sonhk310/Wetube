"use client";

import Image from "next/image";
import { useRef, useState } from "react";
import {
  useGetChannelInfoQuery,
  useUpdateChannelMutation,
  useUploadChannelImageMutation,
} from "@/app/api/channelApi";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/contexts/auth-context";
import { apiErrorMessage } from "@/lib/apiError";
import { toastEmitter } from "@/lib/toastEmitter";
import ImageCropDialog, { CropKind } from "./image-crop-dialog";

const NAME_MAX = 50;
const DESCRIPTION_MAX = 1000;
// Ảnh gốc được cắt và thu nhỏ ngay trong trình duyệt nên cho chọn ảnh khá lớn (ảnh chụp từ điện thoại)
const SOURCE_MAX_MB = 25;

type ImageKind = CropKind;

/** Khung "Thông tin cơ bản" ở Studio → Tuỳ chỉnh: ảnh bìa, ảnh đại diện, tên và mô tả kênh. */
export function ChannelProfileForm() {
  const { user, refresh } = useAuth();
  const channelId = user?.channelId;
  const { data: channel, isLoading } = useGetChannelInfoQuery(channelId ?? "", { skip: !channelId });
  const [updateChannel, { isLoading: isSaving }] = useUpdateChannelMutation();
  const [uploadImage] = useUploadChannelImageMutation();
  const [uploading, setUploading] = useState<ImageKind | null>(null);
  // Ảnh vừa chọn, đang chờ chủ kênh chọn vùng cắt
  const [cropping, setCropping] = useState<{ file: File; kind: ImageKind } | null>(null);
  const [name, setName] = useState<string | null>(null);
  const [description, setDescription] = useState<string | null>(null);
  const avatarInput = useRef<HTMLInputElement>(null);
  const bannerInput = useRef<HTMLInputElement>(null);

  if (isLoading || !channel) {
    return <Skeleton className="h-64 w-full" />;
  }

  const nameValue = name ?? channel.name;
  const descriptionValue = description ?? channel.description ?? "";
  const dirty = nameValue.trim() !== channel.name || descriptionValue.trim() !== (channel.description ?? "");

  /** Gửi các trường đã đổi lên server rồi làm mới thông tin người đang đăng nhập (ảnh / tên trên thanh trên cùng). */
  const save = async (patch: { name?: string; description?: string; picture?: string; backgroundImgUrl?: string }, done: string) => {
    try {
      await updateChannel(patch).unwrap();
      await refresh();
      toastEmitter.success(done);
      return true;
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không lưu được thay đổi, vui lòng thử lại."));
      return false;
    }
  };

  const onPickImage = (kind: ImageKind, file: File | undefined) => {
    if (!file) return;
    if (!file.type.startsWith("image/")) {
      toastEmitter.error("Tệp phải là ảnh.");
      return;
    }
    if (file.size > SOURCE_MAX_MB * 1024 * 1024) {
      toastEmitter.error(`Ảnh quá lớn (tối đa ${SOURCE_MAX_MB} MB).`);
      return;
    }
    setCropping({ file, kind });
  };

  /** Chủ kênh đã chọn vùng cắt: tải ảnh đã cắt lên rồi gắn vào kênh. */
  const onCropped = async (kind: ImageKind, file: File) => {
    setCropping(null);
    setUploading(kind);
    try {
      const { url } = await uploadImage({ file, kind }).unwrap();
      await save(kind === "avatar" ? { picture: url } : { backgroundImgUrl: url }, "Đã cập nhật ảnh");
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không tải được ảnh lên, vui lòng thử lại."));
    } finally {
      setUploading(null);
    }
  };

  const onSave = async () => {
    const ok = await save({ name: nameValue, description: descriptionValue }, "Đã lưu thay đổi");
    if (ok) {
      setName(null);
      setDescription(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <Label>Ảnh bìa</Label>
        <p className="text-sm text-muted-foreground">
          Nên dùng ảnh ngang, từ 2048 px bề ngang trở lên. Sau khi chọn ảnh, anh chọn vùng muốn giữ (khung ngang 6:1).
        </p>
        <div className="relative aspect-[16/5] w-full max-w-2xl overflow-hidden rounded-xl bg-gradient-to-r from-gray-200 via-gray-100 to-gray-300 md:aspect-[6/1]">
          {channel.backgroundImgUrl && (
            <Image src={channel.backgroundImgUrl} alt="Ảnh bìa" fill sizes="672px" className="object-cover" />
          )}
        </div>
        <div className="flex gap-2">
          <Button type="button" variant="outline" disabled={uploading !== null || isSaving} onClick={() => bannerInput.current?.click()}>
            {uploading === "banner" ? "Đang tải lên..." : channel.backgroundImgUrl ? "Đổi ảnh bìa" : "Tải ảnh bìa lên"}
          </Button>
          {channel.backgroundImgUrl && (
            <Button
              type="button"
              variant="ghost"
              disabled={uploading !== null || isSaving}
              onClick={() => void save({ backgroundImgUrl: "" }, "Đã xoá ảnh bìa")}
            >
              Xoá
            </Button>
          )}
        </div>
        <input
          ref={bannerInput}
          type="file"
          accept="image/*"
          className="hidden"
          onChange={(e) => {
            onPickImage("banner", e.target.files?.[0]);
            e.target.value = "";
          }}
        />
      </div>

      <div className="space-y-2">
        <Label>Ảnh đại diện</Label>
        <p className="text-sm text-muted-foreground">
          Sau khi chọn ảnh, anh chọn vùng muốn giữ (khung tròn). Ảnh này hiện cả ở bình luận và trong các phòng xem chung.
        </p>
        <div className="flex items-center gap-4">
          <Image
            src={channel.picture || "https://placehold.co/96x96?text=W"}
            alt={channel.name}
            width={96}
            height={96}
            className="size-24 rounded-full bg-white object-cover"
          />
          <Button type="button" variant="outline" disabled={uploading !== null || isSaving} onClick={() => avatarInput.current?.click()}>
            {uploading === "avatar" ? "Đang tải lên..." : "Đổi ảnh đại diện"}
          </Button>
        </div>
        <input
          ref={avatarInput}
          type="file"
          accept="image/*"
          className="hidden"
          onChange={(e) => {
            onPickImage("avatar", e.target.files?.[0]);
            e.target.value = "";
          }}
        />
      </div>

      <div className="max-w-xl space-y-2">
        <Label htmlFor="channel-name">Tên kênh</Label>
        <Input id="channel-name" value={nameValue} maxLength={NAME_MAX} onChange={(e) => setName(e.target.value)} />
        <Label htmlFor="channel-description" className="pt-2">
          Mô tả
        </Label>
        <Textarea
          id="channel-description"
          value={descriptionValue}
          maxLength={DESCRIPTION_MAX}
          rows={5}
          placeholder="Giới thiệu kênh của bạn với người xem"
          onChange={(e) => setDescription(e.target.value)}
        />
        <p className="text-right text-xs text-muted-foreground">
          {descriptionValue.length}/{DESCRIPTION_MAX}
        </p>
        <Button onClick={onSave} disabled={!dirty || !nameValue.trim() || isSaving}>
          {isSaving ? "Đang lưu..." : "Lưu thay đổi"}
        </Button>
      </div>

      <ImageCropDialog
        file={cropping?.file ?? null}
        kind={cropping?.kind ?? "avatar"}
        onCancel={() => setCropping(null)}
        onConfirm={(cropped) => cropping && void onCropped(cropping.kind, cropped)}
      />
    </div>
  );
}
