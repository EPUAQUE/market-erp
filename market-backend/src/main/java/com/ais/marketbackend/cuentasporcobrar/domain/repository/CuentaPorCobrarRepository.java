package com.ais.marketbackend.cuentasporcobrar.domain.repository;

import com.ais.marketbackend.cuentasporcobrar.domain.model.CuentaPorCobrar;
import com.ais.marketbackend.shared.domain.Pagina;
import java.util.List;
import java.util.Optional;

public interface CuentaPorCobrarRepository {

    CuentaPorCobrar save(CuentaPorCobrar cuenta);

    Optional<CuentaPorCobrar> findById(Long id);

    /**
     * Igual que {@link #findById}, pero bloquea la fila con
     * {@code PESSIMISTIC_WRITE} dentro de la transacción actual — usado por
     * {@code CuentaPorCobrarServiceImpl.registrarCobro}/{@code anular} para
     * serializar mutaciones concurrentes sobre la misma cuenta. Sin esto, dos
     * cobros casi simultáneos podían leer el mismo saldo pendiente y juntas
     * superarlo aunque cada uno, evaluado solo, no lo hiciera — y la colección
     * JPA de cobros (con {@code orphanRemoval}) podía perder uno de los dos en
     * un merge concurrente sin lock.
     */
    Optional<CuentaPorCobrar> findByIdConBloqueo(Long id);

    /** Sin paginar — uso interno (ej. agregados del dashboard). */
    List<CuentaPorCobrar> findByTiendaId(Long tiendaId);

    Pagina<CuentaPorCobrar> findByTiendaId(Long tiendaId, int pagina, int tamano);
}
