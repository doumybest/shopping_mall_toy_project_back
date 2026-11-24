package com.example.shoppingmall;

import com.example.shoppingmall.auth.EmailVerificationService;
import com.example.shoppingmall.auth.MemberService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class EmailVerificationIntegrationTest {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        TestSmtpServer.configure(registry);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:emailtest;MODE=Oracle;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    }
    @Autowired EmailVerificationService verification;
    @Autowired MemberService members;
    @Autowired JdbcTemplate jdbc;
    String email() { return UUID.randomUUID()+"@test.example"; }
    String binding() { return EmailVerificationService.fingerprint(UUID.randomUUID().toString()); }
    String send(String email,String binding) throws Exception {
        var response=verification.send(email,binding,UUID.randomUUID().toString());
        assertThat(response.expiresInSeconds()).isBetween(1,300);
        String code=TestSmtpServer.INSTANCE.codeFor(email);
        assertThat(code).matches("[0-9]{6}");
        assertThat(jdbc.queryForObject("SELECT code_hash FROM mall_email_verifications WHERE email=?",String.class,email)).startsWith("$2").isNotEqualTo(code);
        return code;
    }
    MemberService.Registration registration(String email) { return new MemberService.Registration(email,"MemberTest!123","테스터"); }
    void expectStatus(int status,org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,ex -> assertThat(ex.getStatusCode().value()).isEqualTo(status));
    }
    @Test void smtpDeliveryVerificationAndProofConsumptionAreRequired() throws Exception {
        String email=email(),session=binding();
        expectStatus(400,() -> members.register(registration(email),session));
        String code=send(email,session);
        expectStatus(400,() -> members.register(registration(email),session));
        assertThat(verification.verify(email.toUpperCase(),code,session).validForSeconds()).isEqualTo(600);
        var member=members.register(registration(email),session);
        assertThat(member.email()).isEqualTo(email);
        assertThat(jdbc.queryForObject("SELECT email_verified_at FROM mall_members WHERE id=?",Timestamp.class,member.id())).isNotNull();
        assertThat(jdbc.queryForObject("SELECT code_hash FROM mall_email_verifications WHERE email=?",String.class,email)).isNull();
        expectStatus(400,() -> members.register(registration(email),session));
    }
    @Test void wrongGuessesPersistAndLockAfterFiveAttempts() throws Exception {
        String email=email(),session=binding(),code=send(email,session);
        String wrong=code.equals("000000")?"000001":"000000";
        for(int i=0;i<5;i++) expectStatus(400,() -> verification.verify(email,wrong,session));
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM mall_email_verifications WHERE email=?",Integer.class,email)).isEqualTo(5);
        expectStatus(429,() -> verification.verify(email,code,session));
        expectStatus(400,() -> members.register(registration(email),session));
    }
    @Test void expiredCodeAndOtherBrowserCannotVerify() throws Exception {
        String email=email(),session=binding(),code=send(email,session);
        expectStatus(400,() -> verification.verify(email,code,binding()));
        jdbc.update("UPDATE mall_email_verifications SET expires_at=? WHERE email=?",Timestamp.from(Instant.now().minusSeconds(1)),email);
        expectStatus(400,() -> verification.verify(email,code,session));
    }
    @Test void verifiedProofCannotBeUsedForAnotherEmailOrSessionAndExpires() throws Exception {
        String email=email(),session=binding(),code=send(email,session);
        verification.verify(email,code,session);
        expectStatus(400,() -> members.register(registration(email),binding()));
        expectStatus(400,() -> members.register(registration(email()),session));
        jdbc.update("UPDATE mall_email_verifications SET verified_at=? WHERE email=?",Timestamp.from(Instant.now().minusSeconds(601)),email);
        expectStatus(400,() -> members.register(registration(email),session));
    }
    @Test void resendCooldownQuotaAndPreviousProofInvalidation() throws Exception {
        String email=email(),session=binding(),first=send(email,session);
        verification.verify(email,first,session);
        expectStatus(429,() -> verification.send(email,session,"other-source"));
        jdbc.update("UPDATE mall_email_verifications SET sent_at=? WHERE email=?",Timestamp.from(Instant.now().minusSeconds(61)),email);
        String latest=send(email,session);
        expectStatus(400,() -> members.register(registration(email),session));
        verification.verify(email,latest,session);
        jdbc.update("UPDATE mall_email_verifications SET sent_at=?,send_count=5 WHERE email=?",Timestamp.from(Instant.now().minusSeconds(61)),email);
        expectStatus(429,() -> verification.send(email,session,"another-source"));
    }
    @Test void ipQuotaIsStoredAndCannotBeBypassedByAnotherRecipient() throws Exception {
        String source=UUID.randomUUID().toString();
        String email=email(),session=binding();
        verification.send(email,session,source);TestSmtpServer.INSTANCE.codeFor(email);
        jdbc.update("UPDATE mall_email_send_limits SET send_count=20 WHERE source_hash=?",EmailVerificationService.fingerprint(source));
        expectStatus(429,() -> verification.send(email(),session,source));
    }
    @Test void failedSmtpDeliveryCannotLeaveUsableCode() {
        String email=email(),session=binding();
        TestSmtpServer.INSTANCE.failNext.set(true);
        expectStatus(503,() -> verification.send(email,session,UUID.randomUUID().toString()));
        assertThat(jdbc.queryForObject("SELECT code_hash FROM mall_email_verifications WHERE email=?",String.class,email)).isNull();
        expectStatus(400,() -> verification.verify(email,"123456",session));
        expectStatus(400,() -> members.register(registration(email),session));
    }
    @Test void failedMemberCreationDoesNotConsumeProof() throws Exception {
        String email=email(),session=binding(),code=send(email,session);
        verification.verify(email,code,session);
        expectStatus(400,() -> members.register(new MemberService.Registration(email,"가".repeat(30),"테스터"),session));
        assertThat(jdbc.queryForObject("SELECT consumed_at FROM mall_email_verifications WHERE email=?",Timestamp.class,email)).isNull();
        assertThat(members.register(registration(email),session).email()).isEqualTo(email);
    }
}
