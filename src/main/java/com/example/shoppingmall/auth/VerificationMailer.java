package com.example.shoppingmall.auth;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class VerificationMailer {
    private final ObjectProvider<JavaMailSender> sender;
    private final String host;
    private final String from;

    public VerificationMailer(ObjectProvider<JavaMailSender> sender,
            @Value("${spring.mail.host:}") String host, @Value("${mall.mail-from:}") String from) {
        this.sender = sender;
        this.host = host;
        this.from = from;
    }

    public void checkConfiguration() {
        if (host.isBlank() || from.isBlank() || sender.getIfAvailable() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "メール送信の設定が完了していません。管理者にお問い合わせください。");
        }
    }

    public void send(String email, String code) {
        checkConfiguration();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("【ソソマーケット】会員登録の認証コード");
        message.setText("ソソマーケットの会員登録用認証コードです。\n\nCode: " + code
            + "\n\n5分以内に会員登録画面へ入力してください。"
            + "\nお心当たりがない場合は、このメールを破棄してください。");
        sender.getObject().send(message);
    }
}
