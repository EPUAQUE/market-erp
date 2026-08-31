package com.ais.marketbackend.auditoria.infrastructure.persistence.repositories;

import com.ais.marketbackend.auditoria.infrastructure.persistence.entities.AuditEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventJpaRepository extends JpaRepository<AuditEventEntity, Long> {

    Page<AuditEventEntity> findByTiendaId(Long tiendaId, Pageable pageable);
}
