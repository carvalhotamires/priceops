package com.uniesp.priceops.repository;

import com.uniesp.priceops.model.VinculoItemFornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VinculoItemFornecedorRepository extends JpaRepository<VinculoItemFornecedor, Long> {

    Optional<VinculoItemFornecedor> findByFornecedorIdAndCodigoFornecedorIgnoreCase(Long fornecedorId, String codigoFornecedor);

    List<VinculoItemFornecedor> findByFornecedorId(Long fornecedorId);

    List<VinculoItemFornecedor> findByFornecedorIdAndAtivoTrue(Long fornecedorId);

    List<VinculoItemFornecedor> findByProdutoId(Long produtoId);

    boolean existsByFornecedorIdAndCodigoFornecedorIgnoreCase(Long fornecedorId, String codigoFornecedor);
}
