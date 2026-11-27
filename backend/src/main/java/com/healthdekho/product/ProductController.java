package com.healthdekho.product;

import com.healthdekho.product.dto.AvailableProductResponse;
import com.healthdekho.security.CurrentCustomer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final CurrentCustomer currentCustomer;

    public ProductController(ProductService productService, CurrentCustomer currentCustomer) {
        this.productService = productService;
        this.currentCustomer = currentCustomer;
    }

    /** FR-1 C: available insurance policies for the logged-in customer. */
    @GetMapping("/available")
    public List<AvailableProductResponse> available() {
        return productService.availableProducts(currentCustomer.id()).stream()
                .map(AvailableProductResponse::from)
                .toList();
    }
}
