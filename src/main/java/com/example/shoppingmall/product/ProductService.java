package com.example.shoppingmall.product;

import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly=true)
public class ProductService {
    public record Edit(@NotBlank @Size(max=120) String name,@NotBlank @Size(max=40) String category,
        @Min(1) @Max(100000000) long price,@NotBlank @Size(max=2000) String description,
        @NotBlank @Size(max=20) String emoji,@Size(max=1000) @Pattern(regexp="^$|https://[^\\s]+",message="HTTPSの画像URLのみ使用できます。") String imageUrl,
        @Min(0) @Max(1000000) int stock) {}

    private final ProductRepository repository;
    public ProductService(ProductRepository repository) { this.repository=repository; }
    public List<Product> findAll() { return repository.findAllByActiveOrderByIdAsc(1).stream().map(ProductService::view).toList(); }
    public Product findById(long id) {
        return repository.findByIdAndActive(id,1).map(ProductService::view)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"商品が見つかりません。"));
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public ProductEntity lockEntity(long id) {
        return repository.findLockedById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"商品が見つかりません。"));
    }
    @Transactional
    public Product create(Edit body) {
        ProductEntity product=new ProductEntity();
        apply(product,body);
        product.setActive(1);
        return view(repository.saveAndFlush(product));
    }
    @Transactional
    public Product update(long id,Edit body) {
        ProductEntity product=lockEntity(id);
        if(product.getActive()!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"販売終了の商品です。");
        apply(product,body); // Managed entity changes are persisted by dirty checking on commit.
        return view(product);
    }
    @Transactional
    public void delete(long id) { lockEntity(id).setActive(0); }
    private static void apply(ProductEntity product,Edit body) {
        product.setName(body.name().trim());
        product.setCategory(body.category().trim());
        product.setPrice(body.price());
        product.setDescription(body.description().trim());
        product.setEmoji(body.emoji());
        product.setImageUrl(body.imageUrl());
        product.setStock(body.stock());
    }
    public static Product view(ProductEntity product) {
        return new Product(product.getId(),product.getName(),product.getCategory(),product.getPrice(),
            product.getDescription(),product.getEmoji(),product.getImageUrl(),product.getStock(),product.getActive());
    }
}
