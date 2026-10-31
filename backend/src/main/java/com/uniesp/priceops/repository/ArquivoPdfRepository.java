package com.uniesp.priceops.repository;

import com.uniesp.priceops.model.ArquivoPdf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArquivoPdfRepository extends JpaRepository<ArquivoPdf, Long> {

    Optional<ArquivoPdf> findByHashSha256(String hashSha256);

    List<ArquivoPdf> findAllByOrderByEnviadoEmDesc();

    List<ArquivoPdf> findByFornecedorIdOrderByEnviadoEmDesc(Long fornecedorId);

    List<ArquivoPdf> findByStatusProcessamentoIgnoreCaseOrderByEnviadoEmDesc(String statusProcessamento);

    List<ArquivoPdf> findByNomeOriginalContainingIgnoreCaseOrderByEnviadoEmDesc(String nomeOriginal);
}
