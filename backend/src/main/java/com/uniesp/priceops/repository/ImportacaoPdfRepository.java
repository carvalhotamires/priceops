package com.uniesp.priceops.repository;

import com.uniesp.priceops.model.ImportacaoPdf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImportacaoPdfRepository extends JpaRepository<ImportacaoPdf, Long> {

    Optional<ImportacaoPdf> findByFornecedorIdAndHashSha256(Long fornecedorId, String hashSha256);

    List<ImportacaoPdf> findAllByOrderByImportadoEmDesc();

    List<ImportacaoPdf> findByFornecedorIdOrderByImportadoEmDesc(Long fornecedorId);

    List<ImportacaoPdf> findByStatusImportacaoIgnoreCaseOrderByImportadoEmDesc(String statusImportacao);

    List<ImportacaoPdf> findByNomeOriginalContainingIgnoreCaseOrderByImportadoEmDesc(String nomeOriginal);
}
