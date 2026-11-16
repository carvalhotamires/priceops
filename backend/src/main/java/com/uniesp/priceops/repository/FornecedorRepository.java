package com.uniesp.priceops.repository;

import com.uniesp.priceops.model.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {
    Optional<Fornecedor> findByCnpj(String cnpj);
    List<Fornecedor> findByNomeFantasiaContainingIgnoreCaseOrRazaoSocialContainingIgnoreCase(String nomeFantasia, String razaoSocial);
    List<Fornecedor> findByStatusIgnoreCase(String status);
}
