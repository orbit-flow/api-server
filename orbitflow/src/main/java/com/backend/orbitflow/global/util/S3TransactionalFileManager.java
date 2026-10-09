package com.backend.orbitflow.global.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

// DB 트랜잭션 결과에 맞춘 S3 파일 정리
// 업로드 : 트랜잭션이 롤백되면 업로드한 파일 삭제 / 삭제 : 커밋된 뒤에만 실제 삭제
@Slf4j
@Component
@RequiredArgsConstructor
public class S3TransactionalFileManager {

    private final S3Service s3Service;

    public List<String> upload(String dirName, List<MultipartFile> files) {
        List<String> urls = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                urls.add(s3Service.uploadFile(dirName, file));
            }
        } finally {
            deleteOnRollback(List.copyOf(urls));
        }
        return urls;
    }

    public String upload(String dirName, MultipartFile file) {
        return upload(dirName, List.of(file)).get(0);
    }

    public void deleteAfterCommit(List<String> urls) {
        if (urls.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(urls);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(urls);
            }
        });
    }

    private void deleteOnRollback(List<String> urls) {
        if (urls.isEmpty() || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteQuietly(urls);
                }
            }
        });
    }

    // 파일 삭제 실패는 응답에 영향을 주지 않도록 로그만 남김
    private void deleteQuietly(List<String> urls) {
        for (String url : urls) {
            try {
                s3Service.deleteFile(url);
            } catch (Exception e) {
                log.warn("S3 파일 삭제 실패: {}", url, e);
            }
        }
    }
}
