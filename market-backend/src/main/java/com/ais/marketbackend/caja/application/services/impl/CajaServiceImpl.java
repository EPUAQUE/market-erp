package com.ais.marketbackend.caja.application.services.impl;

import com.ais.marketbackend.caja.application.dtos.CajaSesionResumen;
import com.ais.marketbackend.caja.application.dtos.MovimientoCajaResumen;
import com.ais.marketbackend.caja.application.services.interfaces.CajaService;
import com.ais.marketbackend.caja.domain.exception.CajaSesionAbiertaException;
import com.ais.marketbackend.caja.domain.exception.CorrelationIdReutilizadoException;
import com.ais.marketbackend.caja.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.caja.domain.model.CajaSesion;
import com.ais.marketbackend.caja.domain.model.MovimientoCaja;
import com.ais.marketbackend.caja.domain.model.TipoMovimientoCaja;
import com.ais.marketbackend.caja.domain.repository.CajaSesionRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code abrir}/{@code registrarMovimiento}/{@code cerrar} son deliberadamente SIN
 * {@code @Transactional} propio cuando reciben {@code correlationId} — mismo motivo
 * que {@code VentaServiceImpl.crear}: tras una colisión de restricción única, la
 * sesión de Hibernate que acaba de fallar el flush queda inutilizable para releer en
 * la misma transacción. Cada llamada al repositorio ya es transaccional por sí sola
 * vía Spring Data.
 */
@Service
public class CajaServiceImpl implements CajaService {

    private final CajaSesionRepository cajaSesionRepository;

    public CajaServiceImpl(CajaSesionRepository cajaSesionRepository) {
        this.cajaSesionRepository = cajaSesionRepository;
    }

    @Override
    public CajaSesionResumen abrir(Long tiendaId, BigDecimal montoInicial) {
        return abrir(tiendaId, montoInicial, null);
    }

    @Override
    public CajaSesionResumen abrir(Long tiendaId, BigDecimal montoInicial, String correlationId) {
        String correlationIdNormalizado = normalizarCorrelationId(correlationId);
        if (correlationIdNormalizado != null) {
            Optional<CajaSesion> existente =
                    cajaSesionRepository.findByTiendaIdAndCorrelationIdApertura(tiendaId, correlationIdNormalizado);
            if (existente.isPresent()) {
                return resolverAperturaIdempotente(existente.get(), montoInicial);
            }
        }
        if (cajaSesionRepository.findAbiertaByTiendaId(tiendaId).isPresent()) {
            throw new CajaSesionAbiertaException(tiendaId);
        }
        CajaSesion sesion = CajaSesion.nueva(tiendaId, montoInicial, correlationIdNormalizado);
        try {
            return toResumen(cajaSesionRepository.save(sesion));
        } catch (ReferenciaInvalidaException e) {
            // Dos aperturas concurrentes con el mismo correlationId: la que pierde la
            // carrera choca contra la restricción única — releer antes de decidir si
            // era la carrera esperada (reintento idempotente) o una referencia
            // realmente inválida, igual que VentaServiceImpl.crear.
            if (correlationIdNormalizado != null) {
                Optional<CajaSesion> existente = cajaSesionRepository
                        .findByTiendaIdAndCorrelationIdApertura(tiendaId, correlationIdNormalizado);
                if (existente.isPresent()) {
                    return resolverAperturaIdempotente(existente.get(), montoInicial);
                }
            }
            throw e;
        }
    }

    private CajaSesionResumen resolverAperturaIdempotente(CajaSesion existente, BigDecimal montoInicial) {
        if (existente.getMontoInicial().compareTo(montoInicial) != 0) {
            throw new CorrelationIdReutilizadoException(existente.getCorrelationIdApertura());
        }
        return toResumen(existente);
    }

    @Override
    public CajaSesionResumen registrarMovimiento(
            Long tiendaId, TipoMovimientoCaja tipo, String concepto, BigDecimal monto) {
        return registrarMovimiento(tiendaId, tipo, concepto, monto, null);
    }

    @Override
    public CajaSesionResumen registrarMovimiento(
            Long tiendaId, TipoMovimientoCaja tipo, String concepto, BigDecimal monto, String correlationId) {
        String correlationIdNormalizado = normalizarCorrelationId(correlationId);
        CajaSesion sesion = obtenerAbiertaORequerida(tiendaId);
        if (correlationIdNormalizado != null) {
            Optional<MovimientoCaja> existente = sesion.movimientoPorCorrelationId(correlationIdNormalizado);
            if (existente.isPresent()) {
                return resolverMovimientoIdempotente(sesion, existente.get(), tipo, concepto, monto);
            }
        }
        sesion.registrarMovimiento(tipo, concepto, monto, correlationIdNormalizado);
        try {
            return toResumen(cajaSesionRepository.save(sesion));
        } catch (ReferenciaInvalidaException e) {
            if (correlationIdNormalizado != null) {
                CajaSesion recargada = obtenerAbiertaORequerida(tiendaId);
                Optional<MovimientoCaja> existente = recargada.movimientoPorCorrelationId(correlationIdNormalizado);
                if (existente.isPresent()) {
                    return resolverMovimientoIdempotente(recargada, existente.get(), tipo, concepto, monto);
                }
            }
            throw e;
        }
    }

    private CajaSesionResumen resolverMovimientoIdempotente(
            CajaSesion sesion, MovimientoCaja existente, TipoMovimientoCaja tipo, String concepto, BigDecimal monto) {
        boolean coincide = existente.getTipo() == tipo && Objects.equals(existente.getConcepto(), concepto)
                && existente.getMonto().compareTo(monto) == 0;
        if (!coincide) {
            throw new CorrelationIdReutilizadoException(existente.getCorrelationId());
        }
        return toResumen(sesion);
    }

    @Override
    public CajaSesionResumen cerrar(Long tiendaId, BigDecimal montoFinalContado) {
        return cerrar(tiendaId, montoFinalContado, null);
    }

    @Override
    public CajaSesionResumen cerrar(Long tiendaId, BigDecimal montoFinalContado, String correlationId) {
        String correlationIdNormalizado = normalizarCorrelationId(correlationId);
        Optional<CajaSesion> abierta = cajaSesionRepository.findAbiertaByTiendaId(tiendaId);
        if (abierta.isEmpty() && correlationIdNormalizado != null) {
            Optional<CajaSesion> existente =
                    cajaSesionRepository.findByTiendaIdAndCorrelationIdCierre(tiendaId, correlationIdNormalizado);
            if (existente.isPresent()) {
                return resolverCierreIdempotente(existente.get(), montoFinalContado);
            }
        }
        CajaSesion sesion = abierta.orElseThrow(
                () -> new ResourceNotFoundException("No hay una caja abierta para la tienda " + tiendaId + "."));
        sesion.cerrar(montoFinalContado, correlationIdNormalizado);
        try {
            return toResumen(cajaSesionRepository.save(sesion));
        } catch (ReferenciaInvalidaException e) {
            if (correlationIdNormalizado != null) {
                Optional<CajaSesion> existente = cajaSesionRepository
                        .findByTiendaIdAndCorrelationIdCierre(tiendaId, correlationIdNormalizado);
                if (existente.isPresent()) {
                    return resolverCierreIdempotente(existente.get(), montoFinalContado);
                }
            }
            throw e;
        }
    }

    private CajaSesionResumen resolverCierreIdempotente(CajaSesion existente, BigDecimal montoFinalContado) {
        if (existente.getMontoFinalContado().compareTo(montoFinalContado) != 0) {
            throw new CorrelationIdReutilizadoException(existente.getCorrelationIdCierre());
        }
        return toResumen(existente);
    }

    /** {@code null}/en blanco se tratan igual — sin correlationId, sin idempotencia. */
    private String normalizarCorrelationId(String correlationId) {
        if (correlationId == null) {
            return null;
        }
        String recortado = correlationId.trim();
        return recortado.isEmpty() ? null : recortado;
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
