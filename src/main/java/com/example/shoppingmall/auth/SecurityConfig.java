package com.example.shoppingmall.auth;

import java.util.Locale;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean UserDetailsService userDetailsService(MemberRepository members) {
        return email -> members.findByEmail(email.trim().toLowerCase(Locale.ROOT))
            .map(member -> User.withUsername(member.getEmail()).password(member.getPasswordHash()).roles(member.getRole()).build())
            .orElseThrow(() -> new UsernameNotFoundException("アカウント情報が一致しません。"));
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Keep session-backed CSRF protection; the SPA fetches a masked token from /api/auth/csrf.
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/error", "/api/auth/csrf", "/api/auth/register", "/api/auth/login",
                "/api/auth/email/send", "/api/auth/email/verify").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/*", "/api/products/*/reviews", "/api/events", "/api/events/{id:[0-9]+}", "/api/events/{id:[0-9]+}/image", "/api/health").permitAll()
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated())
            .formLogin(form -> form.loginProcessingUrl("/api/auth/login").usernameParameter("email")
                .successHandler((req,res,auth) -> { res.setContentType("application/json;charset=UTF-8"); res.getWriter().write("{\"ok\":true}"); })
                .failureHandler((req,res,ex) -> { res.setStatus(401); res.setContentType("application/json;charset=UTF-8"); res.getWriter().write("{\"message\":\"メールアドレスまたはパスワードをご確認ください。\"}"); }))
            .logout(logout -> logout.logoutUrl("/api/auth/logout").invalidateHttpSession(true).deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req,res,auth) -> res.setStatus(204)))
            .requestCache(cache -> cache.disable())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req,res,ex) -> { res.setStatus(401); res.setContentType("application/json;charset=UTF-8"); res.getWriter().write("{\"message\":\"ログインが必要です。\"}"); })
                .accessDeniedHandler((req,res,ex) -> { res.setStatus(403); res.setContentType("application/json;charset=UTF-8"); res.getWriter().write("{\"message\":\"権限がないか、セッションの有効期限が切れています。画面を更新して再度お試しください。\"}"); }));
        return http.build();
    }
}
