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
 * Fase 5 (PLAN_MEJORAS.md), "Cobertura prioritaria" — flujo E2E: completar una
 * venta a crédito → nace la cuenta por cobrar por el total → un cobro parcial
 * la deja PENDIENTE con saldo reducido → un segundo cobro por el resto la deja
 * en COBRADA con saldo cero. De paso ejercita el endpoint nuevo de Fase 11
 * (`GET .../por-venta/{ventaId}`) contra el backend real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class VentaCreditoCobroE2EIT {

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
    void ventaACreditoCobradaEnDosPartesQuedaConSaldoCeroYEstadoCobrada() throws Exception {
        String sufijo = String.valueOf(System.nanoTime() % 100_000_000L);
        String token = apoyo.login("admin", "Admin1234!Seguro");

        Long grupoId = apoyo.crearGrupoTienda(token, "G-VCR" + sufijo, "Grupo venta credito");
        Long tiendaId = apoyo.crearTienda(token, "T-VCR" + sufijo, "Tienda venta credito", grupoId);
        Long categoriaId = apoyo.crearCategoria(token, "Categoria VCR " + sufijo);
        Long marcaId = apoyo.crearMarca(token, "Marca VCR " + sufijo);
        Long unidadId = apoyo.crearUnidadMedida(token, "Unidad VCR " + sufijo, "u");
        Long productoId =
                apoyo.crearProducto(token, "SKU-VCR-" + sufijo, "Producto VCR", categoriaId, marcaId, unidadId);
        apoyo.asignarProductoATienda(token, productoId, tiendaId, "20.00");
        Long proveedorId = apoyo.crearProveedor(token, "NIT-VCR" + sufijo, "Proveedor VCR");
        apoyo.crearYRecibirCompra(token, tiendaId, proveedorId, productoId, "10", "10.00");
        Long clienteId = apoyo.crearCliente(token, "Cliente VCR " + sufijo, new BigDecimal("1000.00"));

        Long ventaId = apoyo.extraerId(apoyo.doPost(
                "/api/v1/ventas/tiendas/" + tiendaId,
                Map.of(
                        "clienteId", clienteId, "lineas",
                        List.of(Map.of("productoId", productoId, "cantidad", new BigDecimal("2"), "precioUnitario",
                                new BigDecimal("20.00"))),
                        "metodoPago", "CREDITO", "correlationId", java.util.UUID.randomUUID().toString()),
                token));

        apoyo.doPost("/api/v1/ventas/tiendas/" + tiendaId + "/" + ventaId + "/completar", null, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"));

        Long cuentaId = apoyo.extraerId(
                apoyo.doGet("/api/v1/cuentas-por-cobrar/tiendas/" + tiendaId + "/por-venta/" + ventaId, token)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                        .andExpect(jsonPath("$.saldoPendiente").value("40.0000")));

        apoyo.doPost(
                "/api/v1/cuentas-por-cobrar/tiendas/" + tiendaId + "/" + cuentaId + "/cobros",
                Map.of("monto", new BigDecimal("15.00"), "metodoPago", "EFECTIVO"),
                token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.saldoPendiente").value("25.0000"));

        apoyo.doPost(
                "/api/v1/cuentas-por-cobrar/tiendas/" + tiendaId + "/" + cuentaId + "/cobros",
                Map.of("monto", new BigDecimal("25.00"), "metodoPago", "TRANSFERENCIA"),
                token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COBRADA"))
                .andExpect(jsonPath("$.saldoPendiente").value("0.0000"));

        String finalJson = apoyo.doGet("/api/v1/cuentas-por-cobrar/tiendas/" + tiendaId + "/" + cuentaId, token)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(finalJson).contains("\"estado\":\"COBRADA\"");
    }
}
