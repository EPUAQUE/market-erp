package com.ais.marketbackend.cuentasporpagar.domain.repository;

import com.ais.marketbackend.cuentasporpagar.domain.model.CuentaPorPagar;
import com.ais.marketbackend.shared.domain.Pagina;
import java.util.List;
import java.util.Optional;

public interface CuentaPorPagarRepository {

    CuentaPorPagar save(CuentaPorPagar cuenta);

    Optional<CuentaPorPagar> findById(Long id);

    /**
     * Igual que {@link #findById}, pero bloquea la fila con
     * {@code PESSIMISTIC_WRITE} dentro de la transacción actual — usado por
     * {@code CuentaPorPagarServiceImpl.registrarPago}/{@code anular} para
     * serializar mutaciones concurrentes sobre la misma cuenta (mismo motivo
     * que {@code CuentaPorCobrarRepository.findByIdConBloqueo}).
     */
    Optional<CuentaPorPagar> findByIdConBloqueo(Long id);

    /** Sin paginar — uso interno (dashboard, notificaciones de vencimiento). */
    List<CuentaPorPagar> findByTiendaId(Long tiendaId);

    /** Fase 11 (PLAN_MEJORAS.md): crece 1:1 con cada compra recibida, igual que CxC — el endpoint público usa esta. */
    Pagina<CuentaPorPagar> findByTiendaId(Long tiendaId, int pagina, int tamano);
}
