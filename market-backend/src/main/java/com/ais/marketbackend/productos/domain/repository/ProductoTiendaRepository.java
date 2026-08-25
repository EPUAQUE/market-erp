package com.ais.marketbackend.productos.domain.repository;

import com.ais.marketbackend.productos.domain.model.ProductoTienda;
import com.ais.marketbackend.shared.domain.Pagina;
import java.util.List;
import java.util.Optional;

public interface ProductoTiendaRepository {

    ProductoTienda save(ProductoTienda productoTienda);

    Optional<ProductoTienda> findById(Long id);

    Optional<ProductoTienda> findByProductoIdAndTiendaId(Long productoId, Long tiendaId);

    List<ProductoTienda> findByProductoId(Long productoId);

    /** Sin paginar — uso interno (ej. agregados del dashboard). */
    List<ProductoTienda> findByTiendaId(Long tiendaId);

    Pagina<ProductoTienda> findByTiendaId(Long tiendaId, int pagina, int tamano);
}
