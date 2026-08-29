package com.ais.marketbackend.cuentasporpagar.domain.repository;

import com.ais.marketbackend.cuentasporpagar.domain.model.CuentaPorPagar;
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

    List<CuentaPorPagar> findByTiendaId(Long tiendaId);
}
