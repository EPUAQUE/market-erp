package com.ais.marketbackend.ventas.infrastructure.persistence.entities;

import com.ais.marketbackend.ventas.domain.model.EstadoVenta;
import com.ais.marketbackend.ventas.domain.model.MetodoPago;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code clienteId}/{@code tiendaId} son columnas planas (no {@code @ManyToOne}):
 * esas tablas son propiedad de otros módulos. {@code lineas} sí usa relación
 * JPA porque es intra-módulo (parte del mismo agregado).
 */
@Entity
@Table(name = "venta")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VentaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "tienda_id", nullable = false)
    private Long tiendaId;

    @Column(name = "vendedor_id", nullable = false)
    private Long vendedorId;

    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoVenta estado;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<LineaVentaEntity> lineas = new ArrayList<>();

    /** `null` en ventas creadas antes de que este campo existiera. */
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 20)
    private MetodoPago metodoPago;

    /**
     * `null` salvo ventas sincronizadas desde la cola offline — ver {@code Venta}.
     * Único compuesto con {@code tienda_id}/{@code vendedor_id} a nivel de BD
     * (`ventas/006-correlation-id-compuesto.xml`) — no se declara aquí con
     * {@code unique = true} porque esa restricción es sobre las tres columnas
     * juntas, no sobre esta sola.
     */
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    public VentaEntity(
            Long id, Long clienteId, Long tiendaId, Long vendedorId, Instant fecha, EstadoVenta estado,
            MetodoPago metodoPago, String correlationId) {
        this.id = id;
        this.clienteId = clienteId;
        this.tiendaId = tiendaId;
        this.vendedorId = vendedorId;
        this.fecha = fecha;
        this.estado = estado;
        this.metodoPago = metodoPago;
        this.correlationId = correlationId;
    }
}
