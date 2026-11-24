package com.example.shoppingmall.product;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceTest {
    @Test void missingProductReturns404() {
        var repository=mock(ProductRepository.class);
        when(repository.findByIdAndActive(999L,1)).thenReturn(java.util.Optional.empty());
        var service=new ProductService(repository);
        assertThatThrownBy(() -> service.findById(999L)).isInstanceOfSatisfying(ResponseStatusException.class,
            ex -> assertThat(ex.getStatusCode().value()).isEqualTo(404));
    }
}
