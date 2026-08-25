package com.ais.marketbackend.traslados.domain.model;

import com.ais.marketbackend.traslados.domain.exception.EstadoTrasladoInvalidoException;
import com.ais.marketbackend.traslados.domain.exception.TrasladoMismaTiendaException;
import com.ais.marketbackend.traslados.domain.exception.TrasladoSinLineasException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Movimiento de inventario entre dos tiendas. Las líneas viven dentro del
 * agregado (no son su propia raíz). {@code completar()} solo cambia el estado
 * — la mutación real de inventario en ambas tiendas (vía
 * {@code InventarioService}, otro módulo) la dispara
 * {@code TrasladoServiceImpl.completar}, no esta clase. Mismo diseño que
 * {@code Compra}/{@code Venta}.
 */
public class Traslado {

    private final Long id;
    private final Long tiendaOrigenId;
    private final Long tiendaDestinoId;
    private final Instant fecha;
    private EstadoTraslado estado;
    private final List<LineaTraslado> lineas;

    public Traslado(
            Long id, Long tiendaOrigenId, Long tiendaDestinoId, Instant fecha, EstadoTraslado estado,
            List<LineaTraslado> lineas) {
        this.id = id;
        this.tiendaOrigenId = Objects.requireNonNull(tiendaOrigenId, "tiendaOrigenId");
        this.tiendaDestinoId = Objects.requireNonNull(tiendaDestinoId, "tiendaDestinoId");
        if (tiendaOrigenId.equals(tiendaDestinoId)) {
            throw new TrasladoMismaTiendaException();
        }
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.estado = Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(lineas, "lineas");
        if (lineas.isEmpty()) {
            throw new TrasladoSinLineasException();
        }
        this.lineas = new ArrayList<>(lineas);
    }

    public static Traslado nuevo(Long tiendaOrigenId, Long tiendaDestinoId, List<LineaTraslado> lineas) {
        return new Traslado(null, tiendaOrigenId, tiendaDestinoId, Instant.now(), EstadoTraslado.BORRADOR, lineas);
    }

    public void completar() {
        exigirBorrador();
        this.estado = EstadoTraslado.COMPLETADO;
    }

    public void anular() {
        exigirBorrador();
        this.estado = EstadoTraslado.ANULADO;
    }

    private void exigirBorrador() {
        if (estado != EstadoTraslado.BORRADOR) {
            throw new EstadoTrasladoInvalidoException(estado);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getTiendaOrigenId() {
        return tiendaOrigenId;
    }

    public Long getTiendaDestinoId() {
        return tiendaDestinoId;
    }

    public Instant getFecha() {
        return fecha;
    }

    public EstadoTraslado getEstado() {
        return estado;
    }

    public List<LineaTraslado> getLineas() {
        return List.copyOf(lineas);
    }
}
