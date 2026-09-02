package com.ais.marketbackend.productos.application.services.interfaces;

import com.ais.marketbackend.productos.application.dtos.ProductoResumen;
import com.ais.marketbackend.shared.domain.Pagina;
import java.util.List;

public interface ProductoService {

    ProductoResumen crear(
            String codigoInterno, String codigoBarras, String nombre, String descripcion, String descripcionCorta,
            Long categoriaId, Long marcaId, Long unidadMedidaId, String imagenUrl);

    ProductoResumen actualizar(
            Long id, String codigoBarras, String nombre, String descripcion, String descripcionCorta,
            Long categoriaId, Long marcaId, Long unidadMedidaId, String imagenUrl);

    ProductoResumen actualizarImagen(Long id, String imagenUrl);

    void activar(Long id);

    void desactivar(Long id);

    /** Sin paginar — uso interno. El endpoint público usa la variante paginada. */
    List<ProductoResumen> listar();

    Pagina<ProductoResumen> listar(int pagina, int tamano);
}
