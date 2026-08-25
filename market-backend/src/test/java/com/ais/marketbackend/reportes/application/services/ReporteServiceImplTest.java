package com.ais.marketbackend.reportes.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.compras.application.dtos.CompraResumen;
import com.ais.marketbackend.compras.application.services.interfaces.CompraService;
import com.ais.marketbackend.compras.domain.model.EstadoCompra;
import com.ais.marketbackend.reportes.application.dtos.ReporteComprasResumen;
import com.ais.marketbackend.reportes.application.dtos.ReporteVentasResumen;
import com.ais.marketbackend.reportes.application.services.impl.ReporteServiceImpl;
import com.ais.marketbackend.reportes.domain.exception.RangoFechasInvalidoException;
import com.ais.marketbackend.ventas.application.dtos.VentaResumen;
import com.ais.marketbackend.ventas.application.services.interfaces.VentaService;
import com.ais.marketbackend.ventas.domain.model.EstadoVenta;
import com.ais.marketbackend.ventas.domain.model.MetodoPago;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReporteServiceImplTest {

    private VentaService ventaService;
    private CompraService compraService;
    private ReporteServiceImpl reporteService;

    private final Instant desde = Instant.parse("2026-08-01T00:00:00Z");
    private final Instant hasta = Instant.parse("2026-08-31T23:59:59Z");

    @BeforeEach
    void setUp() {
        ventaService = mock(VentaService.class);
        compraService = mock(CompraService.class);
        reporteService = new ReporteServiceImpl(ventaService, compraService);
        when(ventaService.listarPorTienda(1L)).thenReturn(List.of());
        when(compraService.listarPorTienda(1L)).thenReturn(List.of());
    }

    @Test
    void reporteVentasIncluyeSoloCompletadasDentroDelRango() {
        when(ventaService.listarPorTienda(1L)).thenReturn(List.of(
                venta(EstadoVenta.COMPLETADA, desde.plus(1, ChronoUnit.DAYS), new BigDecimal("100.00")),
                venta(EstadoVenta.COMPLETADA, desde.minus(1, ChronoUnit.DAYS), new BigDecimal("999.00")),
                venta(EstadoVenta.BORRADOR, desde.plus(1, ChronoUnit.DAYS), new BigDecimal("50.00"))));

        ReporteVentasResumen resumen = reporteService.reporteVentas(1L, desde, hasta);

        assertThat(resumen.cantidadVentas()).isEqualTo(1);
        assertThat(resumen.totalVentas()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void reporteVentasConRangoInvertidoLanzaExcepcion() {
        assertThatThrownBy(() -> reporteService.reporteVentas(1L, hasta, desde))
                .isInstanceOf(RangoFechasInvalidoException.class);
    }

    @Test
    void reporteComprasIncluyeSoloRecibidasDentroDelRango() {
        when(compraService.listarPorTienda(1L)).thenReturn(List.of(
                compra(EstadoCompra.RECIBIDA, desde.plus(1, ChronoUnit.DAYS), new BigDecimal("40.00")),
                compra(EstadoCompra.RECIBIDA, hasta.plus(1, ChronoUnit.DAYS), new BigDecimal("999.00")),
                compra(EstadoCompra.BORRADOR, desde.plus(1, ChronoUnit.DAYS), new BigDecimal("10.00"))));

        ReporteComprasResumen resumen = reporteService.reporteCompras(1L, desde, hasta);

        assertThat(resumen.cantidadCompras()).isEqualTo(1);
        assertThat(resumen.totalCompras()).isEqualByComparingTo(new BigDecimal("40.00"));
    }

    @Test
    void reporteComprasConRangoInvertidoLanzaExcepcion() {
        assertThatThrownBy(() -> reporteService.reporteCompras(1L, hasta, desde))
                .isInstanceOf(RangoFechasInvalidoException.class);
    }

    private VentaResumen venta(EstadoVenta estado, Instant fecha, BigDecimal total) {
        return new VentaResumen(1L, 2L, 1L, 4L, fecha, estado, List.of(), total, MetodoPago.EFECTIVO);
    }

    private CompraResumen compra(EstadoCompra estado, Instant fecha, BigDecimal total) {
        return new CompraResumen(1L, 2L, 1L, fecha, estado, List.of(), total);
    }
}
