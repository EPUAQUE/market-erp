package com.ais.marketbackend.caja.domain.repository;

import com.ais.marketbackend.caja.domain.model.CajaSesion;
import com.ais.marketbackend.shared.domain.Pagina;
import java.util.List;
import java.util.Optional;

public interface CajaSesionRepository {

    CajaSesion save(CajaSesion sesion);

    Optional<CajaSesion> findById(Long id);

    Optional<CajaSesion> findAbiertaByTiendaId(Long tiendaId);

    /** Sin paginar — uso interno (ej. agregados del dashboard). */
    List<CajaSesion> findByTiendaId(Long tiendaId);

    Pagina<CajaSesion> findByTiendaId(Long tiendaId, int pagina, int tamano);
}
