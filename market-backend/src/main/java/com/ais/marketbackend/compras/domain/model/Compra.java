package com.ais.marketbackend.compras.domain.model;

import com.ais.marketbackend.compras.domain.exception.CompraSinLineasException;
import com.ais.marketbackend.compras.domain.exception.EstadoCompraInvalidoException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Orden de compra a un proveedor para una tienda. Las líneas viven dentro del
 * agregado (no son su propia raíz). {@code recibir()} solo cambia el estado —
 * la mutación real de inventario (vía {@code InventarioService}, otro módulo) la
 * dispara {@code CompraServiceImpl.recibir}, no esta clase.
 */
public class Compra {

    private final Long id;
    private final Long proveedorId;
    private final Long tiendaId;
    private final Instant fecha;
    private EstadoCompra estado;
    private final List<LineaCompra> lineas;

    public Compra(
            Long id, Long proveedorId, Long tiendaId, Instant fecha, EstadoCompra estado, List<LineaCompra> lineas) {
        this.id = id;
        this.proveedorId = Objects.requireNonNull(proveedorId, "proveedorId");
        this.tiendaId = Objects.requireNonNull(tiendaId, "tiendaId");
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.estado = Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(lineas, "lineas");
        if (lineas.isEmpty()) {
            throw new CompraSinLineasException();
        }
        this.lineas = new ArrayList<>(lineas);
    }

    public static Compra nueva(Long proveedorId, Long tiendaId, List<LineaCompra> lineas) {
        return new Compra(null, proveedorId, tiendaId, Instant.now(), EstadoCompra.BORRADOR, lineas);
    }

    public void recibir() {
        exigirBorrador();
        this.estado = EstadoCompra.RECIBIDA;
    }

    public void anular() {
        exigirBorrador();
        this.estado = EstadoCompra.ANULADA;
    }

    public BigDecimal total() {
        BigDecimal total = lineas.stream().map(LineaCompra::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.setScale(4, RoundingMode.HALF_UP);
    }

    private void exigirBorrador() {
        if (estado != EstadoCompra.BORRADOR) {
            throw new EstadoCompraInvalidoException(estado);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getProveedorId() {
        return proveedorId;
    }

    public Long getTiendaId() {
        return tiendaId;
    }

    public Instant getFecha() {
        return fecha;
    }

    public EstadoCompra getEstado() {
        return estado;
    }

    public List<LineaCompra> getLineas() {
        return List.copyOf(lineas);
    }
}
