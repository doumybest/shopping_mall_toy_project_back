package com.example.shoppingmall.review;

import com.example.shoppingmall.auth.MemberService;
import com.example.shoppingmall.order.OrderItemRepository;
import com.example.shoppingmall.product.*;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly=true)
public class ReviewService {
    public record Write(@Min(1) @Max(5) int rating,@NotBlank @Size(max=1000) String content) {}
    public record Review(long id,long memberId,String author,int rating,String content,String createdAt) {}
    private final ReviewRepository repository;
    private final OrderItemRepository orderItems;
    private final ProductRepository productRepository;
    private final MemberService members;
    private final ProductService products;

    public ReviewService(ReviewRepository repository,OrderItemRepository orderItems,ProductRepository productRepository,MemberService members,ProductService products) {
        this.repository=repository;this.orderItems=orderItems;this.productRepository=productRepository;
        this.members=members;this.products=products;
    }
    public List<Review> list(long productId) {
        products.findById(productId);
        return repository.findAllByProduct_IdOrderByIdDesc(productId).stream().map(review ->
            new Review(review.getId(),review.getMember().getId(),review.getMember().getName(),review.getRating(),review.getContent(),review.getCreatedAt().toString())).toList();
    }
    @Transactional
    public void write(long memberId,long productId,Write body) {
        var member=members.lockEntity(memberId);
        products.findById(productId);
        if(!orderItems.existsByOrder_Member_IdAndProduct_IdAndOrder_Status(memberId,productId,"PAID"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"購入済みの商品のみレビューを投稿できます。");
        ReviewEntity review=new ReviewEntity();
        review.setMember(member);review.setProduct(productRepository.getReferenceById(productId));
        review.setRating(body.rating());review.setContent(body.content().trim());
        repository.saveAndFlush(review);
    }
    @Transactional
    public void delete(long memberId,long reviewId) {
        members.lockEntity(memberId);
        ReviewEntity review=repository.findByIdAndMember_Id(reviewId,memberId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"ご自身のレビューのみ削除できます。"));
        repository.delete(review);
    }
}
