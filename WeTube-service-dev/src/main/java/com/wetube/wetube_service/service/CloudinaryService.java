package com.wetube.wetube_service.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public String uploadVideo(MultipartFile videoFile) {
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


        return cloudinary.url()
                .resourceType("video")
                .format("mp4")
                .transformation(
                        new Transformation()
                                .videoCodec("h264") 
                                .quality("auto")
                )
                .secure(true) 
                .generate(publicId);
    }

    public String uploadThumbnail(MultipartFile thumbnailFile) {
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
            // Tách publicId từ URL (Cloudinary lưu video dưới dạng <folder>/<publicId>)
            String publicId = extractPublicId(videoUrl);

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


    private String extractPublicId(String url) {
        String withoutExtension = url.substring(0, url.lastIndexOf('.'));
        return withoutExtension.substring(withoutExtension.indexOf("/upload/") + 8);
    }
}
