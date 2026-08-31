package com.ais.marketbackend.auditoria.domain.repository;

import com.ais.marketbackend.auditoria.domain.model.AuditEvent;
import com.ais.marketbackend.shared.domain.Pagina;

public interface AuditEventRepository {

    AuditEvent save(AuditEvent evento);

    /** Orden más reciente primero — no todo evento tiene tienda (ej. login fallido). */
    Pagina<AuditEvent> listarTodo(int pagina, int tamano);

    Pagina<AuditEvent> listarPorTienda(Long tiendaId, int pagina, int tamano);
}
