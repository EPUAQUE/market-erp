package com.ais.marketbackend.cuentasporcobrar.domain.model;

import com.ais.marketbackend.cuentasporcobrar.domain.exception.CobroExcedeSaldoException;
import com.ais.marketbackend.cuentasporcobrar.domain.exception.CuentaConCobrosException;
import com.ais.marketbackend.cuentasporcobrar.domain.exception.EstadoCuentaPorCobrarInvalidoException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Deuda de un cliente originada al completar una {@code Venta} (otro módulo —
 * ver {@code VentaServiceImpl.completar}). {@code DIAS_CREDITO_DEFAULT} es un
 * plazo fijo; este módulo no distingue ventas de contado vs. crédito todavía —
 * mismo diseño simplificado que {@code CuentaPorPagar}.
 */
public class CuentaPorCobrar {

    private static final int DIAS_CREDITO_DEFAULT = 30;

    private final Long id;
    private final Long ventaId;
    private final Long clienteId;
    private final Long tiendaId;
    private final Instant fechaEmision;
    private final Instant fechaVencimiento;
    private final BigDecimal montoOriginal;
    private BigDecimal saldoPendiente;
    private EstadoCuentaPorCobrar estado;
    private final List<Cobro> cobros;

    public CuentaPorCobrar(
            Long id, Long ventaId, Long clienteId, Long tiendaId, Instant fechaEmision, Instant fechaVencimiento,
            BigDecimal montoOriginal, BigDecimal saldoPendiente, EstadoCuentaPorCobrar estado, List<Cobro> cobros) {
        this.id = id;
        this.ventaId = Objects.requireNonNull(ventaId, "ventaId");
        this.clienteId = Objects.requireNonNull(clienteId, "clienteId");
        this.tiendaId = Objects.requireNonNull(tiendaId, "tiendaId");
        this.fechaEmision = Objects.requireNonNull(fechaEmision, "fechaEmision");
        this.fechaVencimiento = Objects.requireNonNull(fechaVencimiento, "fechaVencimiento");
        Objects.requireNonNull(montoOriginal, "montoOriginal");
        Objects.requireNonNull(saldoPendiente, "saldoPendiente");
        if (montoOriginal.signum() <= 0) {
            throw new IllegalArgumentException("El monto original debe ser mayor que cero.");
        }
        if (saldoPendiente.signum() < 0) {
            throw new IllegalArgumentException("El saldo pendiente no puede ser negativo.");
        }
        this.montoOriginal = montoOriginal;
        this.saldoPendiente = saldoPendiente;
        this.estado = Objects.requireNonNull(estado, "estado");
        this.cobros = new ArrayList<>(Objects.requireNonNull(cobros, "cobros"));
    }

    public static CuentaPorCobrar nueva(Long ventaId, Long clienteId, Long tiendaId, BigDecimal montoOriginal) {
        Instant emision = Instant.now();
        Instant vencimiento = emision.plus(DIAS_CREDITO_DEFAULT, ChronoUnit.DAYS);
        return new CuentaPorCobrar(
                null, ventaId, clienteId, tiendaId, emision, vencimiento, montoOriginal, montoOriginal,
                EstadoCuentaPorCobrar.PENDIENTE, List.of());
    }

    public void registrarCobro(BigDecimal monto, MetodoPago metodoPago) {
        if (estado != EstadoCuentaPorCobrar.PENDIENTE) {
            throw new EstadoCuentaPorCobrarInvalidoException(estado);
        }
        if (monto.compareTo(saldoPendiente) > 0) {
            throw new CobroExcedeSaldoException();
        }
        this.cobros.add(Cobro.nuevo(monto, metodoPago));
        this.saldoPendiente = saldoPendiente.subtract(monto);
        if (saldoPendiente.signum() == 0) {
            this.estado = EstadoCuentaPorCobrar.COBRADA;
        }
    }

    public void anular() {
        if (estado != EstadoCuentaPorCobrar.PENDIENTE) {
            throw new EstadoCuentaPorCobrarInvalidoException(estado);
        }
        if (!cobros.isEmpty()) {
            throw new CuentaConCobrosException();
        }
        this.estado = EstadoCuentaPorCobrar.ANULADA;
    }

    public Long getId() {
        return id;
    }

    public Long getVentaId() {
        return ventaId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public Long getTiendaId() {
        return tiendaId;
    }

    public Instant getFechaEmision() {
        return fechaEmision;
    }

    public Instant getFechaVencimiento() {
        return fechaVencimiento;
    }

    public BigDecimal getMontoOriginal() {
        return montoOriginal;
    }

    public BigDecimal getSaldoPendiente() {
        return saldoPendiente;
    }

    public EstadoCuentaPorCobrar getEstado() {
        return estado;
    }

    public List<Cobro> getCobros() {
        return List.copyOf(cobros);
    }
}
