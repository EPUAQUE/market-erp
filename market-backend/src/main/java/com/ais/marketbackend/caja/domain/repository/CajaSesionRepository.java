package com.ais.marketbackend.caja.domain.repository;

import com.ais.marketbackend.caja.domain.model.CajaSesion;
import com.ais.marketbackend.shared.domain.Pagina;
import java.util.List;
import java.util.Optional;

public interface CajaSesionRepository {

    CajaSesion save(CajaSesion sesion);

    Optional<CajaSesion> findById(Long id);

    Optional<CajaSesion> findAbiertaByTiendaId(Long tiendaId);

    /**
     * Igual que {@link #findAbiertaByTiendaId}, pero bloquea la fila con
     * {@code PESSIMISTIC_WRITE} dentro de la transacción actual — usado para
     * serializar registrar movimientos y cerrar entre solicitudes concurrentes
     * sobre la misma sesión (ver {@code CajaServiceImpl}). Sin esto, dos
     * movimientos concurrentes pueden perderse entre sí (colección JPA con
     * {@code orphanRemoval}) y dos cierres concurrentes pueden pisarse el
     * monto contado sin ningún error.
     */
    Optional<CajaSesion> findAbiertaByTiendaIdConBloqueo(Long tiendaId);

    Optional<CajaSesion> findByTiendaIdAndCorrelationIdApertura(Long tiendaId, String correlationIdApertura);

    Optional<CajaSesion> findByTiendaIdAndCorrelationIdCierre(Long tiendaId, String correlationIdCierre);

    /** Sin paginar — uso interno (ej. agregados del dashboard). */
    List<CajaSesion> findByTiendaId(Long tiendaId);

    Pagina<CajaSesion> findByTiendaId(Long tiendaId, int pagina, int tamano);
}
