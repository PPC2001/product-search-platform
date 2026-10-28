package com.example.product.search.service;


import java.util.List;

import com.example.product.search.document.ProductDocument;
import com.example.product.search.dto.ProductResponse;
import com.example.product.search.dto.ProductSearchResponse;
import com.example.product.search.exception.ProductSearchException;
import com.example.product.search.mapper.ProductMapper;
import com.example.product.search.repository.ProductSearchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductSearchRepository productSearchRepository;

    public List<ProductResponse> searchByProductNameFuzzy(String query) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("Search query must not be blank");
        }

        try {
            return productSearchRepository.searchByProductNameFuzzy(query.trim())
                    .stream()
                    .map(ProductMapper::toResponse)
                    .toList();

        } catch (Exception exception) {
            log.error("Fuzzy product search failed for query '{}'", query, exception);
            throw new ProductSearchException("Unable to search products by name", exception);
        }
    }

    public ProductSearchResponse search(String queryText, String category, Double minPrice,
                                        Double maxPrice, boolean activeOnly, String sortBy,
                                        String sortDirection, int page, int size) {

        validateSearchRequest(queryText, minPrice, maxPrice, sortBy, sortDirection, page, size);

        long startTime = System.nanoTime();
        try {
            Query query = NativeQuery.builder().withQuery(q -> q.bool(b -> {
                        b.must(m -> m.match(match -> match
                                .field("productName")
                                .query(queryText.trim())
                                .fuzziness("AUTO")));

                        if (StringUtils.hasText(category)) {
                            b.filter(f -> f.term(t -> t
                                    .field("category")
                                    .value(category.trim())));
                        }

                        if (minPrice != null || maxPrice != null) {
                            b.filter(f -> f.range(r -> {
                                r.number(n -> {
                                    n.field("price");

                                    if (minPrice != null) {
                                        n.gte(minPrice);
                                    }

                                    if (maxPrice != null) {
                                        n.lte(maxPrice);
                                    }

                                    return n;
                                });

                                return r;
                            }));
                        }

                        if (activeOnly) {
                            b.filter(f -> f.term(t -> t
                                    .field("isActive")
                                    .value(true)));
                        }

                        return b;
                    }))
                    .withPageable(PageRequest.of(page, size))
                    .build();

            if ("price".equals(sortBy)) {
                Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
                query.addSort(Sort.by(direction, "price"));
            }

            SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);

            List<ProductResponse> products = hits.getSearchHits()
                    .stream()
                    .map(SearchHit::getContent)
                    .map(ProductMapper::toResponse)
                    .toList();

            long totalElements = hits.getTotalHits();

            int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

            log.info("Product search completed: page={}, size={}, " + "totalHits={}, durationMs={}", page, size, totalElements,
                    (System.nanoTime() - startTime) / 1_000_000);

            return new ProductSearchResponse(products, totalElements, page, size, totalPages);

        } catch (Exception exception) {
            log.error("Product search failed", exception);
            throw new ProductSearchException("Unable to complete product search", exception);
        }
    }

    private void validateSearchRequest(
            String queryText,
            Double minPrice,
            Double maxPrice,
            String sortBy,
            String sortDirection,
            int page,
            int size) {

        if (!StringUtils.hasText(queryText) || queryText.length() > 100) {
            throw new IllegalArgumentException("Query must contain 1 to 100 characters");
        }

        if (minPrice != null && minPrice < 0 || maxPrice != null && maxPrice < 0) {
            throw new IllegalArgumentException("Prices cannot be negative");
        }

        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("minPrice cannot exceed maxPrice");
        }

        if (!"relevance".equalsIgnoreCase(sortBy) && !"price".equalsIgnoreCase(sortBy)) {
            throw new IllegalArgumentException("sortBy must be relevance or price");
        }

        if (!"asc".equalsIgnoreCase(sortDirection) && !"desc".equalsIgnoreCase(sortDirection)) {
            throw new IllegalArgumentException("sortDirection must be asc or desc");
        }

        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }
    }
}
