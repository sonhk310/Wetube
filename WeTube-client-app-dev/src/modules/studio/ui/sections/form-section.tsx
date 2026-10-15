"use client";

import { Button } from "@/components/ui/button";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { Globe2Icon, Clock, Film, Loader2 } from "lucide-react";
import Link from "next/link";
import { useEffect, useState, useCallback } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import toast from "react-hot-toast";
import {
  useGetVideoFormDetailsQuery,
  useUpdateVideoDetailsMutation,
} from "@/app/api/videoApi";
import { ThumbnailSelector } from "@/components/ThumbnailSelector";
import { useRouter } from "next/navigation";
import { useDispatch } from "react-redux";
import { playlistApi } from "@/app/api/playlistApi";
import { useAuth } from "@/contexts/auth-context";

const MAX_THUMBNAIL_SIZE_MB = 5;
const MAX_THUMBNAIL_SIZE_BYTES = MAX_THUMBNAIL_SIZE_MB * 1024 * 1024;

const formSchema = z.object({
  title: z
    .string()
    .trim()
    .min(1, { message: "Title is required." })
    .max(250, { message: "Title must be 250 characters or fewer." }),
  description: z
    .string()
    .max(5000, { message: "Description must be 5000 characters or fewer." })
    .optional(),
  status: z.enum(["ACTIVE", "PRIVATE", "UNLISTED", "PENDING", "INACTIVE"]),
  tags: z
    .string()
    .max(200, { message: "Tags must be 200 characters or fewer." })
    .refine((value) => value === "" || /^(#\w+(\s+#\w+)*)$/.test(value), {
      message: 'Tags must be in the format "#tag1 #tag2"',
    })
    .optional(),
  thumbnailFile: z
    .instanceof(File)
    .refine(
      (file) => file.size <= MAX_THUMBNAIL_SIZE_BYTES,
      `Thumbnail file must be ${MAX_THUMBNAIL_SIZE_MB}MB or less.`
    )
    .optional(),
});
type VideoFormValues = z.infer<typeof formSchema>;

const formatDuration = (seconds: number) => {
  if (isNaN(seconds) || seconds < 0) return "00:00";
  const h = Math.floor(seconds / 3600)
    .toString()
    .padStart(2, "0");
  const m = Math.floor((seconds % 3600) / 60)
    .toString()
    .padStart(2, "0");
  const s = Math.floor(seconds % 60)
    .toString()
    .padStart(2, "0");
  if (h === "00") return `${m}:${s}`;
  return `${h}:${m}:${s}`;
};

interface FormSectionProps {
  readonly videoId: string;
}

export default function FormSection({ videoId }: FormSectionProps) {
  const { user, isLoading: isAuthLoading } = useAuth();
  
  const {
    data: video,
    isLoading,
    error,
  } = useGetVideoFormDetailsQuery(videoId, { skip: isAuthLoading });
  const [updateVideo, { isLoading: isUpdating }] =
    useUpdateVideoDetailsMutation();

  const router = useRouter();
  const dispatch = useDispatch();
  
  // Thumbnail states
  const [thumbnailPreview, setThumbnailPreview] = useState<string>("");
  const [resolution, setResolution] = useState<string>("");

  // Progress bar states
  const [progress, setProgress] = useState(0);
  // const [isSubmitting, setIsSubmitting] = useState(false);

  // Track form changes manually for file uploads
  const [hasChanges, setHasChanges] = useState(false);

  const form = useForm<VideoFormValues>({
    resolver: zodResolver(formSchema),
    mode: "onChange",
    defaultValues: {
      title: "",
      description: "",
      status: "ACTIVE",
      tags: "",
      thumbnailFile: undefined,
    },
  });

  // Watch for form changes
  useEffect(() => {
    const subscription = form.watch((value, { name }) => {
      if (name && video) {
        // Check if any field has changed from original values
        const titleChanged = value.title !== video.title;
        const descriptionChanged =
          value.description !== (video.description || "");
        const statusChanged = value.status !== video.status;
        const tagsChanged =
          value.tags !== video.tags.map((t) => `#${t.name}`).join(" ");
        const thumbnailChanged = !!value.thumbnailFile;

        setHasChanges(
          titleChanged ||
            descriptionChanged ||
            statusChanged ||
            tagsChanged ||
            thumbnailChanged
        );
      }
    });
    return () => subscription.unsubscribe();
  }, [form, video]);

  useEffect(() => {
    if (video) {
      form.reset({
        title: video.title,
        description: video.description || "",
        status: video.status,
        tags: video.tags.map((t) => `#${t.name}`).join(" "),
      });

      // Set initial thumbnail preview
      if (video.thumbnailUrl) {
        setThumbnailPreview(video.thumbnailUrl);
      }

      // Load video metadata
      const videoElement = document.createElement("video");
      videoElement.src = video.videoUrl;
      videoElement.crossOrigin = "anonymous";

      const handleMetadataLoaded = () => {
        setResolution(
          `${videoElement.videoWidth} x ${videoElement.videoHeight}`
        );
      };
      videoElement.addEventListener("loadedmetadata", handleMetadataLoaded);

      return () => {
        videoElement.removeEventListener(
          "loadedmetadata",
          handleMetadataLoaded
        );
      };
    }
  }, [video, form]);

  useEffect(() => {
    return () => {
      if (thumbnailPreview && thumbnailPreview.startsWith("blob:")) {
        URL.revokeObjectURL(thumbnailPreview);
      }
    };
  }, [thumbnailPreview]);

  const handleThumbnailChange = useCallback(
    (selectedFile: File) => {
      if (thumbnailPreview && thumbnailPreview.startsWith("blob:")) {
        URL.revokeObjectURL(thumbnailPreview);
      }
      const newPreviewUrl = URL.createObjectURL(selectedFile);
      setThumbnailPreview(newPreviewUrl);
      form.setValue("thumbnailFile", selectedFile, { shouldValidate: true });
      setHasChanges(true); // Manually set changes for file upload
    },
    [form, thumbnailPreview]
  );

  const onSubmit = async (values: VideoFormValues) => {
    // setIsSubmitting(true);
    setProgress(0);

    // Simulate progress
    const progressInterval = setInterval(() => {
      setProgress((prev) => {
        if (prev >= 90) {
          clearInterval(progressInterval);
          return 90;
        }
        return prev + Math.random() * 15;
      });
    }, 200);

    try {
      const formData = new FormData();
      formData.append("title", values.title);
      formData.append("description", values.description || "");
      formData.append("status", values.status);
      formData.append("tags", values.tags || "");

      if (values.thumbnailFile) {
        formData.append("thumbnailFile", values.thumbnailFile);
      }

      await updateVideo({ videoId, formData }).unwrap();

      // Force invalidate playlist cache manually với user-specific tag
      if (user?.sub) {
        dispatch(
          playlistApi.util.invalidateTags([
            "Playlist",
            { type: "Playlist", id: `USER_${user.sub}` },
          ])
        );
      }

      // Complete progress
      setProgress(100);

      // Wait a bit to show 100% progress
      await new Promise((resolve) => setTimeout(resolve, 500));

      toast.success("Update successfully!");

      // Navigate after showing success
      setTimeout(() => {
        router.push("/studio");
      }, 300);
    } catch (err) {
      console.error("Failed to update video:", err);
      toast.error("Failed to update video!");
      setProgress(0);
      // setIsSubmitting(false);
      clearInterval(progressInterval);
    }
  };

  const renderButtonContent = () => {
    if (isUpdating) {
      return (
        <>
          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
          Saving...
        </>
      );
    }
    return "Save";
  };

  if (isLoading) return <FormSectionSkeleton />;
  if (error || !video)
    return <p className="text-red-500">Failed to load video details.</p>;

  return (
    <div className="flex flex-col h-full max-h-[calc(100vh-80px)] relative">
      <Form {...form}>
        <form
          onSubmit={form.handleSubmit(onSubmit)}
          className="flex flex-col flex-1 min-h-0"
        >
          <div className="flex items-center justify-between pb-4 border-b flex-shrink-0">
            <h1 className="text-2xl font-bold">Video Details</h1>
            <Button
              type="submit"
              disabled={isUpdating || (!form.formState.isDirty && !hasChanges)}
            >
              {renderButtonContent()}
            </Button>
          </div>

          <div className="flex-1 grid grid-cols-1 lg:grid-cols-5 gap-x-8 pt-6 overflow-hidden">
            <div className="lg:col-span-3 space-y-8 overflow-y-auto pr-6 pb-12">
              <FormField
                name="title"
                control={form.control}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="font-semibold">Title</FormLabel>
                    <FormControl>
                      <Textarea
                        {...field}
                        placeholder="Input video title"
                        disabled={isUpdating}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                name="description"
                control={form.control}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="font-semibold">Description</FormLabel>
                    <FormControl>
                      <Textarea
                        {...field}
                        rows={12}
                        placeholder="Describe your video..."
                        disabled={isUpdating}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                name="thumbnailFile"
                control={form.control}
                render={() => (
                  <FormItem>
                    <FormControl>
                      <div className="w-full max-w-xs">
                        <ThumbnailSelector
                          thumbnailPreview={thumbnailPreview}
                          onThumbnailChange={handleThumbnailChange}
                          disabled={isUpdating}
                        />
                      </div>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                name="tags"
                control={form.control}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="font-semibold">Tags</FormLabel>
                    <FormControl>
                      <Input
                        {...field}
                        placeholder="Enter tags, starting with #"
                        disabled={isUpdating}
                      />
                    </FormControl>
                    <FormMessage />
                    <p className="text-xs text-muted-foreground">
                      Tags help others find your video.
                    </p>
                  </FormItem>
                )}
              />
            </div>

            <div className="lg:col-span-2 space-y-6">
              <div className="bg-muted/30 rounded-2xl overflow-hidden">
                <div className="aspect-video">
                  <video
                    src={video.videoUrl}
                    controls
                    className="w-full rounded-2xl bg-black aspect-video"
                  />
                </div>
                <div className="p-4 space-y-2 bg-gray-300 mt-4 rounded-2xl">
                  <p className="text-xs text-muted-foreground">Video link</p>
                  <Link
                    href={`/watch/${video.id}`}
                    className="text-sm text-blue-600 break-all"
                  >{`${window.location.origin}/watch/${video.id}`}</Link>

                  {/* Progress Bar */}
                  {isUpdating && (
                    <div className="mt-4 space-y-2">
                      <div className="flex items-center justify-between text-sm">
                        <span className="text-muted-foreground">
                          Saving progress...
                        </span>
                        <span className="font-medium">
                          {Math.round(progress)}%
                        </span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2.5">
                        <div
                          className="bg-blue-600 h-2.5 rounded-full transition-all duration-300 ease-out"
                          style={{ width: `${progress}%` }}
                        />
                      </div>
                      {progress === 100 && (
                        <div className="flex items-center justify-center text-sm text-green-600 mt-2">
                          <span>✓ Saved successfully! Redirecting...</span>
                        </div>
                      )}
                    </div>
                  )}

                  <div className="flex items-center gap-x-6">
                    <div>
                      <p className="text-xs text-muted-foreground">Duration</p>
                      <div className="flex items-center gap-1.5 text-sm">
                        <Clock className="w-4 h-4" />
                        <span>{formatDuration(video.duration)}</span>
                      </div>
                    </div>

                    {resolution && (
                      <div>
                        <p className="text-xs text-muted-foreground">
                          Resolution
                        </p>
                        <div className="flex items-center gap-1.5 text-sm">
                          <Film className="w-4 h-4" />
                          <span>{resolution}</span>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </div>
              <FormField
                name="status"
                control={form.control}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="font-semibold">Status</FormLabel>
                    <Select
                      onValueChange={field.onChange}
                      value={field.value}
                      disabled={isUpdating}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="ACTIVE">
                          <div className="flex items-center gap-2">
                            <Globe2Icon className="size-4" /> Active
                          </div>
                        </SelectItem>
                        {/* <SelectItem value="PRIVATE">
                          <div className="flex items-center gap-2">
                            <LockIcon className="size-4" /> Private
                          </div>
                        </SelectItem> */}
                      </SelectContent>
                    </Select>
                  </FormItem>
                )}
              />
            </div>
          </div>
        </form>
      </Form>
    </div>
  );
}

const FormSectionSkeleton = () => {
  return (
    <div className="flex flex-col h-full max-h-[calc(100vh-80px)]">
      <div className="flex items-center justify-between mb-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-10 w-20" />
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 flex-1">
        <div className="space-y-6 lg:col-span-2">
          <div className="space-y-2">
            <Skeleton className="h-5 w-16" />
            <Skeleton className="h-20 w-full" />
          </div>
          <div className="space-y-2">
            <Skeleton className="h-5 w-24" />
            <Skeleton className="h-32 w-full" />
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-2">
              <Skeleton className="h-5 w-20" />
              <Skeleton className="aspect-video w-full" />
            </div>
          </div>
          <div className="space-y-2">
            <Skeleton className="h-5 w-16" />
            <Skeleton className="h-10 w-full" />
          </div>
          <div className="space-y-2">
            <Skeleton className="h-5 w-20" />
            <Skeleton className="h-10 w-full" />
          </div>
        </div>
        <div className="flex flex-col gap-4 lg:col-span-1">
          <Skeleton className="aspect-video w-full" />
          <div className="p-3 bg-gray-100 rounded space-y-2">
            <Skeleton className="h-4 w-20" />
            <Skeleton className="h-4 w-full" />
            <div className="flex gap-4 pt-2">
              <Skeleton className="h-4 w-16" />
              <Skeleton className="h-4 w-20" />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
