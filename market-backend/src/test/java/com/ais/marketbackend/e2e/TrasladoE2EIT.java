package com.ais.marketbackend.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Fase 5 (PLAN_MEJORAS.md), "Cobertura prioritaria" — flujo E2E: crear un
 * traslado entre dos tiendas → completarlo → verificar que el inventario de
 * origen bajó y el de destino subió en la misma cantidad.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class TrasladoE2EIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private ApoyoE2E apoyo;

    @BeforeEach
    void setUp() {
        apoyo = new ApoyoE2E(mockMvc, objectMapper);
    }

    @Test
    void completarTrasladoBajaOrigenYSubeDestinoEnLaMismaCantidad() throws Exception {
        String sufijo = String.valueOf(System.nanoTime() % 100_000_000L);
        String token = apoyo.login("admin", "Admin1234!Seguro");

        Long grupoId = apoyo.crearGrupoTienda(token, "G-TR" + sufijo, "Grupo traslado");
        Long tiendaOrigenId = apoyo.crearTienda(token, "T-TRO" + sufijo, "Tienda origen traslado", grupoId);
        Long tiendaDestinoId = apoyo.crearTienda(token, "T-TRD" + sufijo, "Tienda destino traslado", grupoId);
        Long categoriaId = apoyo.crearCategoria(token, "Categoria TR " + sufijo);
        Long marcaId = apoyo.crearMarca(token, "Marca TR " + sufijo);
        Long unidadId = apoyo.crearUnidadMedida(token, "Unidad TR " + sufijo, "u");
        Long productoId = apoyo.crearProducto(token, "SKU-TR-" + sufijo, "Producto TR", categoriaId, marcaId, unidadId);
        apoyo.asignarProductoATienda(token, productoId, tiendaOrigenId, "10.00");
        apoyo.asignarProductoATienda(token, productoId, tiendaDestinoId, "10.00");
        Long proveedorId = apoyo.crearProveedor(token, "NIT-TR" + sufijo, "Proveedor TR");
        apoyo.crearYRecibirCompra(token, tiendaOrigenId, proveedorId, productoId, "30", "5.00");

        assertThat(new BigDecimal(apoyo.existenciaActual(token, tiendaOrigenId, productoId)))
                .isEqualByComparingTo("30");
        assertThat(new BigDecimal(apoyo.existenciaActual(token, tiendaDestinoId, productoId)))
                .isEqualByComparingTo("0");

        Long trasladoId = apoyo.extraerId(apoyo.doPost(
                "/api/v1/traslados",
                Map.of(
                        "tiendaOrigenId", tiendaOrigenId, "tiendaDestinoId", tiendaDestinoId, "lineas",
                        List.of(Map.of("productoId", productoId, "cantidad", new BigDecimal("12")))),
                token));

        apoyo.doPost("/api/v1/traslados/" + trasladoId + "/completar", null, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADO"));

        assertThat(new BigDecimal(apoyo.existenciaActual(token, tiendaOrigenId, productoId)))
                .isEqualByComparingTo("18");
        assertThat(new BigDecimal(apoyo.existenciaActual(token, tiendaDestinoId, productoId)))
                .isEqualByComparingTo("12");
    }
}
