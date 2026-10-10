package com.backend.orbitflow.global.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.error.GlobalErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    // 허용하는 이미지 형식 (확장자·Content-Type은 파일 내용으로 확인한 형식에서 결정)
    private static final String JPEG = "image/jpeg";
    private static final String PNG = "image/png";
    private static final String WEBP = "image/webp";
    private static final String GIF = "image/gif";

    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    public String uploadFile(String dirName, MultipartFile file) {
        try {
            String contentType = validateFile(file);

            String fileName = createFileName(extensionOf(contentType));
            String fileKey = dirName + "/" + fileName;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileKey)
                    .contentType(contentType)
                    .contentLength(file.getSize())
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(
                            file.getInputStream(),
                            file.getSize()
                    )
            );
            return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + fileKey;

        } catch (IOException e) {
            throw new CommonException(GlobalErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    // 허용 형식(jpeg, png, webp, gif) 여부와 파일 시작 바이트(매직 넘버)가 선언된 Content-Type과 일치하는지 검증, 확인된 형식 반환
    private String validateFile(MultipartFile file) throws IOException {

        String declared = file.getContentType();
        if (declared == null) {
            throw new CommonException(GlobalErrorCode.INVALID_FILE_TYPE);
        }
        declared = declared.split(";")[0].trim().toLowerCase(Locale.ROOT);

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new CommonException(GlobalErrorCode.FILE_SIZE_EXCEED);
        }

        String detected;
        try (InputStream in = file.getInputStream()) {
            detected = detectImageType(in.readNBytes(12));
        }
        if (detected == null || !detected.equals(declared)) {
            throw new CommonException(GlobalErrorCode.INVALID_FILE_TYPE);
        }
        return detected;
    }

    // 시작 바이트로 이미지 형식 판별 (허용 형식이 아니면 null)
    private String detectImageType(byte[] head) {
        if (startsWith(head, 0xFF, 0xD8, 0xFF)) {
            return JPEG;
        }
        if (startsWith(head, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return PNG;
        }
        if (startsWith(head, 'G', 'I', 'F', '8') && head.length >= 6
                && (head[4] == '7' || head[4] == '9') && head[5] == 'a') {
            return GIF;
        }
        // RIFF(0~3) + 파일 크기(4~7) + WEBP(8~11)
        if (startsWith(head, 'R', 'I', 'F', 'F') && head.length >= 12
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return WEBP;
        }
        return null;
    }

    private boolean startsWith(byte[] head, int... expected) {
        if (head.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((head[i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private String createFileName(String ext) {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + "-"
                + UUID.randomUUID()
                + ext;
    }

    // 원본 파일명이 아닌 검증된 형식으로 확장자 결정
    private String extensionOf(String contentType) {
        return switch (contentType) {
            case JPEG -> ".jpg";
            case PNG -> ".png";
            case WEBP -> ".webp";
            case GIF -> ".gif";
            default -> throw new CommonException(GlobalErrorCode.INVALID_FILE_TYPE);
        };
    }

    public List<String> uploadFiles(String dirName, List<MultipartFile> files) {
        return files.stream()
                .map(file -> uploadFile(dirName, file))
                .collect(Collectors.toList());
    }

    // 이 서비스의 버킷에 업로드한 파일인지 (소셜 프로필 등 외부 URL은 삭제 대상 아님)
    public boolean isManagedFile(String fileUrl) {
        return fileUrl != null && fileUrl.startsWith("https://" + bucket + ".s3." + region + ".amazonaws.com/");
    }

    // 이 버킷의 관리 대상이 아닌 URL(외부 URL 등)은 삭제하지 않고 무시
    public void deleteFile(String fileUrl) {
        if (!isManagedFile(fileUrl)) {
            log.warn("관리 대상이 아닌 파일 URL이라 삭제를 건너뜁니다. url={}", fileUrl);
            return;
        }
        try {
            String key = extractKeyFromUrl(fileUrl);

            s3Client.deleteObject(builder -> builder
                    .bucket(bucket)
                    .key(key)
                    .build()
            );

        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            throw new CommonException(GlobalErrorCode.FAILED_DELETE_IMAGE);
        }
    }

    private String extractKeyFromUrl(String fileUrl) {
        try {
            URL url = URI.create(fileUrl).toURL();

            String path = url.getPath();

            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            return path;

        } catch (IllegalArgumentException | MalformedURLException e) {
            throw new CommonException(GlobalErrorCode.INVALID_FILE_URL);
        }
    }

    public void deleteFiles(List<String> fileUrls) {
        for (String fileUrl : fileUrls) {
            deleteFile(fileUrl);
        }
    }
}

