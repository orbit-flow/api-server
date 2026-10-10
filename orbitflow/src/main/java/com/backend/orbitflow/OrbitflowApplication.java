package com.backend.orbitflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class OrbitflowApplication {

    public static void main(String[] args) {
        // 서버 시간대와 무관하게 LocalDateTime 기준을 한국 시간으로 통일
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(OrbitflowApplication.class, args);
    }

}
