package com.uniesp.priceops.repository;

import com.uniesp.priceops.model.ItemImportadoPdf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemImportadoPdfRepository extends JpaRepository<ItemImportadoPdf, Long> {

    List<ItemImportadoPdf> findByImportacaoPdfIdOrderByLinhaTabelaAsc(Long importacaoPdfId);

    List<ItemImportadoPdf> findByImportacaoPdfIdAndStatusItemOrderByLinhaTabelaAsc(Long importacaoPdfId, String statusItem);

    long countByImportacaoPdfId(Long importacaoPdfId);

    long countByImportacaoPdfIdAndStatusItem(Long importacaoPdfId, String statusItem);

    void deleteByImportacaoPdfId(Long importacaoPdfId);
}
