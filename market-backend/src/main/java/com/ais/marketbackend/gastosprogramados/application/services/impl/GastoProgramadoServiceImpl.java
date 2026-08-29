package com.ais.marketbackend.gastosprogramados.application.services.impl;

import com.ais.marketbackend.caja.application.services.interfaces.CajaService;
import com.ais.marketbackend.caja.domain.model.TipoMovimientoCaja;
import com.ais.marketbackend.gastosprogramados.application.dtos.GastoProgramadoResumen;
import com.ais.marketbackend.gastosprogramados.application.dtos.PagoGastoResumen;
import com.ais.marketbackend.gastosprogramados.application.services.interfaces.GastoProgramadoService;
import com.ais.marketbackend.gastosprogramados.domain.model.FrecuenciaGasto;
import com.ais.marketbackend.gastosprogramados.domain.model.GastoProgramado;
import com.ais.marketbackend.gastosprogramados.domain.repository.GastoProgramadoRepository;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code cajaService} es una dependencia cruzada de módulo permitida: solo se
 * usa su puerto {@code application.services.interfaces}. Un pago generado
 * mientras la tienda tiene una caja abierta también se refleja ahí como
 * egreso — si no hay caja abierta, el pago igual se registra.
 */
@Service
public class GastoProgramadoServiceImpl implements GastoProgramadoService {

    private final GastoProgramadoRepository gastoProgramadoRepository;
    private final CajaService cajaService;

    public GastoProgramadoServiceImpl(GastoProgramadoRepository gastoProgramadoRepository, CajaService cajaService) {
        this.gastoProgramadoRepository = gastoProgramadoRepository;
        this.cajaService = cajaService;
    }

    @Override
    @Transactional
    public GastoProgramadoResumen crear(
            Long tiendaId, String concepto, BigDecimal monto, FrecuenciaGasto frecuencia, Instant fechaInicio) {
        GastoProgramado gasto = GastoProgramado.nuevo(tiendaId, concepto, monto, frecuencia, fechaInicio);
        return toResumen(gastoProgramadoRepository.save(gasto));
    }

    @Override
    @Transactional
    public GastoProgramadoResumen actualizar(
            Long tiendaId, Long id, String concepto, BigDecimal monto, FrecuenciaGasto frecuencia) {
        GastoProgramado gasto = obtenerORequerido(tiendaId, id);
        gasto.actualizar(concepto, monto, frecuencia);
        return toResumen(gastoProgramadoRepository.save(gasto));
    }

    @Override
    @Transactional
    public GastoProgramadoResumen activar(Long tiendaId, Long id) {
        GastoProgramado gasto = obtenerORequerido(tiendaId, id);
        gasto.activar();
        return toResumen(gastoProgramadoRepository.save(gasto));
    }

    @Override
    @Transactional
    public GastoProgramadoResumen desactivar(Long tiendaId, Long id) {
        GastoProgramado gasto = obtenerORequerido(tiendaId, id);
        gasto.desactivar();
        return toResumen(gastoProgramadoRepository.save(gasto));
    }

    @Override
    @Transactional
    public GastoProgramadoResumen generarPago(Long tiendaId, Long id) {
        GastoProgramado gasto = obtenerConBloqueoORequerido(tiendaId, id);
        BigDecimal monto = gasto.getMonto();
        String concepto = gasto.getConcepto();
        gasto.generarPago(Instant.now());
        GastoProgramadoResumen resumen = toResumen(gastoProgramadoRepository.save(gasto));
        cajaService.registrarMovimientoSiHayAbierta(
                tiendaId, TipoMovimientoCaja.EGRESO, "Gasto programado: " + concepto, monto);
        return resumen;
    }

    @Override
    public GastoProgramadoResumen obtener(Long tiendaId, Long id) {
        return toResumen(obtenerORequerido(tiendaId, id));
    }

    @Override
    public List<GastoProgramadoResumen> listarPorTienda(Long tiendaId) {
        return gastoProgramadoRepository.findByTiendaId(tiendaId).stream().map(this::toResumen).toList();
    }

    private GastoProgramado obtenerORequerido(Long tiendaId, Long id) {
        GastoProgramado gasto = gastoProgramadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto programado no encontrado: " + id));
        if (!gasto.getTiendaId().equals(tiendaId)) {
            throw new ResourceNotFoundException("Gasto programado no encontrado: " + id);
        }
        return gasto;
    }

    /** Igual que {@link #obtenerORequerido}, pero con {@code findByIdConBloqueo} — ver {@code GastoProgramadoRepository}. */
    private GastoProgramado obtenerConBloqueoORequerido(Long tiendaId, Long id) {
        GastoProgramado gasto = gastoProgramadoRepository.findByIdConBloqueo(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gasto programado no encontrado: " + id));
        if (!gasto.getTiendaId().equals(tiendaId)) {
            throw new ResourceNotFoundException("Gasto programado no encontrado: " + id);
        }
        return gasto;
    }

    private GastoProgramadoResumen toResumen(GastoProgramado gasto) {
        List<PagoGastoResumen> pagos = gasto.getPagos().stream()
                .map(p -> new PagoGastoResumen(p.getId(), p.getFecha(), p.getMonto()))
                .toList();
        return new GastoProgramadoResumen(
                gasto.getId(), gasto.getTiendaId(), gasto.getConcepto(), gasto.getMonto(), gasto.getFrecuencia(),
                gasto.getProximaFecha(), gasto.isActivo(), pagos);
    }
}
