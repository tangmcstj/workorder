package com.workorder.file;

import com.workorder.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final Path uploadRoot;

    public FileController(@Value("${app.upload-root}") String uploadRoot) {
        this.uploadRoot = Path.of(uploadRoot).toAbsolutePath().normalize();
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String suffix = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) {
            suffix = original.substring(dot).replaceAll("[^A-Za-z0-9.]", "");
        }
        String day = DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now());
        Path dir = uploadRoot.resolve(day).normalize();
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + suffix;
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        file.transferTo(target);
        String path = "/api/files/" + day + "/" + filename;
        return ApiResponse.ok(Map.of(
                "name", original,
                "path", path,
                "url", path,
                "size", file.getSize()
        ));
    }

    @GetMapping("/{day}/{filename:.+}")
    public ResponseEntity<Resource> get(@PathVariable String day, @PathVariable String filename, HttpServletRequest request) throws IOException {
        Path file = uploadRoot.resolve(day).resolve(filename).normalize();
        if (!file.startsWith(uploadRoot) || !Files.exists(file) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = resource(file);
        String contentType = request.getServletContext().getMimeType(file.getFileName().toString());
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                .body(resource);
    }

    private Resource resource(Path file) throws MalformedURLException {
        return new UrlResource(file.toUri());
    }
}
