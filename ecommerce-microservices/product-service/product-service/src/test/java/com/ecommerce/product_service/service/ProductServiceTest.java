package com.ecommerce.product_service.service;

import com.ecommerce.product_service.domain.entity.Product;
import com.ecommerce.product_service.domain.enums.Category;
import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.dto.ProductResponse;
import com.ecommerce.product_service.exception.DuplicateProductException;
import com.ecommerce.product_service.exception.ProductNotFoundException;
import com.ecommerce.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)  // activates Mockito — no Spring context needed
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;  // fake — no real DB calls

    @InjectMocks
    ProductService productService;        // real service with mocked repo injected

    Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product("Laptop", "A laptop", new BigDecimal("999.99"), 5, Category.ELECTRONICS);
    }

    @Test
    void findById_existingId_returnsResponse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        ProductResponse response = productService.findById(1L);

        assertThat(response.getName()).isEqualTo("Laptop");
        assertThat(response.getCategory()).isEqualTo(Category.ELECTRONICS);
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    void findById_missingId_throwsProductNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_newProduct_savesAndReturnsResponse() {
        ProductRequest request = buildRequest("New Laptop", Category.ELECTRONICS);
        when(productRepository.findByName("New Laptop")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductResponse response = productService.create(request);

        assertThat(response).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void create_duplicateName_throwsDuplicateProductException() {
        ProductRequest request = buildRequest("Laptop", Category.ELECTRONICS);
        when(productRepository.findByName("Laptop")).thenReturn(Optional.of(sampleProduct));

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(DuplicateProductException.class)
                .hasMessageContaining("Laptop");

        verify(productRepository, never()).save(any());  // save must NOT be called
    }

    @Test
    void delete_existingId_deletesProduct() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.delete(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    void delete_missingId_throwsProductNotFoundException() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).deleteById(any());
    }

    private ProductRequest buildRequest(String name, Category category) {
        ProductRequest req = new ProductRequest();
        req.setName(name);
        req.setDescription("desc");
        req.setPrice(new BigDecimal("100.00"));
        req.setStockQuantity(10);
        req.setCategory(category);
        return req;
    }
}
