package com.ais.marketbackend.auditoria.application.services.interfaces;

import com.ais.marketbackend.auditoria.application.dtos.AuditEventResumen;
import com.ais.marketbackend.shared.domain.Pagina;

public interface AuditoriaService {

    Pagina<AuditEventResumen> listarTodo(int pagina, int tamano);

    Pagina<AuditEventResumen> listarPorTienda(Long tiendaId, int pagina, int tamano);
}
