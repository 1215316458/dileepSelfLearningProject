package com.ecommerce.domain.streams.transformer;

import com.ecommerce.domain.dto.ProductDTO;
import com.ecommerce.domain.model.Product;

import java.util.function.Function;

public class ProductTransformer {

    // Only id, name, price — lightweight for list views
    public static final Function<Product, ProductDTO> toSummary =
            p -> new ProductDTO(p.getIdentity(), p.getName(), p.getPrice(), null, 0, null);

    // All fields — used for product detail page
    public static final Function<Product, ProductDTO> toDetailed =
            p -> new ProductDTO(p.getIdentity(), p.getName(), p.getPrice(), p.getDescription(), p.getStock(), p.getCategory());

    // name, price, category — used for catalog/search results
    public static final Function<Product, ProductDTO> toCatalogEntry =
            p -> new ProductDTO(null, p.getName(), p.getPrice(), null, 0, p.getCategory());
}
