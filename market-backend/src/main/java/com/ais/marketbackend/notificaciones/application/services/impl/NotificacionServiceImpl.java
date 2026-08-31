package com.ais.marketbackend.notificaciones.application.services.impl;

import com.ais.marketbackend.cuentasporcobrar.application.services.interfaces.CuentaPorCobrarService;
import com.ais.marketbackend.cuentasporcobrar.domain.model.EstadoCuentaPorCobrar;
import com.ais.marketbackend.cuentasporpagar.application.services.interfaces.CuentaPorPagarService;
import com.ais.marketbackend.cuentasporpagar.domain.model.EstadoCuentaPorPagar;
import com.ais.marketbackend.gastosprogramados.application.services.interfaces.GastoProgramadoService;
import com.ais.marketbackend.inventario.application.dtos.InventarioResumen;
import com.ais.marketbackend.inventario.application.services.interfaces.InventarioService;
import com.ais.marketbackend.notificaciones.application.dtos.NotificacionResumen;
import com.ais.marketbackend.notificaciones.application.services.interfaces.NotificacionService;
import com.ais.marketbackend.notificaciones.domain.model.Notificacion;
import com.ais.marketbackend.notificaciones.domain.model.TipoNotificacion;
import com.ais.marketbackend.notificaciones.domain.repository.NotificacionRepository;
import com.ais.marketbackend.productos.application.dtos.ProductoTiendaResumen;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoTiendaService;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Los cuatro servicios de otros módulos son dependencias cruzadas permitidas:
 * solo se usan sus puertos {@code application.services.interfaces}, en modo
 * lectura, para detectar condiciones nuevas dignas de notificación. {@code
 * generar} es idempotente por condición — no duplica una notificación mientras
 * la anterior siga sin leerse.
 */
@Service
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final CuentaPorPagarService cuentaPorPagarService;
    private final CuentaPorCobrarService cuentaPorCobrarService;
    private final GastoProgramadoService gastoProgramadoService;
    private final ProductoTiendaService productoTiendaService;
    private final InventarioService inventarioService;

    public NotificacionServiceImpl(
            NotificacionRepository notificacionRepository, CuentaPorPagarService cuentaPorPagarService,
            CuentaPorCobrarService cuentaPorCobrarService, GastoProgramadoService gastoProgramadoService,
            ProductoTiendaService productoTiendaService, InventarioService inventarioService) {
        this.notificacionRepository = notificacionRepository;
        this.cuentaPorPagarService = cuentaPorPagarService;
        this.cuentaPorCobrarService = cuentaPorCobrarService;
        this.gastoProgramadoService = gastoProgramadoService;
        this.productoTiendaService = productoTiendaService;
        this.inventarioService = inventarioService;
    }

    @Override
    @Transactional
    public List<NotificacionResumen> generar(Long tiendaId) {
        Instant ahora = Instant.now();
        List<Notificacion> creadas = new ArrayList<>();

        cuentaPorPagarService.listarPorTienda(tiendaId).stream()
                .filter(c -> c.estado() == EstadoCuentaPorPagar.PENDIENTE && c.fechaVencimiento().isBefore(ahora))
                .forEach(c -> crearSiNueva(
                        creadas, tiendaId, TipoNotificacion.CUENTA_POR_PAGAR_VENCIDA, c.id(),
                        "La cuenta por pagar #" + c.id() + " está vencida."));

        cuentaPorCobrarService.listarPorTienda(tiendaId).stream()
                .filter(c -> c.estado() == EstadoCuentaPorCobrar.PENDIENTE && c.fechaVencimiento().isBefore(ahora))
                .forEach(c -> crearSiNueva(
                        creadas, tiendaId, TipoNotificacion.CUENTA_POR_COBRAR_VENCIDA, c.id(),
                        "La cuenta por cobrar #" + c.id() + " está vencida."));

        gastoProgramadoService.listarPorTienda(tiendaId).stream()
                .filter(g -> g.activo() && !g.proximaFecha().isAfter(ahora))
                .forEach(g -> crearSiNueva(
                        creadas, tiendaId, TipoNotificacion.GASTO_PROGRAMADO_VENCIDO, g.id(),
                        "El gasto programado \"" + g.concepto() + "\" está vencido."));

        Map<Long, BigDecimal> existenciaPorProducto = inventarioService.listarPorTienda(tiendaId).stream()
                .collect(Collectors.toMap(InventarioResumen::productoId, InventarioResumen::existenciaActual));

        productoTiendaService.listarPorTienda(tiendaId).stream()
                .filter(ProductoTiendaResumen::activo)
                .filter(pt -> existenciaPorProducto
                        .getOrDefault(pt.productoId(), BigDecimal.ZERO)
                        .compareTo(pt.stockMinimo()) < 0)
                .forEach(pt -> crearSiNueva(
                        creadas, tiendaId, TipoNotificacion.STOCK_BAJO, pt.productoId(),
                        "Stock bajo del producto #" + pt.productoId() + " (mínimo " + pt.stockMinimo() + ")."));

        return creadas.stream().map(this::toResumen).toList();
    }

    @Override
    @Transactional
    public NotificacionResumen marcarLeida(Long tiendaId, Long id) {
        Notificacion notificacion = obtenerORequerida(tiendaId, id);
        notificacion.marcarLeida();
        return toResumen(notificacionRepository.save(notificacion));
    }

    @Override
    public List<NotificacionResumen> listarPorTienda(Long tiendaId) {
        return notificacionRepository.findByTiendaId(tiendaId).stream().map(this::toResumen).toList();
    }

    @Override
    public Pagina<NotificacionResumen> listarPorTienda(Long tiendaId, int pagina, int tamano) {
        return notificacionRepository.findByTiendaId(tiendaId, pagina, tamano).map(this::toResumen);
    }

    @Override
    public List<NotificacionResumen> listarNoLeidasPorTienda(Long tiendaId) {
        return notificacionRepository.findByTiendaIdAndLeidaFalse(tiendaId).stream().map(this::toResumen).toList();
    }

    @Override
    public Pagina<NotificacionResumen> listarNoLeidasPorTienda(Long tiendaId, int pagina, int tamano) {
        return notificacionRepository.findByTiendaIdAndLeidaFalse(tiendaId, pagina, tamano).map(this::toResumen);
    }

    private void crearSiNueva(
            List<Notificacion> creadas, Long tiendaId, TipoNotificacion tipo, Long referenciaId, String mensaje) {
        if (notificacionRepository.existsByTiendaIdAndTipoAndReferenciaIdAndLeidaFalse(tiendaId, tipo, referenciaId)) {
            return;
        }
        creadas.add(notificacionRepository.save(Notificacion.nueva(tiendaId, tipo, referenciaId, mensaje)));
    }

    private Notificacion obtenerORequerida(Long tiendaId, Long id) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada: " + id));
        if (!notificacion.getTiendaId().equals(tiendaId)) {
            throw new ResourceNotFoundException("Notificación no encontrada: " + id);
        }
        return notificacion;
    }

    private NotificacionResumen toResumen(Notificacion notificacion) {
        return new NotificacionResumen(
                notificacion.getId(), notificacion.getTiendaId(), notificacion.getTipo(),
                notificacion.getReferenciaId(), notificacion.getMensaje(), notificacion.getFecha(),
                notificacion.isLeida(), notificacion.getFechaLectura());
    }
}
