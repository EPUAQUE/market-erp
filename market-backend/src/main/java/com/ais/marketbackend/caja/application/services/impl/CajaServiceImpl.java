package com.ais.marketbackend.caja.application.services.impl;

import com.ais.marketbackend.caja.application.dtos.CajaSesionResumen;
import com.ais.marketbackend.caja.application.dtos.MovimientoCajaResumen;
import com.ais.marketbackend.caja.application.services.interfaces.CajaService;
import com.ais.marketbackend.caja.domain.exception.CajaSesionAbiertaException;
import com.ais.marketbackend.caja.domain.model.CajaSesion;
import com.ais.marketbackend.caja.domain.model.TipoMovimientoCaja;
import com.ais.marketbackend.caja.domain.repository.CajaSesionRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CajaServiceImpl implements CajaService {

    private final CajaSesionRepository cajaSesionRepository;

    public CajaServiceImpl(CajaSesionRepository cajaSesionRepository) {
        this.cajaSesionRepository = cajaSesionRepository;
    }

    @Override
    @Transactional
    public CajaSesionResumen abrir(Long tiendaId, BigDecimal montoInicial) {
        if (cajaSesionRepository.findAbiertaByTiendaId(tiendaId).isPresent()) {
            throw new CajaSesionAbiertaException(tiendaId);
        }
        CajaSesion sesion = CajaSesion.nueva(tiendaId, montoInicial);
        return toResumen(cajaSesionRepository.save(sesion));
    }

    @Override
    @Transactional
    public CajaSesionResumen registrarMovimiento(
            Long tiendaId, TipoMovimientoCaja tipo, String concepto, BigDecimal monto) {
        CajaSesion sesion = obtenerAbiertaORequerida(tiendaId);
        sesion.registrarMovimiento(tipo, concepto, monto);
        return toResumen(cajaSesionRepository.save(sesion));
    }

    @Override
    @Transactional
    public CajaSesionResumen cerrar(Long tiendaId, BigDecimal montoFinalContado) {
        CajaSesion sesion = obtenerAbiertaORequerida(tiendaId);
        sesion.cerrar(montoFinalContado);
        return toResumen(cajaSesionRepository.save(sesion));
    }

    @Override
    public CajaSesionResumen obtenerAbierta(Long tiendaId) {
        return toResumen(obtenerAbiertaORequerida(tiendaId));
    }

    @Override
    public CajaSesionResumen obtener(Long tiendaId, Long id) {
        CajaSesion sesion = cajaSesionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada: " + id));
        if (!sesion.getTiendaId().equals(tiendaId)) {
            throw new ResourceNotFoundException("Caja no encontrada: " + id);
        }
        return toResumen(sesion);
    }

    @Override
    public List<CajaSesionResumen> listarPorTienda(Long tiendaId) {
        return cajaSesionRepository.findByTiendaId(tiendaId).stream().map(this::toResumen).toList();
    }

    @Override
    public Pagina<CajaSesionResumen> listarPorTienda(Long tiendaId, int pagina, int tamano) {
        return cajaSesionRepository.findByTiendaId(tiendaId, pagina, tamano).map(this::toResumen);
    }

    @Override
    @Transactional
    public Optional<CajaSesionResumen> registrarMovimientoSiHayAbierta(
            Long tiendaId, TipoMovimientoCaja tipo, String concepto, BigDecimal monto) {
        return cajaSesionRepository.findAbiertaByTiendaId(tiendaId).map(sesion -> {
            sesion.registrarMovimiento(tipo, concepto, monto);
            return toResumen(cajaSesionRepository.save(sesion));
        });
    }

    @Override
    public boolean hayAbiertaPorTienda(Long tiendaId) {
        return cajaSesionRepository.findAbiertaByTiendaId(tiendaId).isPresent();
    }

    private CajaSesion obtenerAbiertaORequerida(Long tiendaId) {
        return cajaSesionRepository.findAbiertaByTiendaId(tiendaId)
                .orElseThrow(() -> new ResourceNotFoundException("No hay una caja abierta para la tienda " + tiendaId + "."));
    }

    private CajaSesionResumen toResumen(CajaSesion sesion) {
        List<MovimientoCajaResumen> movimientos = sesion.getMovimientos().stream()
                .map(m -> new MovimientoCajaResumen(m.getId(), m.getFecha(), m.getTipo(), m.getConcepto(), m.getMonto()))
                .toList();
        return new CajaSesionResumen(
                sesion.getId(), sesion.getTiendaId(), sesion.getFechaApertura(), sesion.getFechaCierre(),
                sesion.getMontoInicial(), sesion.getMontoFinalContado(), sesion.saldoEsperado(), sesion.getEstado(),
                movimientos);
    }
}
