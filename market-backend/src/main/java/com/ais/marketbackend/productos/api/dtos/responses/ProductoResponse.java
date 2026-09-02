package com.ais.marketbackend.productos.api.dtos.responses;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProductoResponse {

    Long id;
    String codigoInterno;
    String codigoBarras;
    String nombre;
    String descripcion;
    String descripcionCorta;
    Long categoriaId;
    Long marcaId;
    Long unidadMedidaId;
    String imagenUrl;
    boolean activo;
}
