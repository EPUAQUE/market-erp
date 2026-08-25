package com.ais.marketbackend.productos.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProductoTest {

    @Test
    void nuevoProductoEstaActivoPorDefecto() {
        Producto producto = Producto.nuevo("P001", "7501234567890", "Leche", "Leche entera 1L", 1L, 2L, 3L, null);

        assertThat(producto.isActivo()).isTrue();
        assertThat(producto.getCodigoInterno()).isEqualTo("P001");
    }

    @Test
    void desactivarYActivarCambianElEstado() {
        Producto producto = Producto.nuevo("P001", null, "Leche", null, 1L, 2L, 3L, null);

        producto.desactivar();
        assertThat(producto.isActivo()).isFalse();

        producto.activar();
        assertThat(producto.isActivo()).isTrue();
    }

    @Test
    void actualizarDatosNoCambiaElCodigoInterno() {
        Producto producto = Producto.nuevo("P001", null, "Leche", null, 1L, 2L, 3L, null);

        producto.actualizarDatos("7501234567890", "Leche deslactosada", "Nueva descripción", 4L, 5L, 6L, "url.png");

        assertThat(producto.getCodigoInterno()).isEqualTo("P001");
        assertThat(producto.getCodigoBarras()).isEqualTo("7501234567890");
        assertThat(producto.getNombre()).isEqualTo("Leche deslactosada");
        assertThat(producto.getCategoriaId()).isEqualTo(4L);
        assertThat(producto.getMarcaId()).isEqualTo(5L);
        assertThat(producto.getUnidadMedidaId()).isEqualTo(6L);
    }
}
