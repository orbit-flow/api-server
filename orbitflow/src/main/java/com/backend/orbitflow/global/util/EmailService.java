package com.backend.orbitflow.global.util;

import com.backend.orbitflow.global.error.GlobalErrorCode;
import com.backend.orbitflow.global.error.exception.GlobalException;
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
            throw new GlobalException(GlobalErrorCode.MAIL_SEND_ERROR);
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
}
