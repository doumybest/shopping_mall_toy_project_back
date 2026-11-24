package com.example.shoppingmall.auth;

import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailVerificationService {
    public record EmailRequest(@NotBlank @Email @Size(max=200) String email) {}
    public record VerifyRequest(@NotBlank @Email @Size(max=200) String email,@NotNull @Pattern(regexp="[0-9]{6}") String code) {}
    public record SendResult(int expiresInSeconds,int retryAfterSeconds) {}
    public record VerifyResult(int validForSeconds) {}

    private static final int CODE_TTL=300, VERIFIED_TTL=600, RESEND_DELAY=60;
    private static final int MAX_ATTEMPTS=5, EMAIL_HOURLY_LIMIT=5, IP_HOURLY_LIMIT=20;
    private final SecureRandom random=new SecureRandom();
    private final EmailVerificationRepository repository;
    private final EmailSendLimitRepository limits;
    private final MemberRepository members;
    private final PasswordEncoder encoder;
    private final VerificationMailer mailer;

    public EmailVerificationService(EmailVerificationRepository repository,EmailSendLimitRepository limits,
            MemberRepository members,PasswordEncoder encoder,VerificationMailer mailer) {
        this.repository=repository;this.limits=limits;this.members=members;this.encoder=encoder;this.mailer=mailer;
    }
    public static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    public static String fingerprint(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    // Commit failed guesses and limits; rollback would allow unlimited retries.
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public SendResult send(String address,String sessionHash,String remoteAddress) {
        mailer.checkConfiguration();
        String email=normalize(address);
        Instant now=Instant.now();
        limitIp(remoteAddress,now);
        EmailVerificationEntity challenge=repository.findLockedByEmail(email).orElse(null);
        if(challenge==null) {
            challenge=new EmailVerificationEntity();
            challenge.setEmail(email);challenge.setWindowStartedAt(now);
            challenge=repository.saveAndFlush(challenge);
        }
        now=Instant.now();
        if(members.existsByEmail(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"登録済みのメールアドレスです。ログインしてください。");
        if(challenge.getSentAt()!=null && now.isBefore(challenge.getSentAt().plusSeconds(RESEND_DELAY)))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"認証コードは60秒後に再送信できます。");
        boolean newWindow=!now.isBefore(challenge.getWindowStartedAt().plusSeconds(3600));
        int sends=newWindow?0:challenge.getSendCount();
        if(sends>=EMAIL_HOURLY_LIMIT)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"このメールアドレスへの送信回数が上限に達しました。しばらくしてからお試しください。");
        String code=String.format(Locale.ROOT,"%06d",random.nextInt(1_000_000));
        challenge.setCodeHash(encoder.encode(code));challenge.setSessionHash(sessionHash);
        challenge.setExpiresAt(now.plusSeconds(CODE_TTL));challenge.setVerifiedAt(null);challenge.setConsumedAt(null);
        challenge.setSentAt(now);challenge.setAttemptCount(0);challenge.setSendCount(sends+1);
        if(newWindow) challenge.setWindowStartedAt(now);
        try { mailer.send(email,code); }
        catch(MailException exception) {
            challenge.setCodeHash(null);challenge.setExpiresAt(now);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"認証メールを送信できませんでした。しばらくしてからお試しください。");
        }
        int remaining=Math.max(0,(int)java.time.Duration.between(Instant.now(),now.plusSeconds(CODE_TTL)).getSeconds());
        return new SendResult(remaining,RESEND_DELAY);
    }

    private void limitIp(String remoteAddress,Instant now) {
        String key=fingerprint(remoteAddress);
        EmailSendLimitEntity limit=limits.findLockedByKey(key).orElse(null);
        if(limit==null) {
            limit=new EmailSendLimitEntity();limit.setSourceHash(key);limit.setWindowStartedAt(now);limit.setSendCount(1);
            limits.saveAndFlush(limit);
            return;
        }
        boolean newWindow=!now.isBefore(limit.getWindowStartedAt().plusSeconds(3600));
        if(!newWindow && limit.getSendCount()>=IP_HOURLY_LIMIT)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"認証メールのリクエストが多すぎます。しばらくしてからお試しください。");
        if(newWindow) { limit.setWindowStartedAt(now);limit.setSendCount(1); }
        else limit.setSendCount(limit.getSendCount()+1);
    }

    @Transactional(noRollbackFor=ResponseStatusException.class)
    public VerifyResult verify(String address,String code,String sessionHash) {
        EmailVerificationEntity challenge=repository.findLockedByEmail(normalize(address)).orElse(null);
        Instant now=Instant.now();
        if(challenge==null || !sessionHash.equals(challenge.getSessionHash()) || challenge.getConsumedAt()!=null ||
                challenge.getCodeHash()==null || challenge.getExpiresAt()==null || !now.isBefore(challenge.getExpiresAt()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"認証コードが期限切れか、リクエストしたブラウザーと異なります。再送信してください。");
        if(challenge.getVerifiedAt()!=null)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"認証済みです。会員登録を完了してください。");
        if(challenge.getAttemptCount()>=MAX_ATTEMPTS)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"認証コードの入力回数が上限に達しました。再送信してください。");
        challenge.setAttemptCount(challenge.getAttemptCount()+1);
        if(!encoder.matches(code,challenge.getCodeHash()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"認証コードが一致しません。残りの入力回数: "+(MAX_ATTEMPTS-challenge.getAttemptCount()));
        challenge.setVerifiedAt(now);
        return new VerifyResult(VERIFIED_TTL);
    }

    @Transactional(propagation=Propagation.MANDATORY)
    public void consume(String address,String sessionHash) {
        EmailVerificationEntity challenge=repository.findLockedByEmail(normalize(address)).orElse(null);
        Instant now=Instant.now();
        if(challenge==null || !sessionHash.equals(challenge.getSessionHash()) || challenge.getVerifiedAt()==null ||
                challenge.getConsumedAt()!=null || !now.isBefore(challenge.getVerifiedAt().plusSeconds(VERIFIED_TTL)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"メール認証を先に完了し、認証から10分以内に会員登録してください。");
        challenge.setConsumedAt(now);challenge.setCodeHash(null);
    }
}
