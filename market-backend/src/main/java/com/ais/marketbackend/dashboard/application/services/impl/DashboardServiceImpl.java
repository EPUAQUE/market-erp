package com.ais.marketbackend.dashboard.application.services.impl;

import com.ais.marketbackend.caja.application.dtos.CajaSesionResumen;
import com.ais.marketbackend.caja.application.services.interfaces.CajaService;
import com.ais.marketbackend.caja.domain.model.TipoMovimientoCaja;
import com.ais.marketbackend.cuentasporcobrar.application.dtos.CuentaPorCobrarResumen;
import com.ais.marketbackend.cuentasporcobrar.application.services.interfaces.CuentaPorCobrarService;
import com.ais.marketbackend.cuentasporcobrar.domain.model.EstadoCuentaPorCobrar;
import com.ais.marketbackend.cuentasporpagar.application.dtos.CuentaPorPagarResumen;
import com.ais.marketbackend.cuentasporpagar.application.services.interfaces.CuentaPorPagarService;
import com.ais.marketbackend.cuentasporpagar.domain.model.EstadoCuentaPorPagar;
import com.ais.marketbackend.dashboard.application.dtos.CuentaPendienteResumen;
import com.ais.marketbackend.dashboard.application.dtos.DashboardResumen;
import com.ais.marketbackend.dashboard.application.dtos.RecordatorioResumen;
import com.ais.marketbackend.dashboard.application.dtos.SugerenciaCompraResumen;
import com.ais.marketbackend.dashboard.application.dtos.SugerenciaTrasladoResumen;
import com.ais.marketbackend.dashboard.application.dtos.VencimientoResumen;
import com.ais.marketbackend.dashboard.application.services.interfaces.DashboardService;
import com.ais.marketbackend.fel.application.services.interfaces.FelService;
import com.ais.marketbackend.fel.domain.model.EstadoDocumentoFel;
import com.ais.marketbackend.gastosprogramados.application.services.interfaces.GastoProgramadoService;
import com.ais.marketbackend.inventario.application.dtos.InventarioResumen;
import com.ais.marketbackend.inventario.application.dtos.MovimientoInventarioResumen;
import com.ais.marketbackend.inventario.application.services.interfaces.InventarioService;
import com.ais.marketbackend.notificaciones.application.services.interfaces.NotificacionService;
import com.ais.marketbackend.notificaciones.domain.model.TipoNotificacion;
import com.ais.marketbackend.productos.application.dtos.ProductoTiendaResumen;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoTiendaService;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import com.ais.marketbackend.tiendas.application.services.interfaces.TiendaService;
import com.ais.marketbackend.tiendas.domain.model.EstadoTienda;
import com.ais.marketbackend.ventas.application.dtos.VentaResumen;
import com.ais.marketbackend.ventas.application.services.interfaces.VentaService;
import com.ais.marketbackend.ventas.domain.model.EstadoVenta;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Los ocho servicios de otros módulos son dependencias cruzadas permitidas:
 * solo se usan sus puertos {@code application.services.interfaces}, en modo
 * lectura, para componer un resumen agregado. Este módulo no tiene modelo de
 * dominio ni tabla propia — es puro cálculo sobre datos ya existentes.
 *
 * <p>La utilidad/margen ({@code utilidadMesTotal}, {@code margenPromedioMes}) es una
 * aproximación: se calcula con el costo promedio ACTUAL de cada producto (Inventario
 * no conserva el costo histórico por línea de venta), no con el costo real vigente al
 * momento de cada venta. Suficiente como indicador gerencial, no como cifra contable.
 *
 * <p>Los cortes de "hoy"/"mes" se calculan en {@code app.negocio.zona-horaria}
 * (default {@code America/Guatemala}), no en UTC — de lo contrario el día del negocio
 * rota a media tarde en vez de a medianoche local.
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private static final BigDecimal FACTOR_EXCEDENTE_TRASLADO = new BigDecimal("1.3");
    private static final int DIAS_SIN_MOVIMIENTO = 60;
    private static final int DIAS_HORIZONTE_VENCIMIENTOS = 30;
    private static final int DIAS_HORIZONTE_RECORDATORIOS = 7;

    private final VentaService ventaService;
    private final CuentaPorCobrarService cuentaPorCobrarService;
    private final CuentaPorPagarService cuentaPorPagarService;
    private final ProductoTiendaService productoTiendaService;
    private final InventarioService inventarioService;
    private final CajaService cajaService;
    private final FelService felService;
    private final NotificacionService notificacionService;
    private final GastoProgramadoService gastoProgramadoService;
    private final TiendaService tiendaService;
    private final ZoneId zonaHorariaNegocio;

    public DashboardServiceImpl(
            VentaService ventaService, CuentaPorCobrarService cuentaPorCobrarService,
            CuentaPorPagarService cuentaPorPagarService, ProductoTiendaService productoTiendaService,
            InventarioService inventarioService, CajaService cajaService, FelService felService,
            NotificacionService notificacionService, GastoProgramadoService gastoProgramadoService,
            TiendaService tiendaService,
            @Value("${app.negocio.zona-horaria:America/Guatemala}") String zonaHorariaNegocio) {
        this.ventaService = ventaService;
        this.cuentaPorCobrarService = cuentaPorCobrarService;
        this.cuentaPorPagarService = cuentaPorPagarService;
        this.productoTiendaService = productoTiendaService;
        this.inventarioService = inventarioService;
        this.cajaService = cajaService;
        this.felService = felService;
        this.notificacionService = notificacionService;
        this.gastoProgramadoService = gastoProgramadoService;
        this.tiendaService = tiendaService;
        this.zonaHorariaNegocio = ZoneId.of(zonaHorariaNegocio);
    }

    @Override
    public DashboardResumen obtenerResumen(Long tiendaId) {
        Instant ahora = Instant.now();
        // El "día"/"mes" del negocio se calcula en la zona horaria configurada, no en
        // UTC — con UTC fijo el día rota a las 6pm hora Guatemala (UTC-6) y ventas de
        // madrugada quedaban contadas como del día anterior.
        ZonedDateTime ahoraZoned = ahora.atZone(zonaHorariaNegocio);
        Instant inicioDeHoy = ahoraZoned.toLocalDate().atStartOfDay(zonaHorariaNegocio).toInstant();
        Instant inicioDeMes =
                ahoraZoned.toLocalDate().withDayOfMonth(1).atStartOfDay(zonaHorariaNegocio).toInstant();
        Instant inicioDeMesAnterior = ahoraZoned.toLocalDate().minusMonths(1).withDayOfMonth(1)
                .atStartOfDay(zonaHorariaNegocio).toInstant();

        List<InventarioResumen> inventarioTienda = inventarioService.listarPorTienda(tiendaId);
        Map<Long, BigDecimal> existenciaPorProducto = inventarioTienda.stream()
                .collect(Collectors.toMap(InventarioResumen::productoId, InventarioResumen::existenciaActual));
        Map<Long, BigDecimal> costoPorProducto = inventarioTienda.stream()
                .collect(Collectors.toMap(InventarioResumen::productoId, InventarioResumen::costoPromedioActual));

        var todasLasVentas = ventaService.listarPorTienda(tiendaId);
        var ventasCompletadas =
                todasLasVentas.stream().filter(v -> v.estado() == EstadoVenta.COMPLETADA).toList();
        var ventasHoy = ventasCompletadas.stream().filter(v -> !v.fecha().isBefore(inicioDeHoy)).toList();
        var ventasMes = ventasCompletadas.stream().filter(v -> !v.fecha().isBefore(inicioDeMes)).toList();
        var ventasMesAnterior = ventasCompletadas.stream()
                .filter(v -> !v.fecha().isBefore(inicioDeMesAnterior) && v.fecha().isBefore(inicioDeMes))
                .toList();

        BigDecimal ventasHoyTotal = sumar(ventasHoy, VentaResumen::total);
        BigDecimal ventasMesTotal = sumar(ventasMes, VentaResumen::total);
        BigDecimal ventasMesAnteriorTotal = sumar(ventasMesAnterior, VentaResumen::total);
        BigDecimal ticketPromedioMes = dividir(ventasMesTotal, BigDecimal.valueOf(ventasMes.size()));

        long facturasFelCertificadasMes = felService.listarPorTienda(tiendaId).stream()
                .filter(f -> f.estado() == EstadoDocumentoFel.CERTIFICADO)
                .filter(f -> f.fechaCertificacion() != null && !f.fechaCertificacion().isBefore(inicioDeMes))
                .count();

        BigDecimal utilidadMesTotal = ventasMes.stream()
                .flatMap(v -> v.lineas().stream())
                .map(l -> {
                    BigDecimal costo = costoPorProducto.getOrDefault(l.productoId(), BigDecimal.ZERO);
                    return l.precioUnitario().subtract(costo).multiply(l.cantidad());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal margenPromedioMes = ventasMesTotal.compareTo(BigDecimal.ZERO) == 0
                ? null
                : utilidadMesTotal.multiply(BigDecimal.valueOf(100)).divide(ventasMesTotal, 2, RoundingMode.HALF_UP);

        BigDecimal inventarioValorizadoTotal = inventarioTienda.stream()
                .map(i -> i.existenciaActual().multiply(i.costoPromedioActual()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var productosTienda = productoTiendaService.listarPorTienda(tiendaId).stream()
                .filter(ProductoTiendaResumen::activo)
                .toList();
        long productosAgotados = productosTienda.stream()
                .filter(pt -> existenciaPorProducto.getOrDefault(pt.productoId(), BigDecimal.ZERO)
                        .compareTo(BigDecimal.ZERO) == 0)
                .count();
        var productosBajoMinimo = productosTienda.stream()
                .filter(pt -> {
                    BigDecimal existencia = existenciaPorProducto.getOrDefault(pt.productoId(), BigDecimal.ZERO);
                    return existencia.compareTo(BigDecimal.ZERO) > 0 && existencia.compareTo(pt.stockMinimo()) < 0;
                })
                .toList();
        long productosSinMovimiento = productosTienda.stream()
                .filter(pt -> estaSinMovimientoReciente(tiendaId, pt.productoId(), ahora))
                .count();

        var cuentasPorCobrarPendientes = cuentaPorCobrarService.listarPorTienda(tiendaId).stream()
                .filter(c -> c.estado() == EstadoCuentaPorCobrar.PENDIENTE)
                .toList();
        BigDecimal saldoPendienteCuentasPorCobrar = sumar(cuentasPorCobrarPendientes, CuentaPorCobrarResumen::saldoPendiente);
        long cuentasPorCobrarVencidas =
                cuentasPorCobrarPendientes.stream().filter(c -> c.fechaVencimiento().isBefore(ahora)).count();
        BigDecimal[] cxcAging = calcularAging(cuentasPorCobrarPendientes, CuentaPorCobrarResumen::fechaVencimiento,
                CuentaPorCobrarResumen::saldoPendiente, ahora);

        var cuentasPorPagarPendientes = cuentaPorPagarService.listarPorTienda(tiendaId).stream()
                .filter(c -> c.estado() == EstadoCuentaPorPagar.PENDIENTE)
                .toList();
        BigDecimal saldoPendienteCuentasPorPagar = sumar(cuentasPorPagarPendientes, CuentaPorPagarResumen::saldoPendiente);
        long cuentasPorPagarVencidas =
                cuentasPorPagarPendientes.stream().filter(c -> c.fechaVencimiento().isBefore(ahora)).count();
        BigDecimal[] cxpAging = calcularAging(cuentasPorPagarPendientes, CuentaPorPagarResumen::fechaVencimiento,
                CuentaPorPagarResumen::saldoPendiente, ahora);

        boolean cajaAbierta;
        BigDecimal cajaSaldoEsperado;
        try {
            var caja = cajaService.obtenerAbierta(tiendaId);
            cajaAbierta = true;
            cajaSaldoEsperado = caja.saldoEsperado();
        } catch (ResourceNotFoundException e) {
            cajaAbierta = false;
            cajaSaldoEsperado = null;
        }
        BigDecimal ingresosHoy = BigDecimal.ZERO;
        BigDecimal egresosHoy = BigDecimal.ZERO;
        for (CajaSesionResumen sesion : cajaService.listarPorTienda(tiendaId)) {
            for (var movimiento : sesion.movimientos()) {
                if (movimiento.fecha().isBefore(inicioDeHoy)) {
                    continue;
                }
                if (movimiento.tipo() == TipoMovimientoCaja.INGRESO) {
                    ingresosHoy = ingresosHoy.add(movimiento.monto());
                } else {
                    egresosHoy = egresosHoy.add(movimiento.monto());
                }
            }
        }

        var notificacionesNoLeidas = notificacionService.listarNoLeidasPorTienda(tiendaId);
        long alertasPreventivas = notificacionesNoLeidas.stream()
                .filter(n -> n.tipo() == TipoNotificacion.STOCK_BAJO)
                .count();
        long alertasCriticas = notificacionesNoLeidas.size() - alertasPreventivas;

        List<VencimientoResumen> proximosVencimientos = construirProximosVencimientos(
                cuentasPorCobrarPendientes, cuentasPorPagarPendientes, ahora);

        List<CuentaPendienteResumen> topCobrosPendientes = cuentasPorCobrarPendientes.stream()
                .sorted(Comparator.comparing(CuentaPorCobrarResumen::saldoPendiente).reversed())
                .limit(5)
                .map(c -> new CuentaPendienteResumen(c.id(), c.clienteId(), c.saldoPendiente(), c.fechaVencimiento()))
                .toList();
        List<CuentaPendienteResumen> topPagosPendientes = cuentasPorPagarPendientes.stream()
                .sorted(Comparator.comparing(CuentaPorPagarResumen::saldoPendiente).reversed())
                .limit(5)
                .map(c -> new CuentaPendienteResumen(c.id(), c.proveedorId(), c.saldoPendiente(), c.fechaVencimiento()))
                .toList();

        Instant horizonteRecordatorios = ahora.plus(DIAS_HORIZONTE_RECORDATORIOS, ChronoUnit.DAYS);
        List<RecordatorioResumen> recordatorios = gastoProgramadoService.listarPorTienda(tiendaId).stream()
                .filter(g -> g.activo() && !g.proximaFecha().isAfter(horizonteRecordatorios))
                .map(g -> new RecordatorioResumen(g.id(), g.concepto(), g.monto(), g.proximaFecha()))
                .toList();

        List<SugerenciaCompraResumen> sugerenciasCompra = productosBajoMinimo.stream()
                .map(pt -> {
                    BigDecimal existencia = existenciaPorProducto.getOrDefault(pt.productoId(), BigDecimal.ZERO);
                    BigDecimal cantidadSugerida = pt.stockMaximo().subtract(existencia);
                    if (cantidadSugerida.compareTo(BigDecimal.ZERO) < 0) {
                        cantidadSugerida = BigDecimal.ZERO;
                    }
                    return new SugerenciaCompraResumen(pt.productoId(), existencia, pt.stockMinimo(), cantidadSugerida);
                })
                .toList();

        List<SugerenciaTrasladoResumen> sugerenciasTraslado =
                construirSugerenciasTraslado(tiendaId, productosBajoMinimo, existenciaPorProducto);

        return new DashboardResumen(
                tiendaId, ventasHoyTotal, ventasHoy.size(), ventasMesTotal, ventasMes.size(), ventasMesAnteriorTotal,
                ticketPromedioMes, ventasMes.size(), facturasFelCertificadasMes, utilidadMesTotal, margenPromedioMes,
                inventarioValorizadoTotal, productosAgotados, (long) productosBajoMinimo.size(), productosSinMovimiento,
                saldoPendienteCuentasPorCobrar, cuentasPorCobrarVencidas, cxcAging[0], cxcAging[1], cxcAging[2],
                saldoPendienteCuentasPorPagar, cuentasPorPagarVencidas, cxpAging[0], cxpAging[1], cxpAging[2],
                cajaAbierta, cajaSaldoEsperado, ingresosHoy, egresosHoy, alertasCriticas, alertasPreventivas,
                proximosVencimientos, topCobrosPendientes, topPagosPendientes, recordatorios, sugerenciasCompra,
                sugerenciasTraslado);
    }

    private boolean estaSinMovimientoReciente(Long tiendaId, Long productoId, Instant ahora) {
        List<MovimientoInventarioResumen> movimientos = inventarioService.listarMovimientos(tiendaId, productoId);
        if (movimientos.isEmpty()) {
            return true;
        }
        Instant ultimoMovimiento = movimientos.stream()
                .map(MovimientoInventarioResumen::fecha)
                .max(Comparator.naturalOrder())
                .orElse(Instant.EPOCH);
        return ultimoMovimiento.isBefore(ahora.minus(DIAS_SIN_MOVIMIENTO, ChronoUnit.DAYS));
    }

    private List<VencimientoResumen> construirProximosVencimientos(
            List<CuentaPorCobrarResumen> cuentasPorCobrarPendientes, List<CuentaPorPagarResumen> cuentasPorPagarPendientes,
            Instant ahora) {
        Instant horizonte = ahora.plus(DIAS_HORIZONTE_VENCIMIENTOS, ChronoUnit.DAYS);
        var deCobrar = cuentasPorCobrarPendientes.stream()
                .filter(c -> !c.fechaVencimiento().isAfter(horizonte))
                .map(c -> new VencimientoResumen("CUENTA_POR_COBRAR", c.id(), c.saldoPendiente(), c.fechaVencimiento()));
        var dePagar = cuentasPorPagarPendientes.stream()
                .filter(c -> !c.fechaVencimiento().isAfter(horizonte))
                .map(c -> new VencimientoResumen("CUENTA_POR_PAGAR", c.id(), c.saldoPendiente(), c.fechaVencimiento()));
        return java.util.stream.Stream.concat(deCobrar, dePagar)
                .sorted(Comparator.comparing(VencimientoResumen::fechaVencimiento))
                .limit(10)
                .toList();
    }

    private List<SugerenciaTrasladoResumen> construirSugerenciasTraslado(
            Long tiendaId, List<ProductoTiendaResumen> productosBajoMinimo,
            Map<Long, BigDecimal> existenciaPorProducto) {
        if (productosBajoMinimo.isEmpty()) {
            return List.of();
        }
        var otrasTiendas = tiendaService.listar().stream()
                .filter(t -> t.estado() == EstadoTienda.ACTIVA && !t.id().equals(tiendaId))
                .toList();

        record InventarioTienda(Map<Long, BigDecimal> existencias, Map<Long, ProductoTiendaResumen> productos) {
        }
        Map<Long, InventarioTienda> porTienda = otrasTiendas.stream()
                .collect(Collectors.toMap(t -> t.id(), t -> new InventarioTienda(
                        inventarioService.listarPorTienda(t.id()).stream()
                                .collect(Collectors.toMap(InventarioResumen::productoId, InventarioResumen::existenciaActual)),
                        productoTiendaService.listarPorTienda(t.id()).stream()
                                .collect(Collectors.toMap(ProductoTiendaResumen::productoId, pt -> pt)))));

        List<SugerenciaTrasladoResumen> sugerencias = new java.util.ArrayList<>();
        for (ProductoTiendaResumen productoBajoMinimo : productosBajoMinimo) {
            Long productoId = productoBajoMinimo.productoId();
            for (var otraTienda : otrasTiendas) {
                InventarioTienda datos = porTienda.get(otraTienda.id());
                ProductoTiendaResumen ptOtra = datos.productos().get(productoId);
                if (ptOtra == null) {
                    continue;
                }
                BigDecimal existenciaOtra = datos.existencias().getOrDefault(productoId, BigDecimal.ZERO);
                BigDecimal umbral = ptOtra.stockMaximo().multiply(FACTOR_EXCEDENTE_TRASLADO);
                if (existenciaOtra.compareTo(umbral) > 0) {
                    sugerencias.add(new SugerenciaTrasladoResumen(
                            productoId, otraTienda.id(), existenciaOtra, existenciaOtra.subtract(ptOtra.stockMaximo())));
                    break;
                }
            }
        }
        return sugerencias;
    }

    private static <T> BigDecimal[] calcularAging(
            List<T> cuentas, java.util.function.Function<T, Instant> fechaVencimiento,
            java.util.function.Function<T, BigDecimal> saldoPendiente, Instant ahora) {
        BigDecimal bucket0a30 = BigDecimal.ZERO;
        BigDecimal bucket31a60 = BigDecimal.ZERO;
        BigDecimal bucketMas60 = BigDecimal.ZERO;
        for (T cuenta : cuentas) {
            long diasVencido = ChronoUnit.DAYS.between(fechaVencimiento.apply(cuenta), ahora);
            if (diasVencido <= 0) {
                continue;
            }
            BigDecimal saldo = saldoPendiente.apply(cuenta);
            if (diasVencido <= 30) {
                bucket0a30 = bucket0a30.add(saldo);
            } else if (diasVencido <= 60) {
                bucket31a60 = bucket31a60.add(saldo);
            } else {
                bucketMas60 = bucketMas60.add(saldo);
            }
        }
        return new BigDecimal[] {bucket0a30, bucket31a60, bucketMas60};
    }

    private static <T> BigDecimal sumar(List<T> items, java.util.function.Function<T, BigDecimal> extractor) {
        return items.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal dividir(BigDecimal dividendo, BigDecimal divisor) {
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return dividendo.divide(divisor, 2, RoundingMode.HALF_UP);
    }
}
