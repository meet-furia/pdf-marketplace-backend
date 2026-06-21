package com.meet.pdf_marketplace.repository;

import com.meet.pdf_marketplace.entity.ProductEntity;
import com.meet.pdf_marketplace.enums.PdfProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends AbstractRepository<ProductEntity, UUID> {

    List<ProductEntity> findByStatus(PdfProductStatus status);

    Page<ProductEntity> findByStatus(PdfProductStatus status, Pageable pageable);

    List<ProductEntity> findBySellerId(UUID sellerId);

    Optional<ProductEntity> findByFileKey(String fileKey);
}


