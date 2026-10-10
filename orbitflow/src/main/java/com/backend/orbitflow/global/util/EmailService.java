package com.backend.orbitflow.global.util;

import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.error.GlobalErrorCode;


import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

// DB 작업이 없으므로 트랜잭션을 열지 않음 (SMTP 발송 동안 DB 커넥션 점유 방지)
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender emailSender;

    public void sendEmail(
            String toEmail,
            String title,
            String content
    ) {
        try {

            MimeMessage message = emailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(toEmail);
            helper.setSubject(title);
            helper.setText(content, true);
            helper.setReplyTo("orbitflow23@gmail.com");
            emailSender.send(message);

        } catch (Exception e) {
            throw new CommonException(GlobalErrorCode.MAIL_SEND_ERROR);
        }
    }

    public SimpleMailMessage createEmailForm(
            String toEmail,
            String title,
            String text
    ) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject(title);
        message.setText(text);

        return message;
    }

    public void sendWithdrawalEmail(String email, String name) {
        String title = "OrbitFlow 회원 탈퇴 처리 안내";
        String content = "<html>"
                + "<body>"
                + "<h2>" + name + "님의 회원 탈퇴가 처리되었습니다.</h2>"
                + "<p>탈퇴 신청일로부터 30일간 계정 정보가 보관되며, 이 기간 내에 다시 로그인하면 계정이 복구됩니다.</p>"
                + "<p>30일이 지나면 계정과 작성한 게시글·댓글·메시지가 영구 삭제됩니다.</p>"
                + "<footer style = 'color: grey; font-size: small;'>"
                + "<p>이 메일은 자동응답 메일입니다. 회신하지 마시기 바랍니다.</p>"
                + "</footer> </body> </html>";
        sendEmail(email, title, content);
    }

    // 비동기 발송 : 가입 여부에 따라 응답 시간이 달라져 계정 존재가 드러나지 않도록 요청 스레드에서 분리
    @Async
    public void sendPasswordResetEmail(String email, String resetLink) {
        String title = "OrbitFlow 비밀번호 재설정 안내";
        String content = "<html>"
                + "<body>"
                + "<h2>비밀번호 재설정</h2>"
                + "<p>아래 링크에서 새 비밀번호를 설정해 주세요. 링크는 30분 동안 한 번만 사용할 수 있습니다.</p>"
                + "<p><a href='" + resetLink + "'>비밀번호 재설정하기</a></p>"
                + "<p>요청하지 않으셨다면 이 메일을 무시해 주세요. 비밀번호는 변경되지 않습니다.</p>"
                + "<footer style = 'color: grey; font-size: small;'>"
                + "<p>이 메일은 자동응답 메일입니다. 회신하지 마시기 바랍니다.</p>"
                + "</footer> </body> </html>";
        try {
            sendEmail(email, title, content);
        } catch (RuntimeException e) {
            log.error("비밀번호 재설정 메일 발송 실패", e);
        }
    }

    @Async
    public void sendPasswordChangedEmail(String email, String name) {
        String title = "OrbitFlow 비밀번호 변경 안내";
        String content = "<html>"
                + "<body>"
                + "<h2>" + name + "님의 비밀번호가 변경되었습니다.</h2>"
                + "<p>모든 기기에서 로그아웃되었으며, 새 비밀번호로 다시 로그인해야 합니다.</p>"
                + "<p>본인이 변경하지 않았다면 즉시 비밀번호 찾기로 비밀번호를 재설정해 주세요.</p>"
                + "<footer style = 'color: grey; font-size: small;'>"
                + "<p>이 메일은 자동응답 메일입니다. 회신하지 마시기 바랍니다.</p>"
                + "</footer> </body> </html>";
        try {
            sendEmail(email, title, content);
        } catch (RuntimeException e) {
            log.error("비밀번호 변경 안내 메일 발송 실패", e);
        }
    }

    public void sendCodeByEmail(String email, String code) {
        String title = "OrbitFlow 이메일 인증 번호";
        String content = "<html>"
                + "<body>"
                + "<h1>OrbitFlow 인증 코드 : " + code + "</h1>"
                + "<p>해당 코드를 홈페이지에 입력하세요.</p>"
                + "<footer style = 'color: grey; font-size: small;'>"
                + "<p>이 메일은 자동응답 메일입니다. 회신하지 마시기 바랍니다.</p>"
                + "</footer> </body> </html>";
        try {
            sendEmail(email, title, content);
        } catch (RuntimeException e) {
            throw new CommonException(GlobalErrorCode.MAIL_SEND_ERROR);
        }

    }
}
