package com.example.coupangapiserver.product.repository;

import com.example.coupangapiserver.product.domain.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductDocumentRepository extends ElasticsearchRepository<ProductDocument,String> {
}
