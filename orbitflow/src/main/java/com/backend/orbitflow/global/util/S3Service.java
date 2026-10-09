package com.backend.orbitflow.global.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.error.GlobalErrorCode;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    public String uploadFile(String dirName, MultipartFile file) {
        try {
            validateFile(file);

            String fileName = createFileName(getExtension(file.getOriginalFilename()));
            String fileKey = dirName + "/" + fileName;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileKey)
                    .contentType(file.getContentType())
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

    private void validateFile(MultipartFile file) {

        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CommonException(GlobalErrorCode.INVALID_FILE_TYPE);
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new CommonException(GlobalErrorCode.FILE_SIZE_EXCEED);
        }
    }

    private String createFileName(String ext) {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + "-"
                + UUID.randomUUID()
                + ext;
    }

    private String getExtension(String fileName) {
        if (fileName == null) {
            return "";
        }

        int lastIndex = fileName.lastIndexOf(".");

        if (lastIndex == -1) {
            return "";
        }

        return fileName.substring(lastIndex);
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

    public void deleteFile(String fileUrl) {
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

