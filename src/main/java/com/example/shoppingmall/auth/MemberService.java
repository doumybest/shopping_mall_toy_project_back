package com.example.shoppingmall.auth;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly=true)
public class MemberService {
    public record Registration(@NotBlank @Email @Size(max=200) String email,
        @NotBlank @Size(min=10,max=64) String password,@NotBlank @Size(max=50) String name) {}
    public record Member(long id,String email,String name,String role,long points) {}

    private final MemberRepository repository;
    private final PasswordEncoder encoder;
    private final EmailVerificationService verification;

    public MemberService(MemberRepository repository,PasswordEncoder encoder,EmailVerificationService verification) {
        this.repository=repository;
        this.encoder=encoder;
        this.verification=verification;
    }

    @Transactional
    public Member register(Registration request,String sessionHash) {
        verification.consume(request.email(),sessionHash);
        return view(createEntity(request,"MEMBER",true));
    }

    // Trusted bootstrap/test entry point; public registration always uses MEMBER and email proof.
    @Transactional
    public Member create(Registration request,String role) { return view(createEntity(request,role,false)); }

    private MemberEntity createEntity(Registration request,String role,boolean verified) {
        if(request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"パスワードはUTF-8で72バイト以内にしてください。");
        if(!java.util.Set.of("MEMBER","ADMIN").contains(role)) throw new IllegalArgumentException("Unknown role");
        String email=request.email().trim().toLowerCase(Locale.ROOT);
        if(repository.existsByEmail(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"登録済みのメールアドレスです。");
        MemberEntity member=new MemberEntity();
        member.setEmail(email);
        member.setPasswordHash(encoder.encode(request.password()));
        member.setName(request.name().trim());
        member.setRole(role);
        member.setPoints(0);
        if(verified) member.setEmailVerifiedAt(LocalDateTime.now());
        return repository.saveAndFlush(member);
    }

    public Member current(Authentication auth) {
        if(auth==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"ログインが必要です。");
        return repository.findByEmail(auth.getName()).map(MemberService::view)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,"ログインが必要です。"));
    }
    public Member byId(long id) {
        return repository.findById(id).map(MemberService::view)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"会員が見つかりません。"));
    }

    @Transactional(propagation=Propagation.MANDATORY)
    public MemberEntity lockEntity(long id) {
        return repository.findLockedById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"会員が見つかりません。"));
    }
    private static Member view(MemberEntity member) {
        return new Member(member.getId(),member.getEmail(),member.getName(),member.getRole(),member.getPoints());
    }
}
