package com.ais.marketbackend.categorias.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CategoriaTest {

    @Test
    void nuevaCategoriaEstaActivaPorDefecto() {
        Categoria categoria = Categoria.nueva("Bebidas", "http://x.com/bebidas.png");

        assertThat(categoria.estaActiva()).isTrue();
        assertThat(categoria.getEstado()).isEqualTo(EstadoCategoria.ACTIVA);
    }

    @Test
    void desactivarYActivarCambianElEstado() {
        Categoria categoria = Categoria.nueva("Bebidas", null);

        categoria.desactivar();
        assertThat(categoria.estaActiva()).isFalse();

        categoria.activar();
        assertThat(categoria.estaActiva()).isTrue();
    }

    @Test
    void actualizarDatosCambiaNombreEImagen() {
        Categoria categoria = Categoria.nueva("Bebidas", null);

        categoria.actualizarDatos("Bebidas y snacks", "http://x.com/nueva.png");

        assertThat(categoria.getNombre()).isEqualTo("Bebidas y snacks");
        assertThat(categoria.getImagen()).isEqualTo("http://x.com/nueva.png");
    }
}
