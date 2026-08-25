package com.ais.marketbackend.productos.application.services.interfaces;

import com.ais.marketbackend.productos.application.dtos.ProductoTiendaResumen;
import com.ais.marketbackend.shared.domain.Pagina;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductoTiendaService {

    /** Usado por otros módulos (p. ej. Inventario) para validar permitirIngreso/permitirVenta antes de mutar. */
    Optional<ProductoTiendaResumen> obtener(Long productoId, Long tiendaId);

    ProductoTiendaResumen asignar(
            Long productoId, Long tiendaId, BigDecimal precioVenta, BigDecimal stockMinimo, BigDecimal stockMaximo,
            boolean permitirVenta, boolean permitirIngreso);

    ProductoTiendaResumen actualizar(
            Long id, BigDecimal precioVenta, BigDecimal stockMinimo, BigDecimal stockMaximo, boolean permitirVenta,
            boolean permitirIngreso);

    void activar(Long id);

    void desactivar(Long id);

    List<ProductoTiendaResumen> listarPorProducto(Long productoId);

    /** Sin paginar — uso interno (ej. agregados del dashboard). El endpoint público usa la variante paginada. */
    List<ProductoTiendaResumen> listarPorTienda(Long tiendaId);

    Pagina<ProductoTiendaResumen> listarPorTienda(Long tiendaId, int pagina, int tamano);
}
