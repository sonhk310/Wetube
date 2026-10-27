"use client";

import { useMemo } from "react";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import { Spinner } from "@/components/ui/spinner";
import { useGetVideoFormDetailsQuery } from "@/app/api/videoApi";
import { UploadWizard, type DraftVideo } from "@/components/upload/upload-wizard";

interface DraftEditDialogProps {
  videoId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/** Như YouTube Studio: bấm vào bản nháp thì mở lại hộp thoại tải lên để sửa tiếp và đăng. */
export function DraftEditDialog({ videoId, open, onOpenChange }: DraftEditDialogProps) {
  const { data, isError } = useGetVideoFormDetailsQuery(videoId, { skip: !open });

  const draft = useMemo<DraftVideo | null>(
    () =>
      data
        ? {
            id: data.id,
            title: data.title,
            description: data.description ?? "",
            tags: data.tags.map((t) => `#${t.name}`).join(" "),
            searchKeywords: data.searchKeywords ?? "",
            thumbnailUrl: data.thumbnailUrl || null,
            videoUrl: data.videoUrl,
            isShort: !!data.isShort,
          }
        : null,
    [data]
  );

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        showCloseButton={false}
        // Giống hộp thoại tải lên: grid minmax(0,1fr) để nội dung dài không kéo rộng / kéo cao modal
        className="bg-white rounded-lg p-0 sm:max-w-4xl h-[90vh] grid-cols-[minmax(0,1fr)] grid-rows-[minmax(0,1fr)]"
        onPointerDownOutside={(e) => e.preventDefault()}
        onEscapeKeyDown={(e) => e.preventDefault()}
      >
        {open && draft ? (
          <UploadWizard key={draft.id} draft={draft} onClose={() => onOpenChange(false)} />
        ) : (
          <div className="flex h-full flex-col items-center justify-center gap-3">
            <DialogTitle className="sr-only">Bản nháp</DialogTitle>
            {isError ? (
              <>
                <p className="text-gray-600">Không tải được bản nháp.</p>
                <button type="button" className="text-sm font-medium text-blue-600" onClick={() => onOpenChange(false)}>
                  Đóng
                </button>
              </>
            ) : (
              <Spinner />
            )}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
