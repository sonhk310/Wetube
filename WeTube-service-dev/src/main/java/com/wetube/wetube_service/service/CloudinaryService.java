package com.wetube.wetube_service.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.EagerTransformation;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /** Video đã tải lên: link phát + kích thước / thời lượng Cloudinary đo được (null nếu không có). */
    public record UploadedVideo(String url, Integer width, Integer height, Float duration) {
    }

    public String uploadVideo(MultipartFile videoFile) {
        return uploadVideoWithMeta(videoFile).url();
    }

    public UploadedVideo uploadVideoWithMeta(MultipartFile videoFile) {
        if (videoFile == null || videoFile.isEmpty()) {
            throw new IllegalArgumentException("File video is required");
        }

        final Map<?, ?> result;
        try {
            result = cloudinary.uploader().upload(
                    videoFile.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "video"
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException("Upload to Cloudinary failed", e);
        }

        final Object publicIdObj = result.get("public_id");
        if (publicIdObj == null) {
            throw new IllegalStateException("No public_id returned from Cloudinary");
        }
        String publicId = publicIdObj.toString();

        // Bản chuyển mã chỉ được Cloudinary tạo khi có người mở link lần đầu; trong lúc tạo (vài phút với
        // video dài) nó phát kiểu vừa mã hoá vừa gửi, không tua được và trình duyệt thấy thời lượng sai.
        // File đã là MP4 H.264 thì phát thẳng file gốc; còn lại thì xin tạo sẵn bản chuyển mã ngay.
        boolean asUploaded = playsAsUploaded(result);
        if (!asUploaded) {
            requestTranscode(publicId);
        }
        System.out.println("Video " + publicId + ": " + result.get("format") + " " + result.get("video")
                + (asUploaded ? " -> phát file gốc" : " -> chuyển mã H.264"));
        return new UploadedVideo(playbackUrl(cloudinary, publicId, asUploaded), intOrNull(result.get("width")),
                intOrNull(result.get("height")), floatOrNull(result.get("duration")));
    }

    /** Trình duyệt nào cũng phát được (và tua được) file gốc: MP4, H.264 8-bit. */
    static boolean playsAsUploaded(Map<?, ?> uploadResult) {
        if (!"mp4".equals(uploadResult.get("format")) || !(uploadResult.get("video") instanceof Map<?, ?> video)) {
            return false;
        }
        Object pixFormat = video.get("pix_format");
        return "h264".equals(video.get("codec")) && (pixFormat == null || "yuv420p".equals(pixFormat));
    }

    static String playbackUrl(Cloudinary cloudinary, String publicId, boolean asUploaded) {
        var url = cloudinary.url().resourceType("video").format("mp4").secure(true);
        if (!asUploaded) {
            url.transformation(new Transformation().videoCodec("h264").quality("auto"));
        }
        return url.generate(publicId);
    }

    /** Phải trùng với transformation trong playbackUrl, nếu không Cloudinary tạo ra một bản khác. */
    static EagerTransformation transcodeEager() {
        return new EagerTransformation().videoCodec("h264").quality("auto").format("mp4");
    }

    /** Xin Cloudinary tạo sẵn bản q_auto,vc_h264 ở nền; lỗi thì bản đó vẫn được tạo khi có người xem. */
    private void requestTranscode(String publicId) {
        try {
            cloudinary.uploader().explicit(publicId, ObjectUtils.asMap(
                    "type", "upload",
                    "resource_type", "video",
                    "eager", List.of(transcodeEager()),
                    "eager_async", true));
        } catch (Exception e) {
            System.err.println("Không xin được chuyển mã trước cho video " + publicId + ": " + e.getMessage());
        }
    }

    private static Integer intOrNull(Object value) {
        return value instanceof Number n ? n.intValue() : null;
    }

    private static Float floatOrNull(Object value) {
        return value instanceof Number n ? n.floatValue() : null;
    }

    /** Link ảnh thu nhỏ: video ngang 300x200, Shorts 360x640 (9:16). */
    static String thumbnailUrl(Cloudinary cloudinary, String publicId, boolean vertical) {
        return cloudinary.url()
                .resourceType("image")
                .format("webp")
                .transformation(
                        new Transformation()
                                .width(vertical ? 360 : 300)
                                .height(vertical ? 640 : 200)
                                .crop("fill")
                )
                .secure(true)
                .generate(publicId);
    }

    public String uploadThumbnail(MultipartFile thumbnailFile, boolean vertical) {
        if (thumbnailFile == null || thumbnailFile.isEmpty()) {
            throw new IllegalArgumentException("File thumbnail is required");
        }

        final Map<?, ?> result;
        try {
            result = cloudinary.uploader().upload(
                    thumbnailFile.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "image"
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException("Upload to Cloudinary failed", e);
        }

        final Object publicIdObj = result.get("public_id");
        if (publicIdObj == null) {
            throw new IllegalStateException("No public_id returned from Cloudinary");
        }
        return thumbnailUrl(cloudinary, publicIdObj.toString(), vertical);
    }

    public String uploadImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("File thumbnail is required");
        }

        final Map<?, ?> result;
        try {
            result = cloudinary.uploader().upload(
                    imageFile.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "image"
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException("Upload to Cloudinary failed", e);
        }

        final Object publicIdObj = result.get("public_id");
        if (publicIdObj == null) {
            throw new IllegalStateException("No public_id returned from Cloudinary");
        }
        String publicId = publicIdObj.toString();

        return cloudinary.url()
                .resourceType("image")
                .format("webp")
                .transformation(
                        new Transformation()
                                .width(300)
                                .height(200)
                                .crop("fill")
                )
                .secure(true)
                .generate(publicId);
    }


        public void deleteVideo(String videoUrl) {
        try {
            if (!isCloudinaryUrl(videoUrl)) {
                return;
            }
            // publicId không gồm phần transformation (q_auto,vc_h264...), version và đuôi file
            String publicId = publicIdFromDeliveryUrl(videoUrl);

            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "video"));
            if ("ok".equals(result.get("result"))) {
                System.out.println("Xoá video thành công trên Cloudinary: " + publicId);
            } else {
                System.err.println("Không xoá được video trên Cloudinary: " + publicId + " - " + result);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi xoá video Cloudinary: " + e.getMessage());
        }
    }


    /** URL bản mp3 của video (Cloudinary tự chuyển định dạng khi được yêu cầu). */
    public String audioUrlFor(String videoUrl) {
        return cloudinary.url()
                .resourceType("video")
                .format("mp3")
                .secure(true)
                .generate(publicIdFromDeliveryUrl(videoUrl));
    }

    /** Upload file phụ đề WebVTT (dạng raw) và trả về URL công khai. */
    public String uploadSubtitle(String vtt, String publicId) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    vtt.getBytes(StandardCharsets.UTF_8),
                    ObjectUtils.asMap(
                            "resource_type", "raw",
                            "public_id", publicId,
                            "overwrite", true));
            return result.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Upload subtitle to Cloudinary failed", e);
        }
    }

    /**
     * Lấy public_id từ URL giao hàng của Cloudinary, bỏ phần transformation (vd. q_auto,vc_h264),
     * version (v123) và đuôi file.
     */
    static String publicIdFromDeliveryUrl(String url) {
        String path = url.substring(url.indexOf("/upload/") + "/upload/".length());
        String[] segments = path.split("/");
        int i = 0;
        while (i < segments.length - 1
                && (segments[i].matches("[a-z]{1,3}_[^/]*") || segments[i].matches("v\\d+"))) {
            i++;
        }
        String rest = String.join("/", java.util.Arrays.copyOfRange(segments, i, segments.length));
        int dot = rest.lastIndexOf('.');
        return dot > 0 ? rest.substring(0, dot) : rest;
    }

    /** Xoá ảnh (thumbnail) trên Cloudinary; URL không phải của Cloudinary hoặc rỗng thì bỏ qua. */
    public void deleteImage(String imageUrl) {
        try {
            if (!isCloudinaryUrl(imageUrl)) {
                return;
            }
            String publicId = publicIdFromDeliveryUrl(imageUrl);
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
            System.out.println("Xoá ảnh trên Cloudinary " + publicId + ": " + result);
        } catch (Exception e) {
            System.err.println("Lỗi khi xoá ảnh Cloudinary: " + e.getMessage());
        }
    }

    private static boolean isCloudinaryUrl(String url) {
        return url != null && url.contains("/upload/");
    }
}
