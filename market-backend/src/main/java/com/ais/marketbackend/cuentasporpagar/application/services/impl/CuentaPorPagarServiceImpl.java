package com.ais.marketbackend.cuentasporpagar.application.services.impl;

import com.ais.marketbackend.auditoria.infrastructure.aop.Auditable;
import com.ais.marketbackend.caja.application.services.interfaces.CajaService;
import com.ais.marketbackend.caja.domain.model.TipoMovimientoCaja;
import com.ais.marketbackend.cuentasporpagar.application.dtos.CuentaPorPagarResumen;
import com.ais.marketbackend.cuentasporpagar.application.dtos.PagoResumen;
import com.ais.marketbackend.cuentasporpagar.application.services.interfaces.CuentaPorPagarService;
import com.ais.marketbackend.cuentasporpagar.domain.model.CuentaPorPagar;
import com.ais.marketbackend.cuentasporpagar.domain.model.Pago;
import com.ais.marketbackend.cuentasporpagar.domain.repository.CuentaPorPagarRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code cajaService} es una dependencia cruzada de módulo permitida: solo se
 * usa su puerto {@code application.services.interfaces}. Un pago registrado
 * mientras la tienda tiene una caja abierta también se refleja ahí como
 * egreso — si no hay caja abierta, el pago igual se registra.
 */
@Service
public class CuentaPorPagarServiceImpl implements CuentaPorPagarService {

    private final CuentaPorPagarRepository cuentaPorPagarRepository;
    private final CajaService cajaService;

    public CuentaPorPagarServiceImpl(CuentaPorPagarRepository cuentaPorPagarRepository, CajaService cajaService) {
        this.cuentaPorPagarRepository = cuentaPorPagarRepository;
        this.cajaService = cajaService;
    }

    @Override
    @Transactional
    public CuentaPorPagarResumen crear(Long compraId, Long proveedorId, Long tiendaId, BigDecimal montoOriginal) {
        CuentaPorPagar cuenta = CuentaPorPagar.nueva(compraId, proveedorId, tiendaId, montoOriginal);
        return toResumen(cuentaPorPagarRepository.save(cuenta));
    }

    @Override
    @Transactional
    @Auditable(accion = "CUENTA_POR_PAGAR_PAGO", entidad = "CUENTA_POR_PAGAR", tiendaIdParam = "tiendaId",
            entidadIdParam = "id")
    public CuentaPorPagarResumen registrarPago(Long tiendaId, Long id, BigDecimal monto) {
        CuentaPorPagar cuenta = obtenerConBloqueoORequerida(tiendaId, id);
        cuenta.registrarPago(monto);
        CuentaPorPagarResumen resumen = toResumen(cuentaPorPagarRepository.save(cuenta));
        cajaService.registrarMovimientoSiHayAbierta(
                tiendaId, TipoMovimientoCaja.EGRESO, "Pago cuenta por pagar #" + id, monto);
        return resumen;
    }

    @Override
    @Transactional
    @Auditable(accion = "CUENTA_POR_PAGAR_ANULADA", entidad = "CUENTA_POR_PAGAR", tiendaIdParam = "tiendaId",
            entidadIdParam = "id")
    public CuentaPorPagarResumen anular(Long tiendaId, Long id) {
        CuentaPorPagar cuenta = obtenerConBloqueoORequerida(tiendaId, id);
        cuenta.anular();
        return toResumen(cuentaPorPagarRepository.save(cuenta));
    }

    @Override
    public CuentaPorPagarResumen obtener(Long tiendaId, Long id) {
        return toResumen(obtenerORequerida(tiendaId, id));
    }

    @Override
    public List<CuentaPorPagarResumen> listarPorTienda(Long tiendaId) {
        return cuentaPorPagarRepository.findByTiendaId(tiendaId).stream().map(this::toResumen).toList();
    }

    @Override
    public Pagina<CuentaPorPagarResumen> listarPorTienda(Long tiendaId, int pagina, int tamano) {
        return cuentaPorPagarRepository.findByTiendaId(tiendaId, pagina, tamano).map(this::toResumen);
    }

    private CuentaPorPagar obtenerORequerida(Long tiendaId, Long id) {
        CuentaPorPagar cuenta = cuentaPorPagarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta por pagar no encontrada: " + id));
        if (!cuenta.getTiendaId().equals(tiendaId)) {
            throw new ResourceNotFoundException("Cuenta por pagar no encontrada: " + id);
        }
        return cuenta;
    }

    /** Igual que {@link #obtenerORequerida}, pero con {@code findByIdConBloqueo} — ver {@code CuentaPorPagarRepository}. */
    private CuentaPorPagar obtenerConBloqueoORequerida(Long tiendaId, Long id) {
        CuentaPorPagar cuenta = cuentaPorPagarRepository.findByIdConBloqueo(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta por pagar no encontrada: " + id));
        if (!cuenta.getTiendaId().equals(tiendaId)) {
            throw new ResourceNotFoundException("Cuenta por pagar no encontrada: " + id);
        }
        return cuenta;
    }

    private CuentaPorPagarResumen toResumen(CuentaPorPagar cuenta) {
        List<PagoResumen> pagos = cuenta.getPagos().stream()
                .map(p -> new PagoResumen(p.getId(), p.getFecha(), p.getMonto()))
                .toList();
        return new CuentaPorPagarResumen(
                cuenta.getId(), cuenta.getCompraId(), cuenta.getProveedorId(), cuenta.getTiendaId(),
                cuenta.getFechaEmision(), cuenta.getFechaVencimiento(), cuenta.getMontoOriginal(),
                cuenta.getSaldoPendiente(), cuenta.getEstado(), pagos);
    }
}
