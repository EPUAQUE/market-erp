package com.ais.marketbackend.cuentasporpagar.domain.model;

import com.ais.marketbackend.cuentasporpagar.domain.exception.CuentaConPagosException;
import com.ais.marketbackend.cuentasporpagar.domain.exception.EstadoCuentaPorPagarInvalidoException;
import com.ais.marketbackend.cuentasporpagar.domain.exception.PagoExcedeSaldoException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Deuda con un proveedor originada al recibir una {@code Compra} (otro módulo —
 * ver {@code CompraServiceImpl.recibir}). {@code DIAS_CREDITO_DEFAULT} es un
 * plazo fijo; este módulo no modela todavía condiciones de crédito por
 * proveedor.
 */
public class CuentaPorPagar {

    private static final int DIAS_CREDITO_DEFAULT = 30;

    private final Long id;
    private final Long compraId;
    private final Long proveedorId;
    private final Long tiendaId;
    private final Instant fechaEmision;
    private final Instant fechaVencimiento;
    private final BigDecimal montoOriginal;
    private BigDecimal saldoPendiente;
    private EstadoCuentaPorPagar estado;
    private final List<Pago> pagos;

    public CuentaPorPagar(
            Long id, Long compraId, Long proveedorId, Long tiendaId, Instant fechaEmision, Instant fechaVencimiento,
            BigDecimal montoOriginal, BigDecimal saldoPendiente, EstadoCuentaPorPagar estado, List<Pago> pagos) {
        this.id = id;
        this.compraId = Objects.requireNonNull(compraId, "compraId");
        this.proveedorId = Objects.requireNonNull(proveedorId, "proveedorId");
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
        this.pagos = new ArrayList<>(Objects.requireNonNull(pagos, "pagos"));
    }

    public static CuentaPorPagar nueva(Long compraId, Long proveedorId, Long tiendaId, BigDecimal montoOriginal) {
        Instant emision = Instant.now();
        Instant vencimiento = emision.plus(DIAS_CREDITO_DEFAULT, ChronoUnit.DAYS);
        return new CuentaPorPagar(
                null, compraId, proveedorId, tiendaId, emision, vencimiento, montoOriginal, montoOriginal,
                EstadoCuentaPorPagar.PENDIENTE, List.of());
    }

    public void registrarPago(BigDecimal monto) {
        if (estado != EstadoCuentaPorPagar.PENDIENTE) {
            throw new EstadoCuentaPorPagarInvalidoException(estado);
        }
        if (monto.compareTo(saldoPendiente) > 0) {
            throw new PagoExcedeSaldoException();
        }
        this.pagos.add(Pago.nuevo(monto));
        this.saldoPendiente = saldoPendiente.subtract(monto);
        if (saldoPendiente.signum() == 0) {
            this.estado = EstadoCuentaPorPagar.PAGADA;
        }
    }

    public void anular() {
        if (estado != EstadoCuentaPorPagar.PENDIENTE) {
            throw new EstadoCuentaPorPagarInvalidoException(estado);
        }
        if (!pagos.isEmpty()) {
            throw new CuentaConPagosException();
        }
        this.estado = EstadoCuentaPorPagar.ANULADA;
    }

    public Long getId() {
        return id;
    }

    public Long getCompraId() {
        return compraId;
    }

    public Long getProveedorId() {
        return proveedorId;
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

    public EstadoCuentaPorPagar getEstado() {
        return estado;
    }

    public List<Pago> getPagos() {
        return List.copyOf(pagos);
    }
}
