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
 * Fase 5 (PLAN_MEJORAS.md), "Cobertura prioritaria" — flujo E2E de negocio
 * completo vía HTTP real (login real + JWT real, MockMvc corre la cadena
 * completa de Spring Security/MVC): abrir caja → completar una venta en
 * efectivo → verificar que el inventario bajó y la caja registró el ingreso.
 * Primer test del repo que hace login real y reutiliza el JWT devuelto para
 * llamar endpoints protegidos (antes de esta fase, todos los IT autenticaban
 * con un {@code SecurityContextHolder} simulado, sin pasar por HTTP).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class VentaContadoE2EIT {

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
    void abrirCajaCompletarVentaEfectivoBajaInventarioYRegistraIngreso() throws Exception {
        String sufijo = String.valueOf(System.nanoTime() % 100_000_000L);
        String token = apoyo.login("admin", "Admin1234!Seguro");

        Long grupoId = apoyo.crearGrupoTienda(token, "G-VC" + sufijo, "Grupo venta contado");
        Long tiendaId = apoyo.crearTienda(token, "T-VC" + sufijo, "Tienda venta contado", grupoId);
        Long categoriaId = apoyo.crearCategoria(token, "Categoria VC " + sufijo);
        Long marcaId = apoyo.crearMarca(token, "Marca VC " + sufijo);
        Long unidadId = apoyo.crearUnidadMedida(token, "Unidad VC " + sufijo, "u");
        Long productoId = apoyo.crearProducto(token, "SKU-VC-" + sufijo, "Producto VC", categoriaId, marcaId, unidadId);
        apoyo.asignarProductoATienda(token, productoId, tiendaId, "10.00");
        Long proveedorId = apoyo.crearProveedor(token, "NIT-VC" + sufijo, "Proveedor VC");
        apoyo.crearYRecibirCompra(token, tiendaId, proveedorId, productoId, "50", "6.00");
        Long clienteId = apoyo.crearCliente(token, "Cliente VC " + sufijo, null);

        assertThat(new BigDecimal(apoyo.existenciaActual(token, tiendaId, productoId)))
                .isEqualByComparingTo("50");

        apoyo.doPost("/api/v1/caja/tiendas/" + tiendaId + "/abrir", Map.of("montoInicial", new BigDecimal("500.00")), token)
                .andExpect(status().isCreated());

        Long ventaId = apoyo.extraerId(apoyo.doPost(
                "/api/v1/ventas/tiendas/" + tiendaId,
                Map.of(
                        "clienteId", clienteId, "lineas",
                        List.of(Map.of("productoId", productoId, "cantidad", new BigDecimal("3"), "precioUnitario",
                                new BigDecimal("10.00"))),
                        "metodoPago", "EFECTIVO", "correlationId", java.util.UUID.randomUUID().toString()),
                token));

        apoyo.doPost("/api/v1/ventas/tiendas/" + tiendaId + "/" + ventaId + "/completar", null, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"));

        assertThat(new BigDecimal(apoyo.existenciaActual(token, tiendaId, productoId)))
                .isEqualByComparingTo("47");

        String cajaJson = apoyo.doGet("/api/v1/caja/tiendas/" + tiendaId + "/abierta", token)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> montosIngreso = JsonPath.read(cajaJson, "$.movimientos[?(@.tipo == 'INGRESO')].monto");
        assertThat(montosIngreso).hasSize(1);
        assertThat(new BigDecimal(montosIngreso.get(0))).isEqualByComparingTo("30.00");
    }
}
