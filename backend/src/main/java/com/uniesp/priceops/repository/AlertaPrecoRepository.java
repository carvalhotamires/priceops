package com.uniesp.priceops.repository;

import com.uniesp.priceops.model.AlertaPreco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaPrecoRepository extends JpaRepository<AlertaPreco, Long> {

    List<AlertaPreco> findAllByOrderByCriadoEmDesc();

    List<AlertaPreco> findByStatusNotificacaoIgnoreCaseOrderByCriadoEmDesc(String statusNotificacao);

    List<AlertaPreco> findBySeveridadeIgnoreCaseOrderByCriadoEmDesc(String severidade);

    List<AlertaPreco> findByTipoAlertaIgnoreCaseOrderByCriadoEmDesc(String tipoAlerta);

    List<AlertaPreco> findByProdutoIdOrderByCriadoEmDesc(Long produtoId);

    List<AlertaPreco> findByFornecedorIdOrderByCriadoEmDesc(Long fornecedorId);

    long countByStatusNotificacaoIgnoreCase(String statusNotificacao);

    long countByStatusNotificacaoIgnoreCaseAndSeveridadeIgnoreCase(String statusNotificacao, String severidade);
}
