package com.wetube.wetube_service.service.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private final String uploadDir = "uploads"; // thư mục lưu ảnh (cùng cấp target/)

    public String save(MultipartFile file) {
        try {
            // tạo folder nếu chưa có
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // đặt tên file duy nhất
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filepath = Paths.get(uploadDir, filename);

            // ghi file
            Files.write(filepath, file.getBytes());

            // trả về URL để FE gọi
            return "/uploads/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Lỗi lưu file", e);
        }
    }
}
