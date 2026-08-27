package com.ais.marketbackend.seguridad.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class PermisosEfectivosTest {

    @Test
    void tienePermisoRevisaElConjunto() {
        PermisosEfectivos permisos = new PermisosEfectivos(1L, "ana", Set.of("VENTAS_CREAR"), Set.of(1L), false);

        assertThat(permisos.tienePermiso("VENTAS_CREAR")).isTrue();
        assertThat(permisos.tienePermiso("CAJA_CERRAR")).isFalse();
    }

    @Test
    void puedeAccederATiendaSoloSiEstaAsignadaOAlcanceGlobal() {
        PermisosEfectivos sinAlcanceGlobal = new PermisosEfectivos(1L, "ana", Set.of(), Set.of(5L), false);
        PermisosEfectivos conAlcanceGlobal = new PermisosEfectivos(2L, "admin", Set.of(), Set.of(), true);

        assertThat(sinAlcanceGlobal.puedeAccederATienda(5L)).isTrue();
        assertThat(sinAlcanceGlobal.puedeAccederATienda(9L)).isFalse();
        assertThat(conAlcanceGlobal.puedeAccederATienda(999L)).isTrue();
    }

    @Test
    void elConstructorDeCincoArgumentosNoAsignaNingunGrupo() {
        PermisosEfectivos permisos = new PermisosEfectivos(1L, "ana", Set.of(), Set.of(1L), false);

        assertThat(permisos.grupoIds()).isEmpty();
    }

    @Test
    void puedeAccederAGrupoSoloSiEstaAsignadoOAlcanceGlobal() {
        PermisosEfectivos sinAlcanceGlobal = new PermisosEfectivos(1L, "ana", Set.of(), Set.of(), false, Set.of(5L));
        PermisosEfectivos conAlcanceGlobal = new PermisosEfectivos(2L, "admin", Set.of(), Set.of(), true, Set.of());

        assertThat(sinAlcanceGlobal.puedeAccederAGrupo(5L)).isTrue();
        assertThat(sinAlcanceGlobal.puedeAccederAGrupo(9L)).isFalse();
        assertThat(conAlcanceGlobal.puedeAccederAGrupo(999L)).isTrue();
    }
}
