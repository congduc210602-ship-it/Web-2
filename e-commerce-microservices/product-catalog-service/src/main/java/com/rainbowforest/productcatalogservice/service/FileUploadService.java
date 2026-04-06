package com.rainbowforest.productcatalogservice.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileUploadService {

    // Khai báo tên thư mục sẽ chứa ảnh (nó sẽ tự tạo ra ngang hàng với thư mục src)
    private final String UPLOAD_DIR = "uploads/";

    public String uploadImage(MultipartFile file) throws IOException {
        // 1. Tạo thư mục nếu nó chưa tồn tại
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 2. Tạo một tên file ngẫu nhiên để tránh bị trùng tên khi lưu
        // (Ví dụ: a1b2c3d4-ao-thun.jpg)
        String originalFilename = file.getOriginalFilename();
        String fileName = UUID.randomUUID().toString() + "_" + originalFilename;

        // 3. Copy file từ request của React vào ổ cứng máy bạn
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 4. Trả về đường dẫn URL đi qua cổng API Gateway (8900) 
        // để React có thể lấy ảnh ra hiển thị được ngay lập tức
        return "http://localhost:8900/api/catalog/uploads/" + fileName;
    }
}