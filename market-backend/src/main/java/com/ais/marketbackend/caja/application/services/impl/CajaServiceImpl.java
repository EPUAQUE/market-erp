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
 * {@code abrir} es deliberadamente SIN {@code @Transactional} propio cuando recibe
 * {@code correlationId} — mismo motivo que {@code VentaServiceImpl.crear}: tras una
 * colisión de restricción única (aquí, o bien el correlationId de apertura, o la
 * nueva restricción parcial "una sola caja abierta por tienda"), la sesión de
 * Hibernate que acaba de fallar el flush queda inutilizable para releer en la misma
 * transacción — cada llamada al repositorio ya es transaccional por sí sola vía
 * Spring Data, así que la relectura ocurre en una transacción nueva.
 *
 * <p>{@code registrarMovimiento}/{@code cerrar}/{@code registrarMovimientoSiHayAbierta}
 * SÍ son {@code @Transactional}: leen la sesión abierta con
 * {@code findAbiertaByTiendaIdConBloqueo} ({@code PESSIMISTIC_WRITE}) y mutan/guardan
 * dentro de la misma transacción, manteniendo el lock hasta el commit. Sin esto, dos
 * movimientos concurrentes sobre la misma sesión podían perderse entre sí (la
 * colección JPA de movimientos usa {@code orphanRemoval}, que en un merge concurrente
 * sin lock puede borrar como "huérfano" un movimiento insertado por la otra
 * transacción), y dos cierres concurrentes podían pisarse el monto contado sin
 * ningún error. Con el lock, la segunda solicitud espera a que la primera termine de
 * commitear y entonces relee el estado ya actualizado — lo que además hace que una
 * colisión de correlationId por concurrencia real ya no pueda ocurrir (la segunda
 * solicitud ve el movimiento/cierre de la primera en su propia relectura antes de
 * intentar guardar), así que estos métodos no necesitan el patrón de
 * detectar-colisión-y-releer.
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
            // Dos aperturas concurrentes SIN correlationId compartido (o con
            // correlationId distinto) para la misma tienda: el chequeo de arriba no
            // alcanzó a verla, pero la restricción única parcial
            // ux_caja_sesion_abierta_por_tienda sí — la que pierde la carrera choca
            // ahí, no por una referencia inválida real.
            if (cajaSesionRepository.findAbiertaByTiendaId(tiendaId).isPresent()) {
                throw new CajaSesionAbiertaException(tiendaId);
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
    @Transactional
    public CajaSesionResumen registrarMovimiento(
            Long tiendaId, TipoMovimientoCaja tipo, String concepto, BigDecimal monto) {
        // @Transactional propio (no solo delegar): esta sobrecarga es una llamada
        // externa a través del proxy de Spring — sin su propia anotación, la
        // sobrecarga de 5 argumentos que invoca abajo es una auto-invocación que NO
        // pasa por el proxy, así que su @Transactional no tendría efecto y
        // findAbiertaByTiendaIdConBloqueo fallaría con "No active transaction".
        return registrarMovimiento(tiendaId, tipo, concepto, monto, null);
    }

    @Override
    @Transactional
    public CajaSesionResumen registrarMovimiento(
            Long tiendaId, TipoMovimientoCaja tipo, String concepto, BigDecimal monto, String correlationId) {
        String correlationIdNormalizado = normalizarCorrelationId(correlationId);
        CajaSesion sesion = obtenerAbiertaConBloqueoORequerida(tiendaId);
        if (correlationIdNormalizado != null) {
            Optional<MovimientoCaja> existente = sesion.movimientoPorCorrelationId(correlationIdNormalizado);
            if (existente.isPresent()) {
                return resolverMovimientoIdempotente(sesion, existente.get(), tipo, concepto, monto);
            }
        }
        sesion.registrarMovimiento(tipo, concepto, monto, correlationIdNormalizado);
        return toResumen(cajaSesionRepository.save(sesion));
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
    @Transactional
    public CajaSesionResumen cerrar(Long tiendaId, BigDecimal montoFinalContado) {
        // Mismo motivo que la sobrecarga de 4 argumentos de registrarMovimiento.
        return cerrar(tiendaId, montoFinalContado, null);
    }

    @Override
    @Transactional
    public CajaSesionResumen cerrar(Long tiendaId, BigDecimal montoFinalContado, String correlationId) {
        String correlationIdNormalizado = normalizarCorrelationId(correlationId);
        Optional<CajaSesion> abierta = cajaSesionRepository.findAbiertaByTiendaIdConBloqueo(tiendaId);
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
        return toResumen(cajaSesionRepository.save(sesion));
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
        return cajaSesionRepository.findAbiertaByTiendaIdConBloqueo(tiendaId).map(sesion -> {
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

    private CajaSesion obtenerAbiertaConBloqueoORequerida(Long tiendaId) {
        return cajaSesionRepository.findAbiertaByTiendaIdConBloqueo(tiendaId)
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
