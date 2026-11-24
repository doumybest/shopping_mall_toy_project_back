package com.example.shoppingmall.product;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProductController {
    private final ProductService service;
    public ProductController(ProductService service) { this.service = service; }
    @GetMapping("/api/products") List<Product> products() { return service.findAll(); }
    @GetMapping("/api/products/{id}") Product detail(@PathVariable long id) { return service.findById(id); }
    @PostMapping("/api/admin/products") Product create(@Valid @RequestBody ProductService.Edit body) { return service.create(body); }
    @PutMapping("/api/admin/products/{id}") Product update(@PathVariable long id, @Valid @RequestBody ProductService.Edit body) { return service.update(id,body); }
    @DeleteMapping("/api/admin/products/{id}") void delete(@PathVariable long id) { service.delete(id); }
}
