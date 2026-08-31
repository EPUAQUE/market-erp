package com.ais.marketbackend.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
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
 * Fase 5 (PLAN_MEJORAS.md), "Cobertura prioritaria" — flujo E2E: crear una
 * compra → recibirla → verificar que el inventario subió y nació la cuenta
 * por pagar correspondiente, con el saldo completo de la compra.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class CompraRecepcionE2EIT {

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
    void recibirCompraSubeInventarioYCreaCuentaPorPagarPendiente() throws Exception {
        String sufijo = String.valueOf(System.nanoTime() % 100_000_000L);
        String token = apoyo.login("admin", "Admin1234!Seguro");

        Long grupoId = apoyo.crearGrupoTienda(token, "G-CR" + sufijo, "Grupo compra recepcion");
        Long tiendaId = apoyo.crearTienda(token, "T-CR" + sufijo, "Tienda compra recepcion", grupoId);
        Long categoriaId = apoyo.crearCategoria(token, "Categoria CR " + sufijo);
        Long marcaId = apoyo.crearMarca(token, "Marca CR " + sufijo);
        Long unidadId = apoyo.crearUnidadMedida(token, "Unidad CR " + sufijo, "u");
        Long productoId = apoyo.crearProducto(token, "SKU-CR-" + sufijo, "Producto CR", categoriaId, marcaId, unidadId);
        apoyo.asignarProductoATienda(token, productoId, tiendaId, "10.00");
        Long proveedorId = apoyo.crearProveedor(token, "NIT-CR" + sufijo, "Proveedor CR");

        assertThat(new BigDecimal(apoyo.existenciaActual(token, tiendaId, productoId)))
                .isEqualByComparingTo("0");

        Long compraId = apoyo.extraerId(apoyo.doPost(
                "/api/v1/compras/tiendas/" + tiendaId,
                Map.of(
                        "proveedorId", proveedorId, "lineas",
                        List.of(Map.of("productoId", productoId, "cantidad", new BigDecimal("20"), "costoUnitario",
                                new BigDecimal("6.00")))),
                token));

        String compraJson = apoyo.doPost("/api/v1/compras/tiendas/" + tiendaId + "/" + compraId + "/recibir", null, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECIBIDA"))
                .andExpect(jsonPath("$.total").value("120.0000"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(compraJson).contains("RECIBIDA");

        assertThat(new BigDecimal(apoyo.existenciaActual(token, tiendaId, productoId)))
                .isEqualByComparingTo("20");

        String cuentasJson = apoyo.doGet("/api/v1/cuentas-por-pagar/tiendas/" + tiendaId, token)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Map<String, Object>> cuentas = JsonPath.read(cuentasJson, "$.contenido[?(@.compraId == " + compraId + ")]");
        assertThat(cuentas).hasSize(1);
        Map<String, Object> cuenta = cuentas.get(0);
        assertThat(cuenta.get("estado")).isEqualTo("PENDIENTE");
        assertThat(new BigDecimal(cuenta.get("saldoPendiente").toString())).isEqualByComparingTo("120.00");
        assertThat(new BigDecimal(cuenta.get("montoOriginal").toString())).isEqualByComparingTo("120.00");
    }
}
