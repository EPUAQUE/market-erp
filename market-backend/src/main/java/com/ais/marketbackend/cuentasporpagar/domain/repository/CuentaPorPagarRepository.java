package com.ais.marketbackend.cuentasporpagar.domain.repository;

import com.ais.marketbackend.cuentasporpagar.domain.model.CuentaPorPagar;
import java.util.List;
import java.util.Optional;

public interface CuentaPorPagarRepository {

    CuentaPorPagar save(CuentaPorPagar cuenta);

    Optional<CuentaPorPagar> findById(Long id);

    List<CuentaPorPagar> findByTiendaId(Long tiendaId);
}
