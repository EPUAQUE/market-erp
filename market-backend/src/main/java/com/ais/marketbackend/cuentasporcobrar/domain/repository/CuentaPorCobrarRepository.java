package com.ais.marketbackend.cuentasporcobrar.domain.repository;

import com.ais.marketbackend.cuentasporcobrar.domain.model.CuentaPorCobrar;
import com.ais.marketbackend.shared.domain.Pagina;
import java.util.List;
import java.util.Optional;

public interface CuentaPorCobrarRepository {

    CuentaPorCobrar save(CuentaPorCobrar cuenta);

    Optional<CuentaPorCobrar> findById(Long id);

    /** Sin paginar — uso interno (ej. agregados del dashboard). */
    List<CuentaPorCobrar> findByTiendaId(Long tiendaId);

    Pagina<CuentaPorCobrar> findByTiendaId(Long tiendaId, int pagina, int tamano);
}
