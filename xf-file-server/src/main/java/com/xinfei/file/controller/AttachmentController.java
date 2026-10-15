package com.xinfei.file.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 附件上传/查看（POD 签收照片、异常上报凭证等）。
 *
 * <p>上传契约对齐三端 BFF 的 AttachmentClient（isSingle/id/bizId 为历史兼容位、不落维度）；
 * 查看 URL 经网关匿名路由 /api/file/view/**，token 无法附加到 img src，故 uuid 文件名即访问凭证。</p>
 */
@RestController
public class AttachmentController {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "pdf");
    private static final Pattern BIZ_TYPE = Pattern.compile("^[a-z0-9-]{1,32}$");
    private static final DateTimeFormatter YYYYMM = DateTimeFormatter.ofPattern("yyyyMM");

    private final Path uploadRoot;
    private final String publicBaseUrl;

    public AttachmentController(
            @Value("${xinfei.file.upload-root:/data/attachments}") String uploadRoot,
            @Value("${xinfei.file.public-base-url:http://192.168.20.130:8760/api/file/view}") String publicBaseUrl) {
        this.uploadRoot = Path.of(uploadRoot).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
    }

    @PostMapping("/attachment/upload")
    public Map<String, Object> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isSingle", required = false) Boolean isSingle,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "bizId", required = false) String bizId,
            @RequestParam(value = "bizType", required = false, defaultValue = "common") String bizType) throws IOException {

        if (file.isEmpty()) {
            return body(1, "文件为空", null);
        }
        if (!BIZ_TYPE.matcher(bizType).matches()) {
            return body(1, "非法业务标识", null);
        }
        String original = UriUtils.decode(
                file.getOriginalFilename() == null ? "" : Path.of(file.getOriginalFilename()).getFileName().toString(),
                StandardCharsets.UTF_8);
        String ext = extensionOf(original);
        if (ext == null || !ALLOWED_EXT.contains(ext)) {
            return body(1, "不支持的文件类型，仅允许：" + ALLOWED_EXT, null);
        }

        String month = LocalDate.now().format(YYYYMM);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = uploadRoot.resolve(bizType).resolve(month);
        Files.createDirectories(dir);
        // 先落临时文件再原子移动，避免半写文件被 view 读到
        Path tmp = Files.createTempFile(dir, "upload-", ".tmp");
        try (var in = file.getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(tmp, dir.resolve(filename), StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(tmp);
        }

        return body(0, "success", publicBaseUrl + "/" + bizType + "/" + month + "/" + filename);
    }

    /**
     * 匿名查看。经网关路由 /api/file/** StripPrefix=2 → 本服务 /view/**；
     * 同时保留 /attachment/view/** 旧形（容器间直连/历史 URL 兼容）。
     */
    @GetMapping({"/view/**", "/attachment/view/**"})
    public ResponseEntity<FileSystemResource> view(jakarta.servlet.http.HttpServletRequest request) {
        String uri = request.getRequestURI();
        String relative = null;
        for (String prefix : new String[]{"/view/", "/attachment/view/"}) {
            int idx = uri.indexOf(prefix);
            if (idx >= 0) {
                relative = uri.substring(idx + prefix.length());
                break;
            }
        }
        if (relative == null || relative.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Path target = uploadRoot.resolve(relative).normalize();
        if (!target.startsWith(uploadRoot) || !Files.isRegularFile(target)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        String ext = extensionOf(target.getFileName().toString());
        MediaType mediaType = ext == null ? MediaType.APPLICATION_OCTET_STREAM : guessMedia(ext);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header("Cache-Control", "public, max-age=86400")
                .body(new FileSystemResource(target));
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return ext.matches("[a-z0-9]{1,8}") ? ext : null;
    }

    private static MediaType guessMedia(String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "bmp" -> MediaType.parseMediaType("image/bmp");
            case "pdf" -> MediaType.APPLICATION_PDF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    private static Map<String, Object> body(int code, String msg, String data) {
        return Map.of("code", code, "msg", msg, "data", data == null ? "" : data);
    }
}
