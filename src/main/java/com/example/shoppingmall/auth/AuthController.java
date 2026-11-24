package com.example.shoppingmall.auth;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final MemberService members;
    private final EmailVerificationService verification;
    public AuthController(MemberService members, EmailVerificationService verification) {
        this.members = members; this.verification = verification;
    }
    private String session(HttpServletRequest request) {
        return EmailVerificationService.fingerprint(request.getSession().getId());
    }
    @GetMapping("/csrf") Map<String,String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }
    @PostMapping("/email/send") EmailVerificationService.SendResult send(
            @Valid @RequestBody EmailVerificationService.EmailRequest body, HttpServletRequest request) {
        return verification.send(body.email(), session(request), request.getRemoteAddr());
    }
    @PostMapping("/email/verify") EmailVerificationService.VerifyResult verify(
            @Valid @RequestBody EmailVerificationService.VerifyRequest body, HttpServletRequest request) {
        return verification.verify(body.email(), body.code(), session(request));
    }
    @PostMapping("/register") MemberService.Member register(@Valid @RequestBody MemberService.Registration body,
            HttpServletRequest request) { return members.register(body, session(request)); }
    @GetMapping("/me") MemberService.Member me(Authentication auth) { return members.current(auth); }
}
