package com.ais.marketbackend.cuentasporcobrar.application.services.impl;

import com.ais.marketbackend.caja.application.services.interfaces.CajaService;
import com.ais.marketbackend.caja.domain.model.TipoMovimientoCaja;
import com.ais.marketbackend.cuentasporcobrar.application.dtos.CobroResumen;
import com.ais.marketbackend.cuentasporcobrar.application.dtos.CuentaPorCobrarResumen;
import com.ais.marketbackend.cuentasporcobrar.application.services.interfaces.CuentaPorCobrarService;
import com.ais.marketbackend.cuentasporcobrar.domain.model.CuentaPorCobrar;
import com.ais.marketbackend.cuentasporcobrar.domain.model.MetodoPago;
import com.ais.marketbackend.cuentasporcobrar.domain.repository.CuentaPorCobrarRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code cajaService} es una dependencia cruzada de módulo permitida: solo se
 * usa su puerto {@code application.services.interfaces}. Un cobro registrado
 * mientras la tienda tiene una caja abierta también se refleja ahí como
 * ingreso — si no hay caja abierta, el cobro igual se registra (la tienda
 * podría no operar caja diaria todavía).
 */
@Service
public class CuentaPorCobrarServiceImpl implements CuentaPorCobrarService {

    private final CuentaPorCobrarRepository cuentaPorCobrarRepository;
    private final CajaService cajaService;

    public CuentaPorCobrarServiceImpl(CuentaPorCobrarRepository cuentaPorCobrarRepository, CajaService cajaService) {
        this.cuentaPorCobrarRepository = cuentaPorCobrarRepository;
        this.cajaService = cajaService;
    }

    @Override
    @Transactional
    public CuentaPorCobrarResumen crear(Long ventaId, Long clienteId, Long tiendaId, BigDecimal montoOriginal) {
        CuentaPorCobrar cuenta = CuentaPorCobrar.nueva(ventaId, clienteId, tiendaId, montoOriginal);
        return toResumen(cuentaPorCobrarRepository.save(cuenta));
    }

    @Override
    @Transactional
    public CuentaPorCobrarResumen registrarCobro(Long tiendaId, Long id, BigDecimal monto, MetodoPago metodoPago) {
        CuentaPorCobrar cuenta = obtenerORequerida(tiendaId, id);
        cuenta.registrarCobro(monto, metodoPago);
        CuentaPorCobrarResumen resumen = toResumen(cuentaPorCobrarRepository.save(cuenta));
        cajaService.registrarMovimientoSiHayAbierta(
                tiendaId, TipoMovimientoCaja.INGRESO, "Cobro cuenta por cobrar #" + id, monto);
        return resumen;
    }

    @Override
    @Transactional
    public CuentaPorCobrarResumen anular(Long tiendaId, Long id) {
        CuentaPorCobrar cuenta = obtenerORequerida(tiendaId, id);
        cuenta.anular();
        return toResumen(cuentaPorCobrarRepository.save(cuenta));
    }

    @Override
    public CuentaPorCobrarResumen obtener(Long tiendaId, Long id) {
        return toResumen(obtenerORequerida(tiendaId, id));
    }

    @Override
    public List<CuentaPorCobrarResumen> listarPorTienda(Long tiendaId) {
        return cuentaPorCobrarRepository.findByTiendaId(tiendaId).stream().map(this::toResumen).toList();
    }

    @Override
    public Pagina<CuentaPorCobrarResumen> listarPorTienda(Long tiendaId, int pagina, int tamano) {
        return cuentaPorCobrarRepository.findByTiendaId(tiendaId, pagina, tamano).map(this::toResumen);
    }

    private CuentaPorCobrar obtenerORequerida(Long tiendaId, Long id) {
        CuentaPorCobrar cuenta = cuentaPorCobrarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta por cobrar no encontrada: " + id));
        if (!cuenta.getTiendaId().equals(tiendaId)) {
            throw new ResourceNotFoundException("Cuenta por cobrar no encontrada: " + id);
        }
        return cuenta;
    }

    private CuentaPorCobrarResumen toResumen(CuentaPorCobrar cuenta) {
        List<CobroResumen> cobros = cuenta.getCobros().stream()
                .map(c -> new CobroResumen(c.getId(), c.getFecha(), c.getMonto(), c.getMetodoPago()))
                .toList();
        return new CuentaPorCobrarResumen(
                cuenta.getId(), cuenta.getVentaId(), cuenta.getClienteId(), cuenta.getTiendaId(),
                cuenta.getFechaEmision(), cuenta.getFechaVencimiento(), cuenta.getMontoOriginal(),
                cuenta.getSaldoPendiente(), cuenta.getEstado(), cobros);
    }
}
