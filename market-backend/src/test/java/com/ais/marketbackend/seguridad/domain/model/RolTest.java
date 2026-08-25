package com.ais.marketbackend.seguridad.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class RolTest {

    @Test
    void tienePermisoEsVerdaderoSoloParaCodigosAsignados() {
        Permiso ver = new Permiso(1L, "PRODUCTOS_VER", "Ver productos");
        Rol rol = new Rol(1L, "ENCARGADO_TIENDA", false, Set.of(ver));

        assertThat(rol.tienePermiso("PRODUCTOS_VER")).isTrue();
        assertThat(rol.tienePermiso("PRODUCTOS_ELIMINAR")).isFalse();
    }

    @Test
    void alcanceGlobalSeExponeTalCual() {
        Rol admin = new Rol(1L, "ADMIN", true, Set.of());

        assertThat(admin.isAlcanceGlobal()).isTrue();
    }
}
