package com.ais.marketbackend.caja.domain.model;

import com.ais.marketbackend.caja.domain.exception.EstadoCajaSesionInvalidoException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Turno de caja de una tienda: se abre con un monto inicial, acumula
 * movimientos de efectivo (ingresos/egresos) y se cierra con el monto contado
 * físicamente. Solo puede haber una sesión ABIERTA por tienda a la vez — esa
 * regla la aplica {@code CajaServiceImpl.abrir}, no esta clase.
 */
public class CajaSesion {

    private final Long id;
    private final Long tiendaId;
    private final Instant fechaApertura;
    private Instant fechaCierre;
    private final BigDecimal montoInicial;
    private BigDecimal montoFinalContado;
    private EstadoCajaSesion estado;
    private final List<MovimientoCaja> movimientos;
    private final String correlationIdApertura;
    private String correlationIdCierre;

    public CajaSesion(
            Long id, Long tiendaId, Instant fechaApertura, Instant fechaCierre, BigDecimal montoInicial,
            BigDecimal montoFinalContado, EstadoCajaSesion estado, List<MovimientoCaja> movimientos,
            String correlationIdApertura, String correlationIdCierre) {
        this.id = id;
        this.tiendaId = Objects.requireNonNull(tiendaId, "tiendaId");
        this.fechaApertura = Objects.requireNonNull(fechaApertura, "fechaApertura");
        this.fechaCierre = fechaCierre;
        Objects.requireNonNull(montoInicial, "montoInicial");
        if (montoInicial.signum() < 0) {
            throw new IllegalArgumentException("El monto inicial no puede ser negativo.");
        }
        this.montoInicial = montoInicial;
        this.montoFinalContado = montoFinalContado;
        this.estado = Objects.requireNonNull(estado, "estado");
        this.movimientos = new ArrayList<>(Objects.requireNonNull(movimientos, "movimientos"));
        this.correlationIdApertura = correlationIdApertura;
        this.correlationIdCierre = correlationIdCierre;
    }

    public static CajaSesion nueva(Long tiendaId, BigDecimal montoInicial) {
        return nueva(tiendaId, montoInicial, null);
    }

    /** {@code correlationIdApertura} identifica esta intención de apertura para idempotencia — ver {@code CajaServiceImpl}. */
    public static CajaSesion nueva(Long tiendaId, BigDecimal montoInicial, String correlationIdApertura) {
        return new CajaSesion(
                null, tiendaId, Instant.now(), null, montoInicial, null, EstadoCajaSesion.ABIERTA, List.of(),
                correlationIdApertura, null);
    }

    public void registrarMovimiento(TipoMovimientoCaja tipo, String concepto, BigDecimal monto) {
        registrarMovimiento(tipo, concepto, monto, null);
    }

    public void registrarMovimiento(TipoMovimientoCaja tipo, String concepto, BigDecimal monto, String correlationId) {
        exigirAbierta();
        this.movimientos.add(MovimientoCaja.nuevo(tipo, concepto, monto, correlationId));
    }

    /** Busca dentro de esta sesión un movimiento ya registrado con esa clave de idempotencia. */
    public Optional<MovimientoCaja> movimientoPorCorrelationId(String correlationId) {
        return movimientos.stream().filter(m -> Objects.equals(m.getCorrelationId(), correlationId)).findFirst();
    }

    public void cerrar(BigDecimal montoFinalContado) {
        cerrar(montoFinalContado, null);
    }

    public void cerrar(BigDecimal montoFinalContado, String correlationIdCierre) {
        exigirAbierta();
        Objects.requireNonNull(montoFinalContado, "montoFinalContado");
        if (montoFinalContado.signum() < 0) {
            throw new IllegalArgumentException("El monto contado no puede ser negativo.");
        }
        this.montoFinalContado = montoFinalContado;
        this.fechaCierre = Instant.now();
        this.estado = EstadoCajaSesion.CERRADA;
        this.correlationIdCierre = correlationIdCierre;
    }

    public BigDecimal saldoEsperado() {
        BigDecimal ingresos = sumar(TipoMovimientoCaja.INGRESO);
        BigDecimal egresos = sumar(TipoMovimientoCaja.EGRESO);
        return montoInicial.add(ingresos).subtract(egresos);
    }

    private BigDecimal sumar(TipoMovimientoCaja tipo) {
        return movimientos.stream()
                .filter(m -> m.getTipo() == tipo)
                .map(MovimientoCaja::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void exigirAbierta() {
        if (estado != EstadoCajaSesion.ABIERTA) {
            throw new EstadoCajaSesionInvalidoException(estado);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getTiendaId() {
        return tiendaId;
    }

    public Instant getFechaApertura() {
        return fechaApertura;
    }

    public Instant getFechaCierre() {
        return fechaCierre;
    }

    public BigDecimal getMontoInicial() {
        return montoInicial;
    }

    public BigDecimal getMontoFinalContado() {
        return montoFinalContado;
    }

    public EstadoCajaSesion getEstado() {
        return estado;
    }

    public List<MovimientoCaja> getMovimientos() {
        return List.copyOf(movimientos);
    }

    public String getCorrelationIdApertura() {
        return correlationIdApertura;
    }

    public String getCorrelationIdCierre() {
        return correlationIdCierre;
    }
}
