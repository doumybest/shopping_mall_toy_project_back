package com.example.shoppingmall.review;

import com.example.shoppingmall.auth.MemberService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReviewController {
    private final ReviewService reviews; private final MemberService members;
    public ReviewController(ReviewService reviews,MemberService members) { this.reviews=reviews;this.members=members; }
    @GetMapping("/api/products/{productId}/reviews") List<ReviewService.Review> list(@PathVariable long productId) { return reviews.list(productId); }
    @PostMapping("/api/products/{productId}/reviews") void write(Authentication auth,@PathVariable long productId,@Valid @RequestBody ReviewService.Write body) { reviews.write(members.current(auth).id(),productId,body); }
    @DeleteMapping("/api/reviews/{reviewId}") void delete(Authentication auth,@PathVariable long reviewId) { reviews.delete(members.current(auth).id(),reviewId); }
}
