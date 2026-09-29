package com.backend.orbitflow.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Configuration
public class S3Config {
    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Bean
    public AmazonS3Client amazonS3Client() {
        // AWS 자격증명을 위한 객체 생성
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        
        // AWS S3 클라이언트 생성
        return (AmazonS3Client) AmazonS3ClientBuilder.standard()
                .withRegion(region)                                            // 지역 설정
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))  // 자격증명 설정
                .build();
    }
}