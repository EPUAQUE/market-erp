package com.ais.marketbackend.compras.domain.repository;

import com.ais.marketbackend.compras.domain.model.Compra;
import java.util.List;
import java.util.Optional;

public interface CompraRepository {

    Compra save(Compra compra);

    Optional<Compra> findById(Long id);

    List<Compra> findByTiendaId(Long tiendaId);
}
