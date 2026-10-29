package com.uniesp.priceops.repository;
import com.uniesp.priceops.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    Optional<Produto> findByCodigo(String codigo);
    List<Produto> findByNomeContainingIgnoreCase(String nome);
    List<Produto> findByStatus(String status);
}
