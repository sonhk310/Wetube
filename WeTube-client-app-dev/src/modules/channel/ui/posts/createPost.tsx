"use client";

import { useState } from "react";
import { ImagePlus, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import Image from "next/image";
import {useUploadImageMutation} from "@/app/api/postApi";

interface CreatePostFormProps {
    userId: string;
    onSubmit: (data: {
        content: string;
        imageUrl?: string;
        poll?: {
            options: { optionText: string }[];
        } | null;
    }) => void;
}

export default function CreatePostForm({ userId: _userId, onSubmit }: CreatePostFormProps) {
  const [content, setContent] = useState("");
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [preview, setPreview] = useState<string | null>(null);

  const [pollOptions, setPollOptions] = useState<string[]>([]);
  const [newOption, setNewOption] = useState("");

    //   const [uploadImage, { isLoading: isUploading }] = useUploadImageMutation();
  const [uploadImage] = useUploadImageMutation();
    

  const handleImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setPreview(URL.createObjectURL(file));

      try {
        const res = await uploadImage(file).unwrap();
        if (res.url) {
          setImageUrl(res.url);
          console.log("📸 Uploaded imageUrl:", res.url);
        }
      } catch (err) {
        console.error("Upload failed:", err);
        setPreview(null);
      }
    }
  };

  const handleRemoveImage = () => {
    setImageUrl(null);
    setPreview(null);
  };

  const handleAddOption = () => {
    if (newOption.trim()) {
      setPollOptions([...pollOptions, newOption.trim()]);
      setNewOption("");
    }
  };

  const handleRemoveOption = (index: number) => {
    setPollOptions(pollOptions.filter((_, i) => i !== index));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim() && !imageUrl && pollOptions.length === 0) return;

    const poll =
      pollOptions.length > 0
        ? { options: pollOptions.map((opt) => ({ optionText: opt })) }
        : null;

    onSubmit({ content, imageUrl: imageUrl ?? undefined, poll });
    setContent("");
    setImageUrl(null);
    setPreview(null);
    setPollOptions([]);
  };

  return (
    <Card className="w-full mb-4 rounded-2xl shadow-md">
      <CardContent>
        <form
          onSubmit={handleSubmit}
          className="flex flex-col gap-3"
        >
          {/* Input text */}
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="Viết gì đó..."
            className="w-full resize-none rounded-xl border border-gray-300 p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            rows={3}
          />

          {/* Preview image */}
          {preview && (
            <div className="relative w-full">
              <Image
                src={preview}
                alt="Preview"
                width={500}
                height={300}
                className="rounded-xl"
              />
              <button
                type="button"
                onClick={handleRemoveImage}
                className="absolute top-2 right-2 bg-black/60 rounded-full p-1 text-white"
              >
                <X size={16} />
              </button>
            </div>
          )}

          {/* Poll options */}
          <div className="flex flex-col gap-2">
            <div className="flex gap-2">
              <input
                type="text"
                value={newOption}
                onChange={(e) => setNewOption(e.target.value)}
                placeholder="Thêm lựa chọn bình chọn..."
                className="flex-1 rounded-xl border border-gray-300 p-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <Button
                type="button"
                onClick={handleAddOption}
                disabled={!newOption.trim()}
              >
                Add
              </Button>
            </div>

            {pollOptions.length > 0 && (
              <ul className="flex flex-col gap-1">
                {pollOptions.map((opt, idx) => (
                  <li
                    key={idx}
                    className="flex justify-between items-center rounded-md border p-2 text-sm"
                  >
                    <span>{opt}</span>
                    <button
                      type="button"
                      onClick={() => handleRemoveOption(idx)}
                      className="text-red-500 hover:underline"
                    >
                      Del
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          {/* Action buttons */}
          <div className="flex justify-between items-center">
            <label className="flex items-center gap-2 cursor-pointer text-gray-600 hover:text-blue-600">
              <ImagePlus size={20} />
              <span className="text-sm">Image</span>
              <input
                type="file"
                accept="image/*"
                className="hidden"
                onChange={handleImageChange}
              />
            </label>

            <Button
              type="submit"
              disabled={
                !content.trim() && !imageUrl && pollOptions.length === 0
              }
              className="rounded-xl px-4"
            >
              Post
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
