package com.ais.marketbackend;

import static org.assertj.core.api.Assertions.assertThat;

import com.ais.marketbackend.caja.api.dtos.responses.CajaSesionResponse;
import com.ais.marketbackend.caja.api.dtos.responses.MovimientoCajaResponse;
import com.ais.marketbackend.caja.domain.model.EstadoCajaSesion;
import com.ais.marketbackend.caja.domain.model.TipoMovimientoCaja;
import com.ais.marketbackend.clientes.api.dtos.responses.ClienteResponse;
import com.ais.marketbackend.clientes.domain.model.EstadoCliente;
import com.ais.marketbackend.cuentasporcobrar.api.dtos.responses.CobroResponse;
import com.ais.marketbackend.cuentasporcobrar.api.dtos.responses.CuentaPorCobrarResponse;
import com.ais.marketbackend.cuentasporcobrar.domain.model.EstadoCuentaPorCobrar;
import com.ais.marketbackend.productos.api.dtos.responses.ProductoResponse;
import com.ais.marketbackend.ventas.api.dtos.responses.LineaVentaResponse;
import com.ais.marketbackend.ventas.api.dtos.responses.VentaResponse;
import com.ais.marketbackend.ventas.domain.model.EstadoVenta;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Congela la forma JSON de los DTOs de respuesta que market-flutter y
 * market-backoffice parsean por nombre de campo (Fase 5, PLAN_MEJORAS.md).
 * Si un test de esta clase revienta, un campo cambió de nombre/tipo/desapareció
 * sin que nadie lo haya decidido a propósito para ese contrato — antes de
 * actualizar el snapshot en src/test/resources/contracts/, hay que confirmar
 * que ambos clientes ya saben manejar el cambio.
 */
class DtoContractSnapshotTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void ventaResponse() throws IOException {
        VentaResponse venta = VentaResponse.builder()
                .id(1L)
                .clienteId(2L)
                .tiendaId(3L)
                .vendedorId(4L)
                .fecha(Instant.parse("2026-01-01T00:00:00Z"))
                .estado(EstadoVenta.COMPLETADA)
                .lineas(List.of(LineaVentaResponse.builder()
                        .id(10L)
                        .productoId(20L)
                        .cantidad("2.000")
                        .precioUnitario("8.00")
                        .build()))
                .total("16.00")
                .metodoPago(com.ais.marketbackend.ventas.domain.model.MetodoPago.EFECTIVO)
                .build();

        assertMatchesSnapshot(venta, "contracts/venta-response.json");
    }

    @Test
    void clienteResponse() throws IOException {
        ClienteResponse cliente = ClienteResponse.builder()
                .id(1L)
                .nit("12345678-9")
                .nombre("Juan Pérez")
                .direccion("Zona 1")
                .telefono("12345678")
                .correo("juan@example.com")
                .estado(EstadoCliente.ACTIVO)
                .limiteCredito("1000.00")
                .build();

        assertMatchesSnapshot(cliente, "contracts/cliente-response.json");
    }

    @Test
    void cuentaPorCobrarResponse() throws IOException {
        CuentaPorCobrarResponse cuenta = CuentaPorCobrarResponse.builder()
                .id(1L)
                .ventaId(2L)
                .clienteId(3L)
                .tiendaId(4L)
                .fechaEmision(Instant.parse("2026-01-01T00:00:00Z"))
                .fechaVencimiento(Instant.parse("2026-01-31T00:00:00Z"))
                .montoOriginal("100.00")
                .saldoPendiente("50.00")
                .estado(EstadoCuentaPorCobrar.PENDIENTE)
                .cobros(List.of(CobroResponse.builder()
                        .id(10L)
                        .fecha(Instant.parse("2026-01-05T00:00:00Z"))
                        .monto("50.00")
                        .metodoPago(com.ais.marketbackend.cuentasporcobrar.domain.model.MetodoPago.EFECTIVO)
                        .build()))
                .build();

        assertMatchesSnapshot(cuenta, "contracts/cuenta-por-cobrar-response.json");
    }

    @Test
    void productoResponse() throws IOException {
        ProductoResponse producto = ProductoResponse.builder()
                .id(1L)
                .codigoInterno("PROD-001")
                .codigoBarras("7501234567890")
                .nombre("Coca Cola 600ml")
                .descripcion("Refresco")
                .categoriaId(2L)
                .marcaId(3L)
                .unidadMedidaId(4L)
                .imagenUrl("https://example.com/img.jpg")
                .activo(true)
                .build();

        assertMatchesSnapshot(producto, "contracts/producto-response.json");
    }

    @Test
    void cajaSesionResponse() throws IOException {
        CajaSesionResponse caja = CajaSesionResponse.builder()
                .id(1L)
                .tiendaId(2L)
                .fechaApertura(Instant.parse("2026-01-01T08:00:00Z"))
                .fechaCierre(Instant.parse("2026-01-01T18:00:00Z"))
                .montoInicial("100.00")
                .montoFinalContado("545.00")
                .saldoEsperado("550.00")
                .estado(EstadoCajaSesion.CERRADA)
                .movimientos(List.of(MovimientoCajaResponse.builder()
                        .id(10L)
                        .fecha(Instant.parse("2026-01-01T10:00:00Z"))
                        .tipo(TipoMovimientoCaja.INGRESO)
                        .concepto("Venta")
                        .monto("50.00")
                        .build()))
                .build();

        assertMatchesSnapshot(caja, "contracts/caja-sesion-response.json");
    }

    private void assertMatchesSnapshot(Object dto, String resourcePath) throws IOException {
        JsonNode actual = mapper.readTree(mapper.writeValueAsString(dto));
        JsonNode expected;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(in).as("snapshot no encontrado: %s", resourcePath).isNotNull();
            expected = mapper.readTree(in);
        }
        assertThat(actual).as("forma JSON de %s no coincide con el snapshot", resourcePath).isEqualTo(expected);
    }
}
