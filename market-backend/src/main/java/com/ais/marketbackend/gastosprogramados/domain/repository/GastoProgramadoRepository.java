package com.ais.marketbackend.gastosprogramados.domain.repository;

import com.ais.marketbackend.gastosprogramados.domain.model.GastoProgramado;
import java.util.List;
import java.util.Optional;

public interface GastoProgramadoRepository {

    GastoProgramado save(GastoProgramado gasto);

    Optional<GastoProgramado> findById(Long id);

    List<GastoProgramado> findByTiendaId(Long tiendaId);
}
