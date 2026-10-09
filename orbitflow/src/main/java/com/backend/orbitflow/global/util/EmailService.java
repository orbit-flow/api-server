package com.backend.orbitflow.global.util;

import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.error.GlobalErrorCode;


import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Transactional
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
