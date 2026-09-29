package com.backend.orbitflow.global.util;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.backend.orbitflow.global.error.GlobalErrorCode;
import com.backend.orbitflow.global.error.exception.GlobalException;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class S3Service {

    private final AmazonS3Client amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String uploadFile(String dirName, MultipartFile file) {
        try {
            validateFile(file);

            String fileName = createFileName(getExtension(file.getOriginalFilename()));
            String fileUrl = dirName + "/" + fileName;

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            amazonS3Client.putObject(
                new PutObjectRequest(bucket, fileUrl, file.getInputStream(), metadata)
                        .withCannedAcl(CannedAccessControlList.PublicRead)
            );

            return amazonS3Client.getUrl(bucket, fileUrl).toString();
        } catch (IOException e) {
            throw new GlobalException(GlobalErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    private void validateFile(MultipartFile file) {

        String contentType = file.getContentType();
        
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new GlobalException(GlobalErrorCode.INVALID_FILE_TYPE);
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new GlobalException(GlobalErrorCode.FILE_SIZE_EXCEED);
        }
    }

    private String createFileName(String ext) {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + "-"
                + UUID.randomUUID().toString()
                + ext;
            
    }

    private String getExtension(String fileName) {
        if (fileName == null) return "";
        int lastIndex = fileName.lastIndexOf(".");
        if (lastIndex == -1) return "";
        return fileName.substring(lastIndex);
    }

    public List<String> uploadFIles(String dirName, List<MultipartFile> files) {
        return files.stream()
                .map(file -> uploadFile(dirName, file))
                .collect(Collectors.toList());
    }

    public void deleteFile(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            amazonS3Client.deleteObject(bucket, key);
        } catch (GlobalException e) {
            throw e;
        } catch (Exception e) {
            throw new GlobalException(GlobalErrorCode.FAILED_DELETE_IMAGE);
        }
    }

    private String extractKeyFromUrl(String fileUrl) {
        try {
            java.net.URL url = java.net.URI.create(fileUrl).toURL();
            String path = url.getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            return path;
        } catch (IllegalArgumentException | java.net.MalformedURLException e) {
            throw new GlobalException(GlobalErrorCode.INVALID_FILE_URL);
        }
    }

    public  void deleteFiles(List<String> fileUrls) {
        for (String fileUrl : fileUrls) {
            deleteFile(fileUrl);
        }
    } 
}
