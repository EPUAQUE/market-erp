package com.ais.marketbackend.reportes.application.services.impl;

import com.ais.marketbackend.compras.application.services.interfaces.CompraService;
import com.ais.marketbackend.compras.domain.model.EstadoCompra;
import com.ais.marketbackend.reportes.application.dtos.LineaReporteCompra;
import com.ais.marketbackend.reportes.application.dtos.LineaReporteVenta;
import com.ais.marketbackend.reportes.application.dtos.ReporteComprasResumen;
import com.ais.marketbackend.reportes.application.dtos.ReporteVentasResumen;
import com.ais.marketbackend.reportes.application.services.interfaces.ReporteService;
import com.ais.marketbackend.reportes.domain.exception.RangoFechasInvalidoException;
import com.ais.marketbackend.ventas.application.services.interfaces.VentaService;
import com.ais.marketbackend.ventas.domain.model.EstadoVenta;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * {@code ventaService}/{@code compraService} son dependencias cruzadas
 * permitidas: solo se usan sus puertos {@code application.services.interfaces},
 * en modo lectura. Este módulo no tiene modelo de dominio ni tabla propia —
 * solo filtra y agrega datos ya existentes por rango de fechas.
 */
@Service
public class ReporteServiceImpl implements ReporteService {

    private final VentaService ventaService;
    private final CompraService compraService;

    public ReporteServiceImpl(VentaService ventaService, CompraService compraService) {
        this.ventaService = ventaService;
        this.compraService = compraService;
    }

    @Override
    public ReporteVentasResumen reporteVentas(Long tiendaId, Instant desde, Instant hasta) {
        requerirRangoValido(desde, hasta);
        List<LineaReporteVenta> lineas = ventaService.listarPorTienda(tiendaId).stream()
                .filter(v -> v.estado() == EstadoVenta.COMPLETADA)
                .filter(v -> !v.fecha().isBefore(desde) && !v.fecha().isAfter(hasta))
                .map(v -> new LineaReporteVenta(v.id(), v.clienteId(), v.fecha(), v.total()))
                .toList();
        BigDecimal totalVentas = lineas.stream().map(LineaReporteVenta::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReporteVentasResumen(tiendaId, desde, hasta, totalVentas, lineas.size(), lineas);
    }

    @Override
    public ReporteComprasResumen reporteCompras(Long tiendaId, Instant desde, Instant hasta) {
        requerirRangoValido(desde, hasta);
        List<LineaReporteCompra> lineas = compraService.listarPorTienda(tiendaId).stream()
                .filter(c -> c.estado() == EstadoCompra.RECIBIDA)
                .filter(c -> !c.fecha().isBefore(desde) && !c.fecha().isAfter(hasta))
                .map(c -> new LineaReporteCompra(c.id(), c.proveedorId(), c.fecha(), c.total()))
                .toList();
        BigDecimal totalCompras = lineas.stream()
                .map(LineaReporteCompra::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReporteComprasResumen(tiendaId, desde, hasta, totalCompras, lineas.size(), lineas);
    }

    private void requerirRangoValido(Instant desde, Instant hasta) {
        if (desde.isAfter(hasta)) {
            throw new RangoFechasInvalidoException();
        }
    }
}
