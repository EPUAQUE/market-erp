package com.ais.marketbackend.ventas.domain.model;

import com.ais.marketbackend.ventas.domain.exception.EstadoVentaInvalidoException;
import com.ais.marketbackend.ventas.domain.exception.VentaSinLineasException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Venta a un cliente en una tienda. Las líneas viven dentro del agregado (no
 * son su propia raíz). {@code completar()} solo cambia el estado — la
 * mutación real de inventario (vía {@code InventarioService}, otro módulo) la
 * dispara {@code VentaServiceImpl.completar}, no esta clase. Mismo diseño que
 * {@code Compra}.
 */
public class Venta {

    private final Long id;
    private final Long clienteId;
    private final Long tiendaId;
    private final Long vendedorId;
    private final Instant fecha;
    private EstadoVenta estado;
    private final List<LineaVenta> lineas;
    private final MetodoPago metodoPago;
    private final String correlationId;

    /**
     * {@code metodoPago} acepta {@code null} solo para reconstruir ventas
     * anteriores a este campo (nunca lo tuvieron) — {@link #nueva} sí lo
     * exige para toda venta nueva de acá en adelante. {@code correlationId}
     * es opcional siempre — solo lo manda el cliente al sincronizar una
     * venta creada offline (ver {@code VentaServiceImpl.crear} y CLAUDE.md de
     * market-flutter); una venta creada online-directo nunca tuvo uno.
     */
    public Venta(
            Long id, Long clienteId, Long tiendaId, Long vendedorId, Instant fecha, EstadoVenta estado,
            List<LineaVenta> lineas, MetodoPago metodoPago, String correlationId) {
        this.id = id;
        this.clienteId = Objects.requireNonNull(clienteId, "clienteId");
        this.tiendaId = Objects.requireNonNull(tiendaId, "tiendaId");
        this.vendedorId = Objects.requireNonNull(vendedorId, "vendedorId");
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.estado = Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(lineas, "lineas");
        if (lineas.isEmpty()) {
            throw new VentaSinLineasException();
        }
        this.lineas = new ArrayList<>(lineas);
        this.metodoPago = metodoPago;
        this.correlationId = correlationId;
    }

    public static Venta nueva(
            Long clienteId, Long tiendaId, Long vendedorId, List<LineaVenta> lineas, MetodoPago metodoPago) {
        return nueva(clienteId, tiendaId, vendedorId, lineas, metodoPago, null);
    }

    public static Venta nueva(
            Long clienteId, Long tiendaId, Long vendedorId, List<LineaVenta> lineas, MetodoPago metodoPago,
            String correlationId) {
        return new Venta(
                null, clienteId, tiendaId, vendedorId, Instant.now(), EstadoVenta.BORRADOR, lineas,
                Objects.requireNonNull(metodoPago, "metodoPago"), correlationId);
    }

    public void completar() {
        exigirBorrador();
        this.estado = EstadoVenta.COMPLETADA;
    }

    public void anular() {
        exigirBorrador();
        this.estado = EstadoVenta.ANULADA;
    }

    public BigDecimal total() {
        BigDecimal total = lineas.stream().map(LineaVenta::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.setScale(4, RoundingMode.HALF_UP);
    }

    private void exigirBorrador() {
        if (estado != EstadoVenta.BORRADOR) {
            throw new EstadoVentaInvalidoException(estado);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public Long getTiendaId() {
        return tiendaId;
    }

    public Long getVendedorId() {
        return vendedorId;
    }

    public Instant getFecha() {
        return fecha;
    }

    public EstadoVenta getEstado() {
        return estado;
    }

    public List<LineaVenta> getLineas() {
        return List.copyOf(lineas);
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
