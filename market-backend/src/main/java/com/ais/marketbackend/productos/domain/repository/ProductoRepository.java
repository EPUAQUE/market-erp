package com.ais.marketbackend.productos.domain.repository;

import com.ais.marketbackend.productos.domain.model.Producto;
import com.ais.marketbackend.shared.domain.Pagina;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository {

    Producto save(Producto producto);

    Optional<Producto> findById(Long id);

    boolean existsByCodigoInterno(String codigoInterno);

    /** Sin paginar — uso interno. El endpoint público usa la variante paginada. */
    List<Producto> findAll();

    Pagina<Producto> findAll(int pagina, int tamano);
}
