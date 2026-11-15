package com.uniesp.priceops.repository;

import com.uniesp.priceops.model.HistoricoPreco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoricoPrecoRepository extends JpaRepository<HistoricoPreco, Long> {

    List<HistoricoPreco> findAllByOrderByRegistradoEmDescIdDesc();

    List<HistoricoPreco> findByProdutoIdOrderByRegistradoEmAscIdAsc(Long produtoId);

    List<HistoricoPreco> findByFornecedorIdOrderByRegistradoEmDescIdDesc(Long fornecedorId);

    List<HistoricoPreco> findByProdutoIdAndFornecedorIdOrderByRegistradoEmAscIdAsc(Long produtoId, Long fornecedorId);
}
