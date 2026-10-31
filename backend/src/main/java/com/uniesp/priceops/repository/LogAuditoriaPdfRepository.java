package com.uniesp.priceops.repository;

import com.uniesp.priceops.model.LogAuditoriaPdf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogAuditoriaPdfRepository extends JpaRepository<LogAuditoriaPdf, Long> {

    List<LogAuditoriaPdf> findAllByOrderByDataHoraDesc();

    List<LogAuditoriaPdf> findByEventoIgnoreCaseOrderByDataHoraDesc(String evento);

    List<LogAuditoriaPdf> findByHashSha256OrderByDataHoraDesc(String hashSha256);
}
