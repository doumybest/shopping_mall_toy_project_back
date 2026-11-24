package com.example.shoppingmall.common;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {
    private static final Map<String,String> FIELDS = Map.ofEntries(
        Map.entry("email","メールアドレス"), Map.entry("password","パスワード"), Map.entry("name","お名前・商品名"),
        Map.entry("code","認証コード"), Map.entry("category","カテゴリー"), Map.entry("price","価格"),
        Map.entry("stock","在庫数"), Map.entry("quantity","数量"), Map.entry("phone","電話番号"),
        Map.entry("address","住所"), Map.entry("recipient","お届け先氏名"), Map.entry("pointsUsed","利用ポイント"),
        Map.entry("paymentMethod","決済方法"), Map.entry("content","レビュー"), Map.entry("rating","評価"),
        Map.entry("description","商品説明"), Map.entry("emoji","商品アイコン"), Map.entry("imageUrl","画像URL"));
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", ex.getReason() == null ? "リクエストを処理できません。" : ex.getReason()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> FIELDS.getOrDefault(error.getField(), "入力内容") + ": " + error.getDefaultMessage()).findFirst().orElse("入力内容をご確認ください。");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    ResponseEntity<?> badInput(Exception ex) { return ResponseEntity.badRequest().body(Map.of("message", "入力形式をご確認ください。")); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body(Map.of("message", "登録済みの情報か、入力条件を満たしていません。"));
    }
}
