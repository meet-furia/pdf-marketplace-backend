package com.meet.pdf_marketplace.repository;

import com.meet.pdf_marketplace.entity.PdfProductEntity;
import com.meet.pdf_marketplace.enums.PdfProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PdfProductRepository extends AbstractRepository<PdfProductEntity, UUID> {

    List<PdfProductEntity> findByStatus(PdfProductStatus status);

    Page<PdfProductEntity> findByStatus(PdfProductStatus status, Pageable pageable);

    List<PdfProductEntity> findBySellerId(UUID sellerId);

    Optional<PdfProductEntity> findByFileKey(String fileKey);
}
