package com.example.product.search.repository;

import java.util.List;

import com.example.product.search.document.ProductDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;


public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {

    @Query("""
            {
              "match": {
                "productName": {
                  "query": "?0",
                  "fuzziness": "AUTO"
                }
              }
            }
            """)
    List<ProductDocument> searchByProductNameFuzzy(String query);
}
